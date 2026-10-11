package com.ams.dto;

import java.util.Map;

public class StudentLedgerRowResponse {

    private String studentId;
    private String rollNo;
    private String name;
    private Map<String, String> attendanceByDate;
    private int workingDays;
    private double presentDays;
    private double absentDays;
    private double attendanceRate;

    public StudentLedgerRowResponse() {
    }

    public StudentLedgerRowResponse(String studentId, String rollNo, String name,
                                   Map<String, String> attendanceByDate, int workingDays,
                                   double presentDays, double absentDays, double attendanceRate) {
        this.studentId = studentId;
        this.rollNo = rollNo;
        this.name = name;
        this.attendanceByDate = attendanceByDate;
        this.workingDays = workingDays;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
        this.attendanceRate = attendanceRate;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getAttendanceByDate() {
        return attendanceByDate;
    }

    public void setAttendanceByDate(Map<String, String> attendanceByDate) {
        this.attendanceByDate = attendanceByDate;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(int workingDays) {
        this.workingDays = workingDays;
    }

    public double getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(double presentDays) {
        this.presentDays = presentDays;
    }

    public double getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(double absentDays) {
        this.absentDays = absentDays;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
