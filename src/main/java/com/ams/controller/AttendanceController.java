package com.ams.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ams.dto.AttendanceResponse;
import com.ams.dto.AttendanceSummaryResponse;
import com.ams.dto.BulkAttendanceRequest;
import com.ams.dto.BulkAttendanceResponse;
import com.ams.dto.PageResponse;
import com.ams.dto.UpdateAttendanceRequest;
import com.ams.model.AttendanceStatus;
import com.ams.service.AttendanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkAttendanceResponse> markAttendanceBulk(
            @Valid @RequestBody BulkAttendanceRequest request) {
        BulkAttendanceResponse response = attendanceService.markAttendanceBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AttendanceResponse>> getAttendanceHistory(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<AttendanceResponse> response = attendanceService.getAttendanceHistory(
                studentId, date, startDate, endDate, period, status, page, size
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<PageResponse<AttendanceResponse>> getStudentAttendance(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getStudentAttendance(studentId, page, size));
    }

    @GetMapping("/student/{studentId}/summary")
    public ResponseEntity<AttendanceSummaryResponse> getStudentAttendanceSummary(
            @PathVariable String studentId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(studentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> updateAttendanceRecord(
            @PathVariable String id,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.updateAttendanceRecord(id, request));
    }
}
