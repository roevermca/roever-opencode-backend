package com.ams.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ams.dto.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with structured error response")
    void handleResourceNotFound_returns404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/students/123");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Student not found with id 123"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Student not found with id 123");
        assertThat(response.getBody().getPath()).isEqualTo("/api/students/123");
    }

    @Test
    @DisplayName("DuplicateResourceException returns 409 Conflict with structured error response")
    void handleDuplicateResource_returns409() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/attendance");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(
                new DuplicateResourceException("Attendance record already exists for studentId + date + period"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).contains("Attendance record already exists");
        assertThat(response.getBody().getPath()).isEqualTo("/api/attendance");
    }
}
