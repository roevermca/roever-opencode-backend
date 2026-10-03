package com.ams.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ams.dto.DepartmentRequest;
import com.ams.dto.DepartmentResponse;
import com.ams.exception.DuplicateResourceException;
import com.ams.model.Department;
import com.ams.repository.DepartmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private DepartmentRequest departmentRequest;
    private Department department;

    @BeforeEach
    void setUp() {
        departmentRequest = new DepartmentRequest("Computer Science", "CSE", true);
        department = new Department("Computer Science", "CSE", true);
        department.setId("dept-123");
    }

    @Test
    @DisplayName("Successfully create a new department")
    void createDepartment_success() {
        given(departmentRepository.existsByCode("CSE")).willReturn(false);
        given(departmentRepository.save(any(Department.class))).willReturn(department);

        DepartmentResponse response = departmentService.createDepartment(departmentRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("dept-123");
        assertThat(response.getCode()).isEqualTo("CSE");
        assertThat(response.getName()).isEqualTo("Computer Science");
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when department code already exists")
    void createDepartment_duplicateCode_throwsException() {
        given(departmentRepository.existsByCode("CSE")).willReturn(true);

        assertThrows(DuplicateResourceException.class, () -> departmentService.createDepartment(departmentRequest));
    }
}
