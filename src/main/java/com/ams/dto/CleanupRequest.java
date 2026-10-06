package com.ams.dto;

import java.time.LocalDate;

public class CleanupRequest {

    private LocalDate cutoffDate;
    private boolean dryRun;

    public CleanupRequest() {
    }

    public CleanupRequest(LocalDate cutoffDate, boolean dryRun) {
        this.cutoffDate = cutoffDate;
        this.dryRun = dryRun;
    }

    public LocalDate getCutoffDate() {
        return cutoffDate;
    }

    public void setCutoffDate(LocalDate cutoffDate) {
        this.cutoffDate = cutoffDate;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
