package com.ams.dto;

public class AttendanceSummaryResponse {

    private long totalClasses;
    private long present;
    private long absent;
    private double percentage;

    public AttendanceSummaryResponse() {
    }

    public AttendanceSummaryResponse(long totalClasses, long present, long absent, double percentage) {
        this.totalClasses = totalClasses;
        this.present = present;
        this.absent = absent;
        this.percentage = percentage;
    }

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getPresent() {
        return present;
    }

    public void setPresent(long present) {
        this.present = present;
    }

    public long getAbsent() {
        return absent;
    }

    public void setAbsent(long absent) {
        this.absent = absent;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
