package com.ams.dto;

public class StudentReportResponse {

    private String studentId;
    private String rollNo;
    private String name;
    private String departmentId;
    private String departmentName;
    private String courseId;
    private String courseName;
    private String level;
    private int year;
    private String section;
    private long present;
    private long absent;
    private long totalPeriods;
    private double attendanceRate;

    public StudentReportResponse() {
    }

    public StudentReportResponse(String studentId, String rollNo, String name, String departmentId,
                                 String departmentName, String courseId, String courseName,
                                 String level, int year, String section, long present, long absent,
                                 long totalPeriods, double attendanceRate) {
        this.studentId = studentId;
        this.rollNo = rollNo;
        this.name = name;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.level = level;
        this.year = year;
        this.section = section;
        this.present = present;
        this.absent = absent;
        this.totalPeriods = totalPeriods;
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

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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

    public long getTotalPeriods() {
        return totalPeriods;
    }

    public void setTotalPeriods(long totalPeriods) {
        this.totalPeriods = totalPeriods;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
