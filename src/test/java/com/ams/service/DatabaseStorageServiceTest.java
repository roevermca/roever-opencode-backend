package com.ams.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.ams.dto.CleanupResponse;
import com.ams.dto.StorageStatusResponse;
import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;
import com.ams.repository.AttendanceArchiveRepository;
import com.ams.repository.AttendanceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatabaseStorageServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceArchiveRepository archiveRepository;

    @InjectMocks
    private DatabaseStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService.setMaxQuotaBytes(536870912L); // 512 MB
        storageService.setCleanupThresholdPercent(80.0);
        storageService.setTargetThresholdPercent(70.0);
        storageService.setRetentionMinDays(90);
    }

    @Test
    @DisplayName("getStorageStatus should calculate correct MB and HEALTHY status when usage is below 80%")
    void getStorageStatus_whenLowUsage_shouldReturnHealthy() {
        Document stats = new Document();
        stats.put("totalSize", 100L * 1024L * 1024L); // 100 MB
        given(mongoTemplate.executeCommand(any(Document.class))).willReturn(stats);
        given(attendanceRepository.count()).willReturn(5000L);
        given(attendanceRepository.findFirstByOrderByDateAsc()).willReturn(Optional.empty());
        given(attendanceRepository.findFirstByOrderByDateDesc()).willReturn(Optional.empty());

        StorageStatusResponse status = storageService.getStorageStatus();

        assertThat(status.getUsedMb()).isEqualTo(100.0);
        assertThat(status.getMaxLimitMb()).isEqualTo(512.0);
        assertThat(status.getPercentageUsed()).isLessThan(80.0);
        assertThat(status.getStatus()).isEqualTo("HEALTHY");
        assertThat(status.isAutoCleanupActive()).isTrue();
    }

    @Test
    @DisplayName("getStorageStatus should report WARNING_80_PERCENT when usage exceeds 80%")
    void getStorageStatus_whenHighUsage_shouldReturnWarning() {
        Document stats = new Document();
        stats.put("totalSize", 440L * 1024L * 1024L); // 440 MB (~85.9%)
        given(mongoTemplate.executeCommand(any(Document.class))).willReturn(stats);
        given(attendanceRepository.count()).willReturn(45000L);
        given(attendanceRepository.findFirstByOrderByDateAsc()).willReturn(Optional.empty());
        given(attendanceRepository.findFirstByOrderByDateDesc()).willReturn(Optional.empty());

        StorageStatusResponse status = storageService.getStorageStatus();

        assertThat(status.getPercentageUsed()).isGreaterThanOrEqualTo(80.0);
        assertThat(status.getStatus()).isEqualTo("WARNING_80_PERCENT");
    }

    @Test
    @DisplayName("performAutoCleanup should skip cleanup when records are within protected retention window")
    void performAutoCleanup_whenRecordsWithinProtectedWindow_shouldSkip() {
        Document stats = new Document();
        stats.put("totalSize", 450L * 1024L * 1024L);
        given(mongoTemplate.executeCommand(any(Document.class))).willReturn(stats);

        // Oldest record is only 10 days old (within 90 days retention window)
        Attendance recent = new Attendance("s1", LocalDate.now().minusDays(10), 1, AttendanceStatus.PRESENT, "admin", null);
        given(attendanceRepository.findFirstByOrderByDateAsc()).willReturn(Optional.of(recent));

        CleanupResponse response = storageService.performAutoCleanup();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getRecordsDeleted()).isEqualTo(0);
        verify(attendanceRepository, never()).deleteByDateLessThanEqual(any());
    }

    @Test
    @DisplayName("performAutoCleanup should archive and delete oldest batch when records exceed 90 days")
    void performAutoCleanup_whenOldRecordsExist_shouldArchiveAndDelete() {
        Document stats = new Document();
        stats.put("totalSize", 450L * 1024L * 1024L);
        given(mongoTemplate.executeCommand(any(Document.class))).willReturn(stats);

        LocalDate oldDate = LocalDate.now().minusDays(120);
        Attendance oldRec = new Attendance("s1", oldDate, 1, AttendanceStatus.PRESENT, "admin", null);
        given(attendanceRepository.findFirstByOrderByDateAsc()).willReturn(Optional.of(oldRec));
        given(attendanceRepository.findByDateLessThanEqual(any())).willReturn(List.of(oldRec));
        given(attendanceRepository.deleteByDateLessThanEqual(any())).willReturn(100L);

        CleanupResponse response = storageService.performAutoCleanup();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getRecordsDeleted()).isEqualTo(100L);
        assertThat(response.getSummariesArchived()).isEqualTo(1L);
        verify(attendanceRepository).deleteByDateLessThanEqual(any());
    }

    @Test
    @DisplayName("performManualCleanup should return count without deleting when dryRun is true")
    void performManualCleanup_dryRun_shouldNotDelete() {
        Document stats = new Document();
        stats.put("totalSize", 200L * 1024L * 1024L);
        given(mongoTemplate.executeCommand(any(Document.class))).willReturn(stats);

        LocalDate cutoff = LocalDate.now().minusDays(100);
        given(attendanceRepository.countByDateLessThanEqual(cutoff)).willReturn(250L);

        CleanupResponse response = storageService.performManualCleanup(cutoff, true);

        assertThat(response.isDryRun()).isTrue();
        assertThat(response.getRecordsDeleted()).isEqualTo(250L);
        verify(attendanceRepository, never()).deleteByDateLessThanEqual(any());
    }
}
