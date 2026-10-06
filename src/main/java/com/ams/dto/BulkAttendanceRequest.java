package com.ams.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class BulkAttendanceRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Min(value = 1, message = "Period must be between 1 and 5")
    @Max(value = 5, message = "Period must be between 1 and 5")
    private Integer period;

    private Boolean fullDay;

    @NotEmpty(message = "Attendance records cannot be empty")
    @Valid
    private List<StudentAttendanceRecord> records;


    public BulkAttendanceRequest() {
    }

    public BulkAttendanceRequest(LocalDate date, Integer period, List<StudentAttendanceRecord> records) {
        this(date, period, false, records);
    }

    public BulkAttendanceRequest(LocalDate date, Integer period, Boolean fullDay, List<StudentAttendanceRecord> records) {
        this.date = date;
        this.period = period;
        this.fullDay = fullDay;
        this.records = records;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Boolean getFullDay() {
        return fullDay;
    }

    public void setFullDay(Boolean fullDay) {
        this.fullDay = fullDay;
    }

    public List<StudentAttendanceRecord> getRecords() {
        return records;
    }

    public void setRecords(List<StudentAttendanceRecord> records) {
        this.records = records;
    }
}

