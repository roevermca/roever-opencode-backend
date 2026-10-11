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

import com.ams.dto.AttendanceLedgerResponse;
import com.ams.dto.DepartmentReportResponse;
import com.ams.dto.OverallReportResponse;
import com.ams.dto.StudentLedgerRowResponse;
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

    @Test
    void testGetDepartmentReports_IncludesActiveDepartmentsWithStudentCounts() {
        Department dept1 = new Department("Computer Applications", "CA", true);
        dept1.setId("dept-ca");
        Department dept2 = new Department("Commerce", "COM", true);
        dept2.setId("dept-com");

        when(departmentRepository.findByActiveTrue()).thenReturn(List.of(dept1, dept2));

        Student student1 = new Student("23CA001", "Student One", "s1@ams.edu", "123", "dept-ca", "crs-ca", ProgramType.UG, 1, "A", true);
        student1.setId("stu-1");

        when(mongoTemplate.find(any(Query.class), eq(Student.class))).thenReturn(List.of(student1));
        when(departmentRepository.findById("dept-ca")).thenReturn(Optional.of(dept1));

        Attendance att1 = new Attendance("stu-1", LocalDate.now(), 1, AttendanceStatus.PRESENT, "staff-1", Instant.now());
        when(mongoTemplate.find(any(Query.class), eq(Attendance.class))).thenReturn(List.of(att1));

        List<DepartmentReportResponse> reports = reportService.getDepartmentReports(null, null, null, null, null, null, null);

        assertNotNull(reports);
        assertEquals(2, reports.size());

        // Commerce has 0 students enrolled
        DepartmentReportResponse comDept = reports.stream().filter(r -> r.getDepartmentId().equals("dept-com")).findFirst().orElseThrow();
        assertEquals("Commerce", comDept.getDepartment());
        assertEquals(0, comDept.getTotalStudents());
        assertEquals(0.0, comDept.getAttendanceRate());

        // Computer Applications has 1 student enrolled with 100% rate
        DepartmentReportResponse caDept = reports.stream().filter(r -> r.getDepartmentId().equals("dept-ca")).findFirst().orElseThrow();
        assertEquals("Computer Applications", caDept.getDepartment());
        assertEquals(1, caDept.getTotalStudents());
        assertEquals(1, caDept.getPresent());
        assertEquals(100.0, caDept.getAttendanceRate());
    }

    @Test
    void testGetAttendanceLedger_PeriodLogic_FullDay_HalfDay_Absent() {
        Department dept = new Department("MCA", "MCA", true);
        dept.setId("dept-mca");
        Course crs = new Course("Master of Computer Applications", "MCA", "dept-mca", ProgramType.PG, 2, true);
        crs.setId("crs-mca");

        Student student1 = new Student("25PCA001", "AGATHIYA A", "agathiya@ams.edu", "123", "dept-mca", "crs-mca", ProgramType.PG, 1, "A", true);
        student1.setId("stu-1");

        when(mongoTemplate.find(any(Query.class), eq(Student.class))).thenReturn(List.of(student1));
        when(departmentRepository.findById("dept-mca")).thenReturn(Optional.of(dept));
        when(courseRepository.findById("crs-mca")).thenReturn(Optional.of(crs));

        LocalDate d1 = LocalDate.of(2025, 11, 19); // Wednesday
        LocalDate d2 = LocalDate.of(2025, 11, 20); // Thursday
        LocalDate d3 = LocalDate.of(2025, 11, 21); // Friday
        LocalDate d4 = LocalDate.of(2025, 11, 24); // Monday

        Instant now = Instant.now();
        List<Attendance> records = List.of(
                // d1: All 5 periods present -> Full day Present (P)
                new Attendance("stu-1", d1, 1, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d1, 2, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d1, 3, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d1, 4, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d1, 5, AttendanceStatus.PRESENT, "staff-1", now),

                // d2: Periods 1, 2, 3 Present, Periods 4, 5 Absent -> Morning Half Day (H)
                new Attendance("stu-1", d2, 1, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d2, 2, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d2, 3, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d2, 4, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d2, 5, AttendanceStatus.ABSENT, "staff-1", now),

                // d3: Periods 1, 2, 3 Absent, Periods 4, 5 Present -> Afternoon Half Day (H)
                new Attendance("stu-1", d3, 1, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d3, 2, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d3, 3, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d3, 4, AttendanceStatus.PRESENT, "staff-1", now),
                new Attendance("stu-1", d3, 5, AttendanceStatus.PRESENT, "staff-1", now),

                // d4: All periods Absent -> Absent (A)
                new Attendance("stu-1", d4, 1, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d4, 2, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d4, 3, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d4, 4, AttendanceStatus.ABSENT, "staff-1", now),
                new Attendance("stu-1", d4, 5, AttendanceStatus.ABSENT, "staff-1", now)
        );

        when(mongoTemplate.find(any(Query.class), eq(Attendance.class))).thenReturn(records);

        AttendanceLedgerResponse ledger = reportService.getAttendanceLedger(
                d1, d4, "dept-mca", "crs-mca", ProgramType.PG, 1, "A"
        );

        assertNotNull(ledger);
        assertEquals(4, ledger.getTotalWorkingDays());
        assertEquals(1, ledger.getTotalStudents());
        assertEquals("Class : I Master of Computer Applications 2025-2026", ledger.getClassName());

        StudentLedgerRowResponse row = ledger.getStudents().get(0);
        assertEquals("25PCA001", row.getRollNo());
        assertEquals("AGATHIYA A", row.getName());
        assertEquals(4, row.getWorkingDays());
        // d1: P (1.0), d2: H (0.5), d3: H (0.5), d4: A (0.0) -> presentDays = 2.0
        assertEquals(2.0, row.getPresentDays());
        // d1: P (0.0), d2: H (0.5), d3: H (0.5), d4: A (1.0) -> absentDays = 2.0
        assertEquals(2.0, row.getAbsentDays());
        // Attendance rate: (2.0 / 4) * 100 = 50.0%
        assertEquals(50.0, row.getAttendanceRate());

        // Check daily status symbols
        assertEquals("P", row.getAttendanceByDate().get(d1.toString()));
        assertEquals("H", row.getAttendanceByDate().get(d2.toString()));
        assertEquals("H", row.getAttendanceByDate().get(d3.toString()));
        assertEquals("A", row.getAttendanceByDate().get(d4.toString()));

        // Check daily totals
        assertEquals(1.0, ledger.getDailyPresentTotals().get(d1.toString()));
        assertEquals(0.0, ledger.getDailyAbsentTotals().get(d1.toString()));

        assertEquals(0.5, ledger.getDailyPresentTotals().get(d2.toString()));
        assertEquals(0.5, ledger.getDailyAbsentTotals().get(d2.toString()));

        assertEquals(0.5, ledger.getDailyPresentTotals().get(d3.toString()));
        assertEquals(0.5, ledger.getDailyAbsentTotals().get(d3.toString()));

        assertEquals(0.0, ledger.getDailyPresentTotals().get(d4.toString()));
        assertEquals(1.0, ledger.getDailyAbsentTotals().get(d4.toString()));
    }
}
