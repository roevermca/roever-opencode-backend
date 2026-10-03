package com.ams.dto;

public class OverallReportResponse {

    private long totalStudents;
    private long totalRecords;
    private long presentRecords;
    private long absentRecords;
    private double overallPercentage;

    public OverallReportResponse() {
    }

    public OverallReportResponse(long totalStudents, long totalRecords, long presentRecords, long absentRecords, double overallPercentage) {
        this.totalStudents = totalStudents;
        this.totalRecords = totalRecords;
        this.presentRecords = presentRecords;
        this.absentRecords = absentRecords;
        this.overallPercentage = overallPercentage;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getPresentRecords() {
        return presentRecords;
    }

    public void setPresentRecords(long presentRecords) {
        this.presentRecords = presentRecords;
    }

    public long getAbsentRecords() {
        return absentRecords;
    }

    public void setAbsentRecords(long absentRecords) {
        this.absentRecords = absentRecords;
    }

    public double getOverallPercentage() {
        return overallPercentage;
    }

    public void setOverallPercentage(double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }
}
