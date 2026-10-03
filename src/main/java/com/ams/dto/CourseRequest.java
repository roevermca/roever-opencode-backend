package com.ams.dto;

import com.ams.model.ProgramType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CourseRequest {

    @NotBlank(message = "Course name is required")
    private String name;

    @NotBlank(message = "Course code is required")
    private String code;

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    @NotNull(message = "Program type is required (UG/PG)")
    private ProgramType programType;

    @NotNull(message = "Duration in years is required")
    @Min(value = 1, message = "Duration must be at least 1 year")
    @Max(value = 5, message = "Duration cannot exceed 5 years")
    private Integer durationYears;

    private Boolean active = true;

    public CourseRequest() {
    }

    public CourseRequest(String name, String code, String departmentId, ProgramType programType, Integer durationYears, Boolean active) {
        this.name = name;
        this.code = code;
        this.departmentId = departmentId;
        this.programType = programType;
        this.durationYears = durationYears;
        this.active = active != null ? active : true;
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

    public Integer getDurationYears() {
        return durationYears;
    }

    public void setDurationYears(Integer durationYears) {
        this.durationYears = durationYears;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
