package com.ams.dto;

public class DepartmentReportResponse {

    private String departmentId;
    private String department;
    private long totalStudents;
    private long present;
    private long absent;
    private long total;
    private double attendanceRate;

    public DepartmentReportResponse() {
    }

    public DepartmentReportResponse(String departmentId, String department, long totalStudents,
                                    long present, long absent, long total, double attendanceRate) {
        this.departmentId = departmentId;
        this.department = department;
        this.totalStudents = totalStudents;
        this.present = present;
        this.absent = absent;
        this.total = total;
        this.attendanceRate = attendanceRate;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
