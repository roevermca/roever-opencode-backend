package com.ams.dto;

import java.time.LocalDate;

public class BulkAttendanceResponse {

    private boolean success = true;
    private String message;
    private LocalDate date;
    private int period;
    private int totalMarked;
    private int presentCount;
    private int absentCount;

    public BulkAttendanceResponse() {
    }

    public BulkAttendanceResponse(boolean success, String message, LocalDate date, int period,
                                  int totalMarked, int presentCount, int absentCount) {
        this.success = success;
        this.message = message;
        this.date = date;
        this.period = period;
        this.totalMarked = totalMarked;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getTotalMarked() {
        return totalMarked;
    }

    public void setTotalMarked(int totalMarked) {
        this.totalMarked = totalMarked;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }
}
