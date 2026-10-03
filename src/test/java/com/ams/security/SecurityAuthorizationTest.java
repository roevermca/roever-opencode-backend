package com.ams.security;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ams.dto.CourseRequest;
import com.ams.dto.DepartmentRequest;
import com.ams.dto.StudentRequest;
import com.ams.dto.UserRequest;
import com.ams.exception.AccessDeniedException;
import com.ams.model.Course;
import com.ams.model.Department;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.model.User;
import com.ams.repository.CourseRepository;
import com.ams.repository.DepartmentRepository;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;
import com.ams.service.CourseService;
import com.ams.service.DepartmentService;
import com.ams.service.StudentService;
import com.ams.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SecurityAuthorizationTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private StudentService studentService;

    @InjectMocks
    private DepartmentService departmentService;

    @InjectMocks
    private CourseService courseService;

    @InjectMocks
    private UserService userService;

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
    @DisplayName("ADMIN has full management access across all modules")
    void adminAccess_fullManagementAllowed() {
        setSecurityContext("admin-1", Role.ADMIN, "dept-admin", null);

        StudentRequest studentRequest = new StudentRequest("2024CS101", "Student 1", "s1@ams.edu",
                "1234567890", "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        Student student = new Student("2024CS101", "Student 1", "s1@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        student.setId("std-1");
        given(studentRepository.save(any())).willReturn(student);

        assertDoesNotThrow(() -> studentService.createStudent(studentRequest));

        DepartmentRequest deptRequest = new DepartmentRequest("CSE", "CS", true);
        Department dept = new Department("CSE", "CS", true);
        dept.setId("dept-1");
        given(departmentRepository.save(any())).willReturn(dept);

        assertDoesNotThrow(() -> departmentService.createDepartment(deptRequest));
    }

    @Test
    @DisplayName("VP has management access for students, departments, and courses")
    void vpAccess_managementAllowed() {
        setSecurityContext("vp-1", Role.VP, "dept-admin", null);

        CourseRequest courseRequest = new CourseRequest("B.Tech CS", "CS101", "dept-cs", ProgramType.UG, 4, true);
        Course course = new Course("B.Tech CS", "CS101", "dept-cs", ProgramType.UG, 4, true);
        course.setId("c-1");
        given(courseRepository.save(any())).willReturn(course);

        assertDoesNotThrow(() -> courseService.createCourse(courseRequest));

        UserRequest userRequest = new UserRequest("fb-uid-staff", "Staff Name", "staff@ams.edu", Role.STAFF, "dept-cs", true);
        User user = new User("fb-uid-staff", "Staff Name", "staff@ams.edu", Role.STAFF, "dept-cs", true);
        user.setId("u-1");
        given(userRepository.save(any())).willReturn(user);

        assertDoesNotThrow(() -> userService.createUser(userRequest));
    }

    @Test
    @DisplayName("HOD can manage students and courses in their own department")
    void hodOwnDepartmentAccess_allowed() {
        setSecurityContext("hod-cs-1", Role.HOD, "dept-cs", null);

        StudentRequest studentRequest = new StudentRequest("2024CS102", "CS Student", "cs2@ams.edu",
                "1234567890", "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        Student student = new Student("2024CS102", "CS Student", "cs2@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        student.setId("std-2");
        given(studentRepository.save(any())).willReturn(student);

        assertDoesNotThrow(() -> studentService.createStudent(studentRequest));
    }

    @Test
    @DisplayName("HOD cannot manage students or courses in other departments")
    void hodOtherDepartmentAccess_blocked() {
        setSecurityContext("hod-cs-1", Role.HOD, "dept-cs", null);

        StudentRequest studentOtherDept = new StudentRequest("2024EE101", "EE Student", "ee1@ams.edu",
                "1234567890", "dept-ee", "course-ee", ProgramType.UG, 1, "A", true);

        assertThrows(AccessDeniedException.class, () -> studentService.createStudent(studentOtherDept));

        CourseRequest courseOtherDept = new CourseRequest("B.Tech EE", "EE101", "dept-ee", ProgramType.UG, 4, true);
        assertThrows(AccessDeniedException.class, () -> courseService.createCourse(courseOtherDept));

        DepartmentRequest deptRequest = new DepartmentRequest("Electrical", "EE", true);
        assertThrows(AccessDeniedException.class, () -> departmentService.createDepartment(deptRequest));
    }

    @Test
    @DisplayName("STAFF management access is completely blocked")
    void staffManagementAccess_blocked() {
        setSecurityContext("staff-1", Role.STAFF, "dept-cs", null);

        StudentRequest studentRequest = new StudentRequest("2024CS103", "Student", "s3@ams.edu",
                "1234567890", "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);

        assertThrows(AccessDeniedException.class, () -> studentService.createStudent(studentRequest));

        UserRequest userRequest = new UserRequest("fb-uid-2", "Staff 2", "s2@ams.edu", Role.STAFF, "dept-cs", true);
        assertThrows(AccessDeniedException.class, () -> userService.createUser(userRequest));
    }

    @Test
    @DisplayName("STUDENT accessing another student's data is blocked")
    void studentAccessingOtherStudent_blocked() {
        setSecurityContext("student-user-1", Role.STUDENT, "dept-cs", "std-101");

        Student otherStudent = new Student("2024CS102", "Other Student", "other@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        otherStudent.setId("std-102"); // Not std-101!

        given(studentRepository.findById("std-102")).willReturn(Optional.of(otherStudent));

        assertThrows(AccessDeniedException.class, () -> studentService.getStudentById("std-102"));
    }

    @Test
    @DisplayName("STUDENT accessing their own data is allowed")
    void studentAccessingOwnData_allowed() {
        setSecurityContext("student-user-1", Role.STUDENT, "dept-cs", "std-101");

        Student ownStudent = new Student("2024CS101", "My Student Profile", "my@ams.edu", "1234567890",
                "dept-cs", "course-cs", ProgramType.UG, 1, "A", true);
        ownStudent.setId("std-101"); // Matches std-101!

        given(studentRepository.findById("std-101")).willReturn(Optional.of(ownStudent));

        assertDoesNotThrow(() -> {
            var response = studentService.getStudentById("std-101");
            assertThat(response.getId()).isEqualTo("std-101");
        });
    }
}
