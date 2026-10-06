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
    private String studentName;
    private String studentRollNo;
    private String department;
    private Integer year;
    private String section;

    public AttendanceResponse() {
    }

    public AttendanceResponse(String id, String studentId, LocalDate date, int period,
                              AttendanceStatus status, String markedBy, Instant markedAt) {
        this(id, studentId, date, period, status, markedBy, markedAt, null, null, null, null, null);
    }

    public AttendanceResponse(String id, String studentId, LocalDate date, int period,
                              AttendanceStatus status, String markedBy, Instant markedAt,
                              String studentName, String studentRollNo, String department,
                              Integer year, String section) {
        this.id = id;
        this.studentId = studentId;
        this.date = date;
        this.period = period;
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = markedAt;
        this.studentName = studentName;
        this.studentRollNo = studentRollNo;
        this.department = department;
        this.year = year;
        this.section = section;
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRollNo() {
        return studentRollNo;
    }

    public void setStudentRollNo(String studentRollNo) {
        this.studentRollNo = studentRollNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
