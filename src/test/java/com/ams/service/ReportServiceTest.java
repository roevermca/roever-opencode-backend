package com.ams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ams.dto.DepartmentReportResponse;
import com.ams.dto.OverallReportResponse;
import com.ams.dto.StudentReportResponse;
import com.ams.exception.AccessDeniedException;
import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;
import com.ams.model.Course;
import com.ams.model.Department;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.repository.CourseRepository;
import com.ams.repository.DepartmentRepository;
import com.ams.repository.StudentRepository;
import com.ams.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        AuthenticatedUser adminUser = new AuthenticatedUser(
                "admin-1", "fb-admin", "admin@amsportal.edu", "Admin User", Role.ADMIN, "dept-1", null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetOverallReport_CalculatesMetricsCorrectly() {
        Student student1 = new Student("2024CS101", "Student One", "s1@amsportal.edu", "123", "dept-1", "crs-1", ProgramType.UG, 3, "A", true);
        student1.setId("stu-1");
        Student student2 = new Student("2024CS102", "Student Two", "s2@amsportal.edu", "123", "dept-1", "crs-1", ProgramType.UG, 3, "A", true);
        student2.setId("stu-2");

        when(mongoTemplate.find(any(Query.class), eq(Student.class)))
                .thenReturn(List.of(student1, student2));

        Attendance att1 = new Attendance("stu-1", LocalDate.now(), 1, AttendanceStatus.PRESENT, "staff-1", Instant.now());
        Attendance att2 = new Attendance("stu-1", LocalDate.now(), 2, AttendanceStatus.PRESENT, "staff-1", Instant.now());
        Attendance att3 = new Attendance("stu-2", LocalDate.now(), 1, AttendanceStatus.ABSENT, "staff-1", Instant.now());

        when(mongoTemplate.find(any(Query.class), eq(Attendance.class)))
                .thenReturn(List.of(att1, att2, att3));

        OverallReportResponse report = reportService.getOverallReport(
                null, null, "dept-1", null, null, null, null
        );

        assertNotNull(report);
        assertEquals(2, report.getTotalStudents());
        assertEquals(3, report.getTotalRecords());
        assertEquals(2, report.getPresentRecords());
        assertEquals(1, report.getAbsentRecords());
        assertEquals(66.7, report.getOverallPercentage());
    }

    @Test
    void testGetLowAttendanceReports_FiltersUnderThreshold() {
        Student student1 = new Student("2024CS101", "Student One", "s1@amsportal.edu", "123", "dept-1", "crs-1", ProgramType.UG, 3, "A", true);
        student1.setId("stu-1");
        Student student2 = new Student("2024CS102", "Student Two", "s2@amsportal.edu", "123", "dept-1", "crs-1", ProgramType.UG, 3, "A", true);
        student2.setId("stu-2");

        when(mongoTemplate.find(any(Query.class), eq(Student.class)))
                .thenReturn(List.of(student1, student2));

        // stu-1: 100% (2/2)
        Attendance att1 = new Attendance("stu-1", LocalDate.now(), 1, AttendanceStatus.PRESENT, "staff-1", Instant.now());
        Attendance att2 = new Attendance("stu-1", LocalDate.now(), 2, AttendanceStatus.PRESENT, "staff-1", Instant.now());
        // stu-2: 0% (0/2)
        Attendance att3 = new Attendance("stu-2", LocalDate.now(), 1, AttendanceStatus.ABSENT, "staff-1", Instant.now());
        Attendance att4 = new Attendance("stu-2", LocalDate.now(), 2, AttendanceStatus.ABSENT, "staff-1", Instant.now());

        when(mongoTemplate.find(any(Query.class), eq(Attendance.class)))
                .thenReturn(List.of(att1, att2, att3, att4));

        when(departmentRepository.findById("dept-1")).thenReturn(Optional.of(new Department("Computer Science", "CSE", true)));
        when(courseRepository.findById("crs-1")).thenReturn(Optional.of(new Course("B.Tech CSE", "BCSE", "dept-1", ProgramType.UG, 4, true)));

        List<StudentReportResponse> lowAtt = reportService.getLowAttendanceReports(
                null, null, null, null, null, null, null, 75.0
        );

        assertEquals(1, lowAtt.size());
        assertEquals("stu-2", lowAtt.get(0).getStudentId());
        assertEquals(0.0, lowAtt.get(0).getAttendanceRate());
    }

    @Test
    void testStaffUser_CannotAccessReports() {
        AuthenticatedUser staffUser = new AuthenticatedUser(
                "staff-1", "fb-staff", "staff@amsportal.edu", "Staff User", Role.STAFF, "dept-1", null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(staffUser, null, staffUser.getAuthorities())
        );

        assertThrows(AccessDeniedException.class, () ->
                reportService.getOverallReport(null, null, null, null, null, null, null)
        );
    }
}
