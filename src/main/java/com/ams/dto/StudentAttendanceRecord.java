package com.ams.dto;

import com.ams.model.AttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StudentAttendanceRecord {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotNull(message = "Status is required (PRESENT or ABSENT)")
    private AttendanceStatus status;

    public StudentAttendanceRecord() {
    }

    public StudentAttendanceRecord(String studentId, AttendanceStatus status) {
        this.studentId = studentId;
        this.status = status;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
