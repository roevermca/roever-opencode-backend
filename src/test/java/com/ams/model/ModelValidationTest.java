package com.ams.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ModelValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid Attendance model should have zero constraint violations")
    void validAttendance_shouldPassValidation() {
        Attendance attendance = new Attendance(
                "student-123",
                LocalDate.of(2026, 10, 2),
                1,
                AttendanceStatus.PRESENT,
                "staff-001",
                Instant.now()
        );

        Set<ConstraintViolation<Attendance>> violations = validator.validate(attendance);
        assertThat(violations).isEmpty();
        assertThat(attendance.getStudentId()).isEqualTo("student-123");
        assertThat(attendance.getDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(attendance.getPeriod()).isEqualTo(1);
        assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(attendance.getMarkedBy()).isEqualTo("staff-001");
        assertThat(attendance.getMarkedAt()).isNotNull();
    }

    @Test
    @DisplayName("Attendance with period outside 1-5 should fail validation")
    void attendanceWithInvalidPeriod_shouldFailValidation() {
        Attendance invalidPeriodAttendance = new Attendance(
                "student-123",
                LocalDate.now(),
                6, // Period must be <= 5
                AttendanceStatus.PRESENT,
                "staff-001",
                Instant.now()
        );

        Set<ConstraintViolation<Attendance>> violations = validator.validate(invalidPeriodAttendance);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("period"));
    }

    @Test
    @DisplayName("Valid User model should have correct attributes and pass validation")
    void validUser_shouldPassValidation() {
        User user = new User(
                "firebase-uid-123",
                "Dr. Rajesh Sharma",
                "admin@amsportal.edu",
                Role.ADMIN,
                "dept-admin-01",
                true
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).isEmpty();
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getEmail()).isEqualTo("admin@amsportal.edu");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("Valid Student model should have department and course references without duplicating names")
    void validStudent_shouldPassValidation() {
        Student student = new Student(
                "2024CS101",
                "Aarav Sharma",
                "aarav@amsportal.edu",
                "9876543210",
                "dept-cs-01",
                "course-btech-cs",
                ProgramType.UG,
                1,
                "A",
                true
        );

        Set<ConstraintViolation<Student>> violations = validator.validate(student);
        assertThat(violations).isEmpty();
        assertThat(student.getRollNo()).isEqualTo("2024CS101");
        assertThat(student.getDepartmentId()).isEqualTo("dept-cs-01");
        assertThat(student.getCourseId()).isEqualTo("course-btech-cs");
        assertThat(student.getProgramType()).isEqualTo(ProgramType.UG);
        assertThat(student.getYear()).isEqualTo(1);
        assertThat(student.getSection()).isEqualTo("A");
    }

    @Test
    @DisplayName("Valid Department and Course models should hold reference and codes")
    void validDepartmentAndCourse_shouldPassValidation() {
        Department dept = new Department("Computer Science and Engineering", "CSE", true);
        Set<ConstraintViolation<Department>> deptViolations = validator.validate(dept);
        assertThat(deptViolations).isEmpty();
        assertThat(dept.getCode()).isEqualTo("CSE");

        Course course = new Course("B.Tech Computer Science", "BTECH-CS", "dept-cs-01", ProgramType.UG, 4, true);
        Set<ConstraintViolation<Course>> courseViolations = validator.validate(course);
        assertThat(courseViolations).isEmpty();
        assertThat(course.getCode()).isEqualTo("BTECH-CS");
        assertThat(course.getDepartmentId()).isEqualTo("dept-cs-01");
        assertThat(course.getDurationYears()).isEqualTo(4);
    }

    @Test
    @DisplayName("BulkAttendanceRequest with period outside 1-5 or empty records fails validation")
    void bulkAttendanceRequestValidation() {
        com.ams.dto.BulkAttendanceRequest validRequest = new com.ams.dto.BulkAttendanceRequest(
                LocalDate.now(),
                3,
                List.of(new com.ams.dto.StudentAttendanceRecord("std-1", AttendanceStatus.PRESENT))
        );
        assertThat(validator.validate(validRequest)).isEmpty();

        com.ams.dto.BulkAttendanceRequest invalidPeriodRequest = new com.ams.dto.BulkAttendanceRequest(
                LocalDate.now(),
                6, // invalid period > 5
                List.of(new com.ams.dto.StudentAttendanceRecord("std-1", AttendanceStatus.PRESENT))
        );
        assertThat(validator.validate(invalidPeriodRequest)).anyMatch(v -> v.getPropertyPath().toString().equals("period"));

        com.ams.dto.BulkAttendanceRequest emptyRecordsRequest = new com.ams.dto.BulkAttendanceRequest(
                LocalDate.now(),
                1,
                List.of() // empty records
        );
        assertThat(validator.validate(emptyRecordsRequest)).anyMatch(v -> v.getPropertyPath().toString().equals("records"));
    }
}
