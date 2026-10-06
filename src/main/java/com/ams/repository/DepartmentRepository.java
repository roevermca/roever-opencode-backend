package com.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ams.model.Department;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    boolean existsByCode(String code);

    List<Department> findByActiveTrue();
}
