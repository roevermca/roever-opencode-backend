package com.ams.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ams.dto.DepartmentReportResponse;
import com.ams.dto.OverallReportResponse;
import com.ams.dto.StudentReportResponse;
import com.ams.model.ProgramType;
import com.ams.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overall")
    public ResponseEntity<OverallReportResponse> getOverallReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) ProgramType programType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String section) {

        OverallReportResponse response = reportService.getOverallReport(
                startDate, endDate, departmentId, courseId, programType, year, section
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentReportResponse>> getStudentReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) ProgramType programType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String section) {

        List<StudentReportResponse> response = reportService.getStudentReports(
                startDate, endDate, departmentId, courseId, programType, year, section
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentReportResponse>> getDepartmentReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) ProgramType programType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String section) {

        List<DepartmentReportResponse> response = reportService.getDepartmentReports(
                startDate, endDate, departmentId, courseId, programType, year, section
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-attendance")
    public ResponseEntity<List<StudentReportResponse>> getLowAttendanceReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) ProgramType programType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "75.0") Double threshold) {

        List<StudentReportResponse> response = reportService.getLowAttendanceReports(
                startDate, endDate, departmentId, courseId, programType, year, section, threshold
        );
        return ResponseEntity.ok(response);
    }
}
