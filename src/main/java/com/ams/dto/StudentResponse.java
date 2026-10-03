package com.ams.dto;

import com.ams.model.ProgramType;
import com.ams.model.Student;

public class StudentResponse {

    private String id;
    private String rollNo;
    private String name;
    private String email;
    private String phone;
    private String departmentId;
    private String courseId;
    private ProgramType programType;
    private int year;
    private String section;
    private boolean active;

    public StudentResponse() {
    }

    public StudentResponse(String id, String rollNo, String name, String email, String phone,
                           String departmentId, String courseId, ProgramType programType,
                           int year, String section, boolean active) {
        this.id = id;
        this.rollNo = rollNo;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.departmentId = departmentId;
        this.courseId = courseId;
        this.programType = programType;
        this.year = year;
        this.section = section;
        this.active = active;
    }

    public static StudentResponse fromEntity(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getRollNo(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getDepartmentId(),
                student.getCourseId(),
                student.getProgramType(),
                student.getYear(),
                student.getSection(),
                student.isActive()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public void setProgramType(ProgramType programType) {
        this.programType = programType;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
