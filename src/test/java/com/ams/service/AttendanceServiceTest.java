package com.ams.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ams.dto.AttendanceResponse;
import com.ams.dto.AttendanceSummaryResponse;
import com.ams.dto.BulkAttendanceRequest;
import com.ams.dto.BulkAttendanceResponse;
import com.ams.dto.PageResponse;
import com.ams.dto.StudentAttendanceRecord;
import com.ams.dto.UpdateAttendanceRequest;
import com.ams.exception.AccessDeniedException;
import com.ams.exception.DuplicateResourceException;
import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.repository.AttendanceRepository;
import com.ams.repository.StudentRepository;
import com.ams.security.AuthenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private AttendanceService attendanceService;

    private void setSecurityContext(String userId, Role role, String departmentId, String studentId) {
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "firebase-" + userId,
                userId + "@amsportal.edu",
                "Test User",
                role,
                departmentId,
                studentId
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bulk attendance creation succeeds in a single batch operation")
    void markAttendanceBulk_success() {
        setSecurityContext("staff-1", Role.STAFF, "dept-cs", null);

        Student s1 = new Student("2024CS101", "Student 1", "s1@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        s1.setId("std-1");
        Student s2 = new Student("2024CS102", "Student 2", "s2@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        s2.setId("std-2");

        given(studentRepository.findAllById(anyCollection())).willReturn(List.of(s1, s2));
        given(attendanceRepository.findByStudentIdInAndDateAndPeriod(anyCollection(), any(), eq(1)))
                .willReturn(List.of());

        BulkAttendanceRequest request = new BulkAttendanceRequest(
                LocalDate.of(2026, 10, 2),
                1,
                List.of(
                        new StudentAttendanceRecord("std-1", AttendanceStatus.PRESENT),
                        new StudentAttendanceRecord("std-2", AttendanceStatus.ABSENT)
                )
        );

        BulkAttendanceResponse response = attendanceService.markAttendanceBulk(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalMarked()).isEqualTo(2);
        assertThat(response.getPresentCount()).isEqualTo(1);
        assertThat(response.getAbsentCount()).isEqualTo(1);
        verify(mongoTemplate).insert(anyList(), eq(Attendance.class));
    }

    @Test
    @DisplayName("Bulk attendance with fullDay=true successfully records all 5 periods")
    void markAttendanceBulk_fullDay_success() {
        setSecurityContext("staff-1", Role.STAFF, "dept-cs", null);

        Student s1 = new Student("2024CS101", "Student 1", "s1@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        s1.setId("std-1");
        Student s2 = new Student("2024CS102", "Student 2", "s2@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        s2.setId("std-2");

        given(studentRepository.findAllById(anyCollection())).willReturn(List.of(s1, s2));
        for (int p = 1; p <= 5; p++) {
            given(attendanceRepository.findByStudentIdInAndDateAndPeriod(anyCollection(), any(), eq(p)))
                    .willReturn(List.of());
        }

        BulkAttendanceRequest request = new BulkAttendanceRequest(
                LocalDate.of(2026, 10, 2),
                null,
                true,
                List.of(
                        new StudentAttendanceRecord("std-1", AttendanceStatus.PRESENT),
                        new StudentAttendanceRecord("std-2", AttendanceStatus.ABSENT)
                )
        );

        BulkAttendanceResponse response = attendanceService.markAttendanceBulk(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("full day (Periods 1–5)");
        assertThat(response.getTotalMarked()).isEqualTo(10); // 2 students * 5 periods = 10
        assertThat(response.getPresentCount()).isEqualTo(1);
        assertThat(response.getAbsentCount()).isEqualTo(1);
        verify(mongoTemplate).insert(anyList(), eq(Attendance.class));
    }


    @Test
    @DisplayName("Duplicate attendance prevention throws DuplicateResourceException")
    void markAttendanceBulk_duplicatePrevention() {
        setSecurityContext("staff-1", Role.STAFF, "dept-cs", null);

        Student s1 = new Student("2024CS101", "Student 1", "s1@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        s1.setId("std-1");

        given(studentRepository.findAllById(anyCollection())).willReturn(List.of(s1));

        Attendance existing = new Attendance("std-1", LocalDate.of(2026, 10, 2), 1,
                AttendanceStatus.PRESENT, "staff-1", java.time.Instant.now());
        given(attendanceRepository.findByStudentIdInAndDateAndPeriod(anyCollection(), any(), eq(1)))
                .willReturn(List.of(existing));

        BulkAttendanceRequest request = new BulkAttendanceRequest(
                LocalDate.of(2026, 10, 2),
                1,
                List.of(new StudentAttendanceRecord("std-1", AttendanceStatus.PRESENT))
        );

        assertThrows(DuplicateResourceException.class, () -> attendanceService.markAttendanceBulk(request));
    }

    @Test
    @DisplayName("Staff cannot edit or modify submitted attendance")
    void staffCannotEditSubmittedAttendance() {
        setSecurityContext("staff-1", Role.STAFF, "dept-cs", null);

        UpdateAttendanceRequest updateRequest = new UpdateAttendanceRequest(AttendanceStatus.ABSENT);
        assertThrows(AccessDeniedException.class,
                () -> attendanceService.updateAttendanceRecord("att-1", updateRequest));
    }

    @Test
    @DisplayName("HOD cannot mark attendance for students belonging to another department")
    void hodDepartmentRestriction_blockedForOtherDept() {
        setSecurityContext("hod-cs-1", Role.HOD, "dept-cs", null);

        // Student belongs to electrical department, not CS
        Student sEE = new Student("2024EE101", "EE Student", "ee@ams.edu", "1234567890",
                "dept-ee", "course-ee", ProgramType.UG, 1, "A", true);
        sEE.setId("std-ee-1");

        given(studentRepository.findAllById(anyCollection())).willReturn(List.of(sEE));

        BulkAttendanceRequest request = new BulkAttendanceRequest(
                LocalDate.of(2026, 10, 2),
                1,
                List.of(new StudentAttendanceRecord("std-ee-1", AttendanceStatus.PRESENT))
        );

        assertThrows(AccessDeniedException.class, () -> attendanceService.markAttendanceBulk(request));
    }

    @Test
    @DisplayName("STUDENT cannot view other students' attendance")
    void studentOwnershipRestriction_blockedForOtherStudent() {
        setSecurityContext("student-user-1", Role.STUDENT, "dept-cs", "std-101");

        Student otherStudent = new Student("2024CS102", "Other Student", "other@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        otherStudent.setId("std-102"); // Not std-101!

        given(studentRepository.findById("std-102")).willReturn(Optional.of(otherStudent));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.getStudentAttendance("std-102", 0, 20));
        assertThrows(AccessDeniedException.class,
                () -> attendanceService.getStudentAttendanceSummary("std-102"));
    }

    @Test
    @DisplayName("Summary calculation returns accurate counts and percentage")
    void summaryCalculation_accurateMetrics() {
        setSecurityContext("admin-1", Role.ADMIN, "dept-admin", null);

        Student student = new Student("2024CS101", "Aarav Sharma", "aarav@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        student.setId("std-101");

        given(studentRepository.findById("std-101")).willReturn(Optional.of(student));
        given(attendanceRepository.countByStudentId("std-101")).willReturn(40L);
        given(attendanceRepository.countByStudentIdAndStatus("std-101", AttendanceStatus.PRESENT)).willReturn(34L);
        given(attendanceRepository.countByStudentIdAndStatus("std-101", AttendanceStatus.ON_DUTY)).willReturn(0L);
        given(attendanceRepository.countByStudentIdAndStatus("std-101", AttendanceStatus.ABSENT)).willReturn(6L);

        AttendanceSummaryResponse summary = attendanceService.getStudentAttendanceSummary("std-101");

        assertThat(summary.getTotalClasses()).isEqualTo(40L);
        assertThat(summary.getPresent()).isEqualTo(34L);
        assertThat(summary.getAbsent()).isEqualTo(6L);
        assertThat(summary.getPercentage()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Attendance history pagination returns correct metadata")
    void attendanceHistory_pagination() {
        setSecurityContext("admin-1", Role.ADMIN, "dept-admin", null);

        List<Attendance> mockRecords = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            mockRecords.add(new Attendance("std-" + i, LocalDate.of(2026, 10, 2), 1,
                    AttendanceStatus.PRESENT, "staff-1", java.time.Instant.now()));
        }

        given(mongoTemplate.count(any(Query.class), eq(Attendance.class))).willReturn(55L);
        given(mongoTemplate.find(any(Query.class), eq(Attendance.class))).willReturn(mockRecords);

        PageResponse<AttendanceResponse> response = attendanceService.getAttendanceHistory(
                null, LocalDate.of(2026, 10, 2), null, null, 1, null, 0, 20
        );

        assertThat(response.getData()).hasSize(20);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(55L);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Attendance history with studentIds filter queries only target students")
    void attendanceHistory_withStudentIdsList() {
        setSecurityContext("admin-1", Role.ADMIN, "dept-admin", null);

        List<Attendance> mockRecords = List.of(
                new Attendance("std-1", LocalDate.of(2026, 10, 2), 1, AttendanceStatus.PRESENT, "staff-1", java.time.Instant.now()),
                new Attendance("std-2", LocalDate.of(2026, 10, 2), 1, AttendanceStatus.ABSENT, "staff-1", java.time.Instant.now())
        );

        given(mongoTemplate.count(any(Query.class), eq(Attendance.class))).willReturn(2L);
        given(mongoTemplate.find(any(Query.class), eq(Attendance.class))).willReturn(mockRecords);

        PageResponse<AttendanceResponse> response = attendanceService.getAttendanceHistory(
                null, List.of("std-1", "std-2"), LocalDate.of(2026, 10, 2), null, null, 1, null, 0, 20
        );

        assertThat(response.getData()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2L);
    }
}
