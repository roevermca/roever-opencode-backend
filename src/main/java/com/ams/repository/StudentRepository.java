package com.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ams.model.Student;

@Repository
public interface StudentRepository extends MongoRepository<Student, String> {

    Optional<Student> findByRollNo(String rollNo);

    Optional<Student> findByEmail(String email);

    boolean existsByRollNo(String rollNo);

    List<Student> findByDepartmentIdAndCourseIdAndYearAndSectionAndActiveTrue(
        String departmentId,
        String courseId,
        int year,
        String section
    );

    List<Student> findByDepartmentIdAndActiveTrue(String departmentId);

    long countByActiveTrue();
}
