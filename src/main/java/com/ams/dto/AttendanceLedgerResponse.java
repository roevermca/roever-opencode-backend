package com.ams.dto;

import java.util.List;
import java.util.Map;

public class AttendanceLedgerResponse {

    private String className;
    private String departmentName;
    private String courseName;
    private Integer year;
    private String section;
    private List<String> dates;
    private List<StudentLedgerRowResponse> students;
    private Map<String, Double> dailyPresentTotals;
    private Map<String, Double> dailyAbsentTotals;
    private Map<String, Integer> dailyStudentTotals;
    private int totalWorkingDays;
    private int totalStudents;

    public AttendanceLedgerResponse() {
    }

    public AttendanceLedgerResponse(String className, String departmentName, String courseName,
                                    Integer year, String section, List<String> dates,
                                    List<StudentLedgerRowResponse> students,
                                    Map<String, Double> dailyPresentTotals,
                                    Map<String, Double> dailyAbsentTotals,
                                    Map<String, Integer> dailyStudentTotals,
                                    int totalWorkingDays, int totalStudents) {
        this.className = className;
        this.departmentName = departmentName;
        this.courseName = courseName;
        this.year = year;
        this.section = section;
        this.dates = dates;
        this.students = students;
        this.dailyPresentTotals = dailyPresentTotals;
        this.dailyAbsentTotals = dailyAbsentTotals;
        this.dailyStudentTotals = dailyStudentTotals;
        this.totalWorkingDays = totalWorkingDays;
        this.totalStudents = totalStudents;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
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

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public List<StudentLedgerRowResponse> getStudents() {
        return students;
    }

    public void setStudents(List<StudentLedgerRowResponse> students) {
        this.students = students;
    }

    public Map<String, Double> getDailyPresentTotals() {
        return dailyPresentTotals;
    }

    public void setDailyPresentTotals(Map<String, Double> dailyPresentTotals) {
        this.dailyPresentTotals = dailyPresentTotals;
    }

    public Map<String, Double> getDailyAbsentTotals() {
        return dailyAbsentTotals;
    }

    public void setDailyAbsentTotals(Map<String, Double> dailyAbsentTotals) {
        this.dailyAbsentTotals = dailyAbsentTotals;
    }

    public Map<String, Integer> getDailyStudentTotals() {
        return dailyStudentTotals;
    }

    public void setDailyStudentTotals(Map<String, Integer> dailyStudentTotals) {
        this.dailyStudentTotals = dailyStudentTotals;
    }

    public int getTotalWorkingDays() {
        return totalWorkingDays;
    }

    public void setTotalWorkingDays(int totalWorkingDays) {
        this.totalWorkingDays = totalWorkingDays;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }
}
