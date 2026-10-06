package com.ams.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ams.dto.CleanupResponse;
import com.ams.dto.StorageStatusResponse;
import com.ams.model.Attendance;
import com.ams.model.AttendanceArchive;
import com.ams.model.AttendanceStatus;
import com.ams.repository.AttendanceArchiveRepository;
import com.ams.repository.AttendanceRepository;

@Service
public class DatabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStorageService.class);

    private final MongoTemplate mongoTemplate;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceArchiveRepository archiveRepository;

    @Value("${ams.storage.max-quota-bytes:536870912}")
    private long maxQuotaBytes; // Default: 512 MB MongoDB Atlas Free Tier

    @Value("${ams.storage.cleanup-threshold-percent:80.0}")
    private double cleanupThresholdPercent; // Trigger at 80%

    @Value("${ams.storage.target-threshold-percent:70.0}")
    private double targetThresholdPercent; // Safe target 70%

    @Value("${ams.storage.retention-min-days:90}")
    private int retentionMinDays; // Current semester protected window (90 days)

    public DatabaseStorageService(MongoTemplate mongoTemplate,
                                  AttendanceRepository attendanceRepository,
                                  AttendanceArchiveRepository archiveRepository) {
        this.mongoTemplate = mongoTemplate;
        this.attendanceRepository = attendanceRepository;
        this.archiveRepository = archiveRepository;
    }

    public StorageStatusResponse getStorageStatus() {
        long usedBytes = queryStorageBytes();
        double usedMb = roundToTwoDecimals((double) usedBytes / (1024.0 * 1024.0));
        double maxLimitMb = roundToTwoDecimals((double) maxQuotaBytes / (1024.0 * 1024.0));
        double percentage = maxQuotaBytes > 0
                ? roundToTwoDecimals(((double) usedBytes / maxQuotaBytes) * 100.0)
                : 0.0;

        String status;
        if (percentage >= 90.0) {
            status = "CRITICAL_90_PERCENT";
        } else if (percentage >= cleanupThresholdPercent) {
            status = "WARNING_80_PERCENT";
        } else {
            status = "HEALTHY";
        }

        long totalAttendanceRecords = attendanceRepository.count();
        LocalDate oldestDate = attendanceRepository.findFirstByOrderByDateAsc()
                .map(Attendance::getDate)
                .orElse(null);
        LocalDate newestDate = attendanceRepository.findFirstByOrderByDateDesc()
                .map(Attendance::getDate)
                .orElse(null);

        return new StorageStatusResponse(
                usedBytes,
                usedMb,
                maxLimitMb,
                percentage,
                status,
                cleanupThresholdPercent,
                totalAttendanceRecords,
                oldestDate,
                newestDate,
                true // Auto-cleanup actively monitoring
        );
    }

    @Scheduled(cron = "${ams.storage.cleanup-cron:0 0 3 * * *}")
    public void scheduledCleanupCheck() {
        log.info("Running scheduled database storage check (3:00 AM off-peak routine)...");
        try {
            StorageStatusResponse status = getStorageStatus();
            log.info("Database storage check: {} MB / {} MB ({}%) - Status: {}",
                    status.getUsedMb(), status.getMaxLimitMb(), status.getPercentageUsed(), status.getStatus());

            if (status.getPercentageUsed() >= cleanupThresholdPercent) {
                log.warn("Database storage usage ({}%) exceeds threshold ({}%). Executing automatic cleanup...",
                        status.getPercentageUsed(), cleanupThresholdPercent);
                performAutoCleanup();
            }
        } catch (Exception e) {
            log.error("Error during scheduled storage check: {}", e.getMessage(), e);
        }
    }

    public CleanupResponse performAutoCleanup() {
        StorageStatusResponse initialStatus = getStorageStatus();
        double beforeMb = initialStatus.getUsedMb();

        LocalDate maxProtectedCutoff = LocalDate.now().minusDays(retentionMinDays);
        Optional<Attendance> oldestOpt = attendanceRepository.findFirstByOrderByDateAsc();

        if (oldestOpt.isEmpty() || !oldestOpt.get().getDate().isBefore(maxProtectedCutoff)) {
            return new CleanupResponse(
                    true,
                    "No attendance records older than retention period (" + retentionMinDays + " days) found. Storage protected.",
                    false,
                    0,
                    0,
                    maxProtectedCutoff,
                    beforeMb,
                    beforeMb
            );
        }

        long totalDeleted = 0;
        long totalArchived = 0;
        LocalDate currentPurgeCutoff = oldestOpt.get().getDate().plusDays(30);
        if (currentPurgeCutoff.isAfter(maxProtectedCutoff)) {
            currentPurgeCutoff = maxProtectedCutoff;
        }

        // Archive student summary before deletion
        long archived = archiveAttendanceSummaries(currentPurgeCutoff);
        totalArchived += archived;

        // Delete raw records
        long deleted = attendanceRepository.deleteByDateLessThanEqual(currentPurgeCutoff);
        totalDeleted += deleted;

        StorageStatusResponse afterStatus = getStorageStatus();
        double afterMb = afterStatus.getUsedMb();

        log.info("Auto-cleanup completed: Deleted {} raw attendance records older than {}, archived {} summaries. New storage: {} MB ({}%)",
                totalDeleted, currentPurgeCutoff, totalArchived, afterMb, afterStatus.getPercentageUsed());

        return new CleanupResponse(
                true,
                "Auto-cleanup successfully freed database storage.",
                false,
                totalDeleted,
                totalArchived,
                currentPurgeCutoff,
                beforeMb,
                afterMb
        );
    }

    public CleanupResponse performManualCleanup(LocalDate cutoffDate, boolean dryRun) {
        LocalDate maxProtectedCutoff = LocalDate.now().minusDays(retentionMinDays);
        LocalDate effectiveCutoff = cutoffDate != null ? cutoffDate : maxProtectedCutoff;

        // Ensure current semester protection unless explicitly forced with an older date
        if (effectiveCutoff.isAfter(maxProtectedCutoff)) {
            effectiveCutoff = maxProtectedCutoff;
        }

        long recordsAffected = attendanceRepository.countByDateLessThanEqual(effectiveCutoff);
        StorageStatusResponse initialStatus = getStorageStatus();
        double beforeMb = initialStatus.getUsedMb();

        if (dryRun) {
            return new CleanupResponse(
                    true,
                    "Dry run complete. " + recordsAffected + " attendance records are eligible for cleanup prior to " + effectiveCutoff,
                    true,
                    recordsAffected,
                    0,
                    effectiveCutoff,
                    beforeMb,
                    beforeMb
            );
        }

        if (recordsAffected == 0) {
            return new CleanupResponse(
                    true,
                    "No attendance records found older than " + effectiveCutoff + " to clean up.",
                    false,
                    0,
                    0,
                    effectiveCutoff,
                    beforeMb,
                    beforeMb
            );
        }

        long archivedCount = archiveAttendanceSummaries(effectiveCutoff);
        long deletedCount = attendanceRepository.deleteByDateLessThanEqual(effectiveCutoff);

        StorageStatusResponse afterStatus = getStorageStatus();
        double afterMb = afterStatus.getUsedMb();

        return new CleanupResponse(
                true,
                "Successfully archived " + archivedCount + " student summaries and cleaned up " + deletedCount + " old attendance records.",
                false,
                deletedCount,
                archivedCount,
                effectiveCutoff,
                beforeMb,
                afterMb
        );
    }

    private long archiveAttendanceSummaries(LocalDate cutoffDate) {
        List<Attendance> recordsToPurge = attendanceRepository.findByDateLessThanEqual(cutoffDate);
        if (recordsToPurge.isEmpty()) {
            return 0;
        }

        Map<String, List<Attendance>> byStudent = recordsToPurge.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        String dateRange = "History up to " + cutoffDate;
        Instant now = Instant.now();
        int archivedCount = 0;

        for (Map.Entry<String, List<Attendance>> entry : byStudent.entrySet()) {
            String studentId = entry.getKey();
            List<Attendance> list = entry.getValue();

            long total = list.size();
            long present = list.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.ON_DUTY)
                    .count();
            long absent = list.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                    .count();
            double pct = total > 0 ? roundToTwoDecimals((present * 100.0) / total) : 0.0;

            Optional<AttendanceArchive> existing = archiveRepository.findByStudentIdAndDateRange(studentId, dateRange);
            if (existing.isPresent()) {
                AttendanceArchive archive = existing.get();
                archive.setTotalRecords(archive.getTotalRecords() + total);
                archive.setPresentRecords(archive.getPresentRecords() + present);
                archive.setAbsentRecords(archive.getAbsentRecords() + absent);
                archive.setAttendancePercentage(archive.getTotalRecords() > 0
                        ? roundToTwoDecimals((archive.getPresentRecords() * 100.0) / archive.getTotalRecords())
                        : 0.0);
                archive.setArchivedAt(now);
                archiveRepository.save(archive);
            } else {
                archiveRepository.save(new AttendanceArchive(
                        studentId,
                        dateRange,
                        total,
                        present,
                        absent,
                        pct,
                        now
                ));
            }
            archivedCount++;
        }

        return archivedCount;
    }

    private long queryStorageBytes() {
        try {
            Document stats = mongoTemplate.executeCommand(new Document("dbStats", 1));
            if (stats != null) {
                Number totalSize = stats.get("totalSize", Number.class);
                if (totalSize != null && totalSize.longValue() > 0) {
                    return totalSize.longValue();
                }

                Number storageSize = stats.get("storageSize", Number.class);
                Number indexSize = stats.get("indexSize", Number.class);
                long storage = storageSize != null ? storageSize.longValue() : 0L;
                long index = indexSize != null ? indexSize.longValue() : 0L;
                if (storage + index > 0) {
                    return storage + index;
                }

                Number dataSize = stats.get("dataSize", Number.class);
                if (dataSize != null && dataSize.longValue() > 0) {
                    return dataSize.longValue() + index;
                }
            }
        } catch (Exception e) {
            log.warn("Could not query mongo dbStats command: {}. Using collection estimate.", e.getMessage());
        }

        // Safe fallback estimation if dbStats command is restricted or offline
        long attendanceCount = attendanceRepository.count();
        long estimate = (attendanceCount * 120L) + 2_000_000L; // 120 bytes per attendance record + 2MB master overhead
        return Math.min(estimate, maxQuotaBytes);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Setters for unit testing
    void setMaxQuotaBytes(long maxQuotaBytes) {
        this.maxQuotaBytes = maxQuotaBytes;
    }

    void setCleanupThresholdPercent(double cleanupThresholdPercent) {
        this.cleanupThresholdPercent = cleanupThresholdPercent;
    }

    void setTargetThresholdPercent(double targetThresholdPercent) {
        this.targetThresholdPercent = targetThresholdPercent;
    }

    void setRetentionMinDays(int retentionMinDays) {
        this.retentionMinDays = retentionMinDays;
    }
}
