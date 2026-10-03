package com.ams.dto;

import com.ams.model.AttendanceStatus;

import jakarta.validation.constraints.NotNull;

public class UpdateAttendanceRequest {

    @NotNull(message = "Status is required (PRESENT or ABSENT)")
    private AttendanceStatus status;

    public UpdateAttendanceRequest() {
    }

    public UpdateAttendanceRequest(AttendanceStatus status) {
        this.status = status;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
