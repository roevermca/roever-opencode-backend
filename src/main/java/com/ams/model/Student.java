package com.ams.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Document(collection = "students")
@CompoundIndexes({
    @CompoundIndex(
        name = "student_class_section_active_idx",
        def = "{'departmentId': 1, 'courseId': 1, 'year': 1, 'section': 1, 'active': 1}"
    )
})
public class Student {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String rollNo;

    @NotBlank
    @Indexed
    private String name;

    @NotBlank
    @Email
    @Indexed
    private String email;

    private String phone;

    @NotBlank
    @Indexed
    private String departmentId;

    @NotBlank
    @Indexed
    private String courseId;

    @NotNull
    private ProgramType programType;

    @Min(1)
    @Max(4)
    private int year;

    @NotBlank
    private String section;

    private boolean active = true;

    public Student() {
    }

    public Student(String rollNo, String name, String email, String phone, String departmentId,
                   String courseId, ProgramType programType, int year, String section, boolean active) {
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
