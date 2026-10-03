package com.ams.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.ams.dto.CourseRequest;
import com.ams.dto.CourseResponse;
import com.ams.exception.DuplicateResourceException;
import com.ams.model.Course;
import com.ams.model.ProgramType;
import com.ams.repository.CourseRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CourseService courseService;

    private CourseRequest courseRequest;
    private Course course;

    @BeforeEach
    void setUp() {
        courseRequest = new CourseRequest(
                "B.Tech Computer Science",
                "BTECH-CS",
                "dept-cs-01",
                ProgramType.UG,
                4,
                true
        );

        course = new Course(
                "B.Tech Computer Science",
                "BTECH-CS",
                "dept-cs-01",
                ProgramType.UG,
                4,
                true
        );
        course.setId("course-123");
    }

    @Test
    @DisplayName("Successfully create a new course")
    void createCourse_success() {
        given(courseRepository.existsByCode("BTECH-CS")).willReturn(false);
        given(courseRepository.save(any(Course.class))).willReturn(course);

        CourseResponse response = courseService.createCourse(courseRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("course-123");
        assertThat(response.getCode()).isEqualTo("BTECH-CS");
        assertThat(response.getDepartmentId()).isEqualTo("dept-cs-01");
        assertThat(response.getDurationYears()).isEqualTo(4);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when course code already exists")
    void createCourse_duplicateCode_throwsException() {
        given(courseRepository.existsByCode("BTECH-CS")).willReturn(true);

        assertThrows(DuplicateResourceException.class, () -> courseService.createCourse(courseRequest));
    }
}
