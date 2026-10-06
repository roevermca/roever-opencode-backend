package com.ams.dto;

import java.time.LocalDate;

public class CleanupResponse {

    private boolean success;
    private String message;
    private boolean dryRun;
    private long recordsDeleted;
    private long summariesArchived;
    private LocalDate cutoffDate;
    private double storageUsedMbBefore;
    private double storageUsedMbAfter;

    public CleanupResponse() {
    }

    public CleanupResponse(boolean success, String message, boolean dryRun,
                           long recordsDeleted, long summariesArchived, LocalDate cutoffDate,
                           double storageUsedMbBefore, double storageUsedMbAfter) {
        this.success = success;
        this.message = message;
        this.dryRun = dryRun;
        this.recordsDeleted = recordsDeleted;
        this.summariesArchived = summariesArchived;
        this.cutoffDate = cutoffDate;
        this.storageUsedMbBefore = storageUsedMbBefore;
        this.storageUsedMbAfter = storageUsedMbAfter;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public long getRecordsDeleted() {
        return recordsDeleted;
    }

    public void setRecordsDeleted(long recordsDeleted) {
        this.recordsDeleted = recordsDeleted;
    }

    public long getSummariesArchived() {
        return summariesArchived;
    }

    public void setSummariesArchived(long summariesArchived) {
        this.summariesArchived = summariesArchived;
    }

    public LocalDate getCutoffDate() {
        return cutoffDate;
    }

    public void setCutoffDate(LocalDate cutoffDate) {
        this.cutoffDate = cutoffDate;
    }

    public double getStorageUsedMbBefore() {
        return storageUsedMbBefore;
    }

    public void setStorageUsedMbBefore(double storageUsedMbBefore) {
        this.storageUsedMbBefore = storageUsedMbBefore;
    }

    public double getStorageUsedMbAfter() {
        return storageUsedMbAfter;
    }

    public void setStorageUsedMbAfter(double storageUsedMbAfter) {
        this.storageUsedMbAfter = storageUsedMbAfter;
    }
}
