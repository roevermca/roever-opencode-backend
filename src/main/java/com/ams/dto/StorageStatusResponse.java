package com.ams.dto;

import java.time.LocalDate;

public class StorageStatusResponse {

    private long usedBytes;
    private double usedMb;
    private double maxLimitMb;
    private double percentageUsed;
    private String status; // "HEALTHY", "WARNING_80_PERCENT", "CRITICAL_90_PERCENT"
    private double cleanupThresholdPercent;
    private long totalAttendanceRecords;
    private LocalDate oldestRecordDate;
    private LocalDate newestRecordDate;
    private boolean autoCleanupActive;

    public StorageStatusResponse() {
    }

    public StorageStatusResponse(long usedBytes, double usedMb, double maxLimitMb,
                                 double percentageUsed, String status, double cleanupThresholdPercent,
                                 long totalAttendanceRecords, LocalDate oldestRecordDate,
                                 LocalDate newestRecordDate, boolean autoCleanupActive) {
        this.usedBytes = usedBytes;
        this.usedMb = usedMb;
        this.maxLimitMb = maxLimitMb;
        this.percentageUsed = percentageUsed;
        this.status = status;
        this.cleanupThresholdPercent = cleanupThresholdPercent;
        this.totalAttendanceRecords = totalAttendanceRecords;
        this.oldestRecordDate = oldestRecordDate;
        this.newestRecordDate = newestRecordDate;
        this.autoCleanupActive = autoCleanupActive;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public double getUsedMb() {
        return usedMb;
    }

    public void setUsedMb(double usedMb) {
        this.usedMb = usedMb;
    }

    public double getMaxLimitMb() {
        return maxLimitMb;
    }

    public void setMaxLimitMb(double maxLimitMb) {
        this.maxLimitMb = maxLimitMb;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }

    public void setPercentageUsed(double percentageUsed) {
        this.percentageUsed = percentageUsed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCleanupThresholdPercent() {
        return cleanupThresholdPercent;
    }

    public void setCleanupThresholdPercent(double cleanupThresholdPercent) {
        this.cleanupThresholdPercent = cleanupThresholdPercent;
    }

    public long getTotalAttendanceRecords() {
        return totalAttendanceRecords;
    }

    public void setTotalAttendanceRecords(long totalAttendanceRecords) {
        this.totalAttendanceRecords = totalAttendanceRecords;
    }

    public LocalDate getOldestRecordDate() {
        return oldestRecordDate;
    }

    public void setOldestRecordDate(LocalDate oldestRecordDate) {
        this.oldestRecordDate = oldestRecordDate;
    }

    public LocalDate getNewestRecordDate() {
        return newestRecordDate;
    }

    public void setNewestRecordDate(LocalDate newestRecordDate) {
        this.newestRecordDate = newestRecordDate;
    }

    public boolean isAutoCleanupActive() {
        return autoCleanupActive;
    }

    public void setAutoCleanupActive(boolean autoCleanupActive) {
        this.autoCleanupActive = autoCleanupActive;
    }
}
