package com.ams.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.ams.dto.PageResponse;
import com.ams.dto.StudentRequest;
import com.ams.dto.StudentResponse;
import com.ams.exception.DuplicateResourceException;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.model.User;
import com.ams.repository.AttendanceArchiveRepository;
import com.ams.repository.AttendanceRepository;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;
import java.util.Optional;



import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceArchiveRepository archiveRepository;

    @InjectMocks
    private StudentService studentService;


    private StudentRequest studentRequest;
    private Student student;

    @BeforeEach
    void setUp() {
        studentRequest = new StudentRequest(
                "2024CS101",
                "Aarav Sharma",
                "aarav@amsportal.edu",
                "9876543210",
                "dept-cs-01",
                "course-cs-01",
                ProgramType.UG,
                1,
                "A",
                true
        );

        student = new Student(
                "2024CS101",
                "Aarav Sharma",
                "aarav@amsportal.edu",
                "9876543210",
                "dept-cs-01",
                "course-cs-01",
                ProgramType.UG,
                1,
                "A",
                true
        );
        student.setId("std-123");
    }

    @Test
    @DisplayName("Successfully create a new student")
    void createStudent_success() {
        given(studentRepository.existsByRollNo("2024CS101")).willReturn(false);
        given(studentRepository.save(any(Student.class))).willReturn(student);

        StudentResponse response = studentService.createStudent(studentRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("std-123");
        assertThat(response.getRollNo()).isEqualTo("2024CS101");
        assertThat(response.getName()).isEqualTo("Aarav Sharma");
        assertThat(response.getDepartmentId()).isEqualTo("dept-cs-01");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when rollNo already exists")
    void createStudent_duplicateRollNo_throwsException() {
        given(studentRepository.existsByRollNo("2024CS101")).willReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(studentRequest));
    }

    @Test
    @DisplayName("Throw IllegalArgumentException when PG year exceeds 2")
    void createStudent_pgYearExceedsTwo_throwsException() {
        StudentRequest pgReq = new StudentRequest(
                "2024MCA01", "Priya", "priya@amsportal.edu", "9876543210",
                "dept-ca", "course-mca", ProgramType.PG, 3, "A", true
        );
        assertThrows(IllegalArgumentException.class, () -> studentService.createStudent(pgReq));
    }

    @Test
    @DisplayName("Throw IllegalArgumentException when UG year exceeds 3")
    void createStudent_ugYearExceedsThree_throwsException() {
        StudentRequest ugReq = new StudentRequest(
                "2024BCA01", "Kumar", "kumar@amsportal.edu", "9876543210",
                "dept-ca", "course-bca", ProgramType.UG, 4, "A", true
        );
        assertThrows(IllegalArgumentException.class, () -> studentService.createStudent(ugReq));
    }

    @Test
    @DisplayName("Retrieve paginated students with metadata")
    void getStudents_pagination() {
        List<Student> mockList = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Student s = new Student(
                    "2024CS" + (100 + i),
                    "Student " + i,
                    "student" + i + "@amsportal.edu",
                    "9876543210",
                    "dept-cs-01",
                    "course-cs-01",
                    ProgramType.UG,
                    1,
                    "A",
                    true
            );
            s.setId("id-" + i);
            mockList.add(s);
        }

        given(mongoTemplate.count(any(Query.class), eq(Student.class))).willReturn(45L);
        given(mongoTemplate.find(any(Query.class), eq(Student.class))).willReturn(mockList);

        PageResponse<StudentResponse> pageResponse = studentService.getStudents(
                "dept-cs-01", null, null, null, null, null, 0, 20
        );

        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.getData()).hasSize(20);
        assertThat(pageResponse.getPage()).isEqualTo(0);
        assertThat(pageResponse.getSize()).isEqualTo(20);
        assertThat(pageResponse.getTotalElements()).isEqualTo(45L);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("deleteStudent cascades deletion to User, Attendance, and Archive repositories")
    void deleteStudent_cascadeSuccess() {
        given(studentRepository.findById("std-123")).willReturn(Optional.of(student));
        User user = new User("std-123", "Aarav", "aarav@amsportal.edu", Role.STUDENT, "dept-cs-01", true);
        given(userRepository.findByEmail("aarav@amsportal.edu")).willReturn(Optional.of(user));

        studentService.deleteStudent("std-123");

        verify(userRepository).delete(user);
        verify(attendanceRepository).deleteByStudentId("std-123");
        verify(attendanceRepository).deleteByStudentId("2024CS101");
        verify(archiveRepository).deleteByStudentId("std-123");
        verify(archiveRepository).deleteByStudentId("2024CS101");
        verify(studentRepository).delete(student);
    }
}

