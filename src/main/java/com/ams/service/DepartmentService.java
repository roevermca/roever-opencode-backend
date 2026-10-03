package com.ams.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ams.dto.DepartmentRequest;
import com.ams.dto.DepartmentResponse;
import com.ams.exception.DuplicateResourceException;
import com.ams.exception.ResourceNotFoundException;
import com.ams.model.Department;
import com.ams.repository.DepartmentRepository;
import com.ams.security.SecurityUtils;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getDepartments(Boolean activeOnly) {
        List<Department> departments;
        if (Boolean.TRUE.equals(activeOnly)) {
            departments = departmentRepository.findByActiveTrue();
        } else {
            departments = departmentRepository.findAll(Sort.by("name").ascending());
        }
        return departments.stream()
                .map(DepartmentResponse::fromEntity)
                .toList();
    }

    public DepartmentResponse getDepartmentById(String id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return DepartmentResponse.fromEntity(department);
    }

    public DepartmentResponse createDepartment(DepartmentRequest request) {
        SecurityUtils.checkDepartmentManagementAccess();

        String code = request.getCode().trim().toUpperCase();
        if (departmentRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Department with code " + code + " already exists");
        }

        Department department = new Department(
                request.getName().trim(),
                code,
                request.getActive() != null ? request.getActive() : true
        );

        Department saved = departmentRepository.save(department);
        return DepartmentResponse.fromEntity(saved);
    }

    public DepartmentResponse updateDepartment(String id, DepartmentRequest request) {
        SecurityUtils.checkDepartmentManagementAccess();

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        String newCode = request.getCode().trim().toUpperCase();
        if (!department.getCode().equalsIgnoreCase(newCode) && departmentRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Department with code " + newCode + " already exists");
        }

        department.setName(request.getName().trim());
        department.setCode(newCode);
        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        Department updated = departmentRepository.save(department);
        return DepartmentResponse.fromEntity(updated);
    }

    public void deleteDepartment(String id) {
        SecurityUtils.checkDepartmentManagementAccess();

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        departmentRepository.delete(department);
    }
}
