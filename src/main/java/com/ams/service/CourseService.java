package com.ams.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ams.dto.CourseRequest;
import com.ams.dto.CourseResponse;
import com.ams.exception.DuplicateResourceException;
import com.ams.exception.ResourceNotFoundException;
import com.ams.model.Course;
import com.ams.model.ProgramType;
import com.ams.repository.CourseRepository;
import com.ams.security.SecurityUtils;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final MongoTemplate mongoTemplate;

    public CourseService(CourseRepository courseRepository, MongoTemplate mongoTemplate) {
        this.courseRepository = courseRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<CourseResponse> getCourses(
            String departmentId,
            ProgramType programType,
            Integer durationYears,
            Boolean active) {

        List<Criteria> filters = new ArrayList<>();

        if (departmentId != null && !departmentId.isBlank()) {
            filters.add(Criteria.where("departmentId").is(departmentId.trim()));
        }
        if (programType != null) {
            filters.add(Criteria.where("programType").is(programType));
        }
        if (durationYears != null) {
            filters.add(Criteria.where("durationYears").is(durationYears));
        }
        if (active != null) {
            filters.add(Criteria.where("active").is(active));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }
        query.with(Sort.by("name").ascending());

        List<Course> courses = mongoTemplate.find(query, Course.class);
        return courses.stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    public CourseResponse getCourseById(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
        return CourseResponse.fromEntity(course);
    }

    public CourseResponse createCourse(CourseRequest request) {
        SecurityUtils.enforceCourseManagementAccess(request.getDepartmentId());

        String code = request.getCode().trim().toUpperCase();
        if (courseRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Course with code " + code + " already exists");
        }

        Course course = new Course(
                request.getName().trim(),
                code,
                request.getDepartmentId().trim(),
                request.getProgramType(),
                request.getDurationYears(),
                request.getActive() != null ? request.getActive() : true
        );

        Course saved = courseRepository.save(course);
        return CourseResponse.fromEntity(saved);
    }

    public CourseResponse updateCourse(String id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

        SecurityUtils.enforceCourseManagementAccess(course.getDepartmentId());
        SecurityUtils.enforceCourseManagementAccess(request.getDepartmentId());

        String newCode = request.getCode().trim().toUpperCase();
        if (!course.getCode().equalsIgnoreCase(newCode) && courseRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Course with code " + newCode + " already exists");
        }

        course.setName(request.getName().trim());
        course.setCode(newCode);
        course.setDepartmentId(request.getDepartmentId().trim());
        course.setProgramType(request.getProgramType());
        course.setDurationYears(request.getDurationYears());
        if (request.getActive() != null) {
            course.setActive(request.getActive());
        }

        Course updated = courseRepository.save(course);
        return CourseResponse.fromEntity(updated);
    }

    public void deleteCourse(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
        SecurityUtils.enforceCourseManagementAccess(course.getDepartmentId());
        courseRepository.delete(course);
    }
}
