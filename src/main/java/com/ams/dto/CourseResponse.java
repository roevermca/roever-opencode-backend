package com.ams.dto;

import com.ams.model.Course;
import com.ams.model.ProgramType;

public class CourseResponse {

    private String id;
    private String name;
    private String code;
    private String departmentId;
    private ProgramType programType;
    private int durationYears;
    private boolean active;

    public CourseResponse() {
    }

    public CourseResponse(String id, String name, String code, String departmentId,
                          ProgramType programType, int durationYears, boolean active) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.departmentId = departmentId;
        this.programType = programType;
        this.durationYears = durationYears;
        this.active = active;
    }

    public static CourseResponse fromEntity(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDepartmentId(),
                course.getProgramType(),
                course.getDurationYears(),
                course.isActive()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public void setProgramType(ProgramType programType) {
        this.programType = programType;
    }

    public int getDurationYears() {
        return durationYears;
    }

    public void setDurationYears(int durationYears) {
        this.durationYears = durationYears;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
