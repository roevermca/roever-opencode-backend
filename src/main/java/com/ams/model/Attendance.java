package com.ams.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Document(collection = "attendance")
@CompoundIndexes({
    @CompoundIndex(
        name = "student_date_period_unique_idx",
        def = "{'studentId': 1, 'date': 1, 'period': 1}",
        unique = true
    ),
    @CompoundIndex(
        name = "attendance_date_period_idx",
        def = "{'date': 1, 'period': 1}"
    ),
    @CompoundIndex(
        name = "attendance_student_date_idx",
        def = "{'studentId': 1, 'date': 1}"
    )
})
public class Attendance {

    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotNull
    @Indexed
    private LocalDate date;

    @Min(1)
    @Max(5)
    private int period;

    @NotNull
    private AttendanceStatus status;

    @NotBlank
    private String markedBy;

    @NotNull
    private Instant markedAt;

    public Attendance() {
    }

    public Attendance(String studentId, LocalDate date, int period, AttendanceStatus status, String markedBy, Instant markedAt) {
        this.studentId = studentId;
        this.date = date;
        this.period = period;
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = markedAt;
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
