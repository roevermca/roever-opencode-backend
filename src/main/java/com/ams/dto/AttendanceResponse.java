package com.ams.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;

public class AttendanceResponse {

    private String id;
    private String studentId;
    private LocalDate date;
    private int period;
    private AttendanceStatus status;
    private String markedBy;
    private Instant markedAt;

    public AttendanceResponse() {
    }

    public AttendanceResponse(String id, String studentId, LocalDate date, int period,
                              AttendanceStatus status, String markedBy, Instant markedAt) {
        this.id = id;
        this.studentId = studentId;
        this.date = date;
        this.period = period;
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = markedAt;
    }

    public static AttendanceResponse fromEntity(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getStudentId(),
                attendance.getDate(),
                attendance.getPeriod(),
                attendance.getStatus(),
                attendance.getMarkedBy(),
                attendance.getMarkedAt()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public Instant getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(Instant markedAt) {
        this.markedAt = markedAt;
    }
}
