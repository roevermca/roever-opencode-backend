package com.ams.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ams.dto.CleanupRequest;
import com.ams.dto.CleanupResponse;
import com.ams.dto.StorageStatusResponse;
import com.ams.exception.AccessDeniedException;
import com.ams.model.Role;
import com.ams.security.AuthenticatedUser;
import com.ams.service.DatabaseStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest {

    @Mock
    private DatabaseStorageService storageService;

    @InjectMocks
    private SystemController systemController;

    private void setSecurityContext(Role role) {
        AuthenticatedUser user = new AuthenticatedUser("u1", "fb1", "admin@amsportal.edu", "Admin", role, "DEP1", null);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getStorageStatus should succeed for ADMIN role")
    void getStorageStatus_whenAdmin_shouldSucceed() {
        setSecurityContext(Role.ADMIN);
        StorageStatusResponse mockResponse = new StorageStatusResponse(
                1000000L, 0.95, 512.0, 0.19, "HEALTHY", 80.0, 500L, LocalDate.now(), LocalDate.now(), true
        );
        given(storageService.getStorageStatus()).willReturn(mockResponse);

        ResponseEntity<StorageStatusResponse> response = systemController.getStorageStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("HEALTHY");
    }

    @Test
    @DisplayName("getStorageStatus should deny access for STAFF role")
    void getStorageStatus_whenStaff_shouldThrowAccessDenied() {
        setSecurityContext(Role.STAFF);

        assertThrows(AccessDeniedException.class, () -> systemController.getStorageStatus());
    }

    @Test
    @DisplayName("triggerCleanup should succeed for VP role")
    void triggerCleanup_whenVp_shouldSucceed() {
        setSecurityContext(Role.VP);
        CleanupResponse mockResponse = new CleanupResponse(
                true, "Cleaned", false, 50L, 2L, LocalDate.now().minusDays(100), 120.0, 100.0
        );
        given(storageService.performManualCleanup(any(), eq(false))).willReturn(mockResponse);

        ResponseEntity<CleanupResponse> response = systemController.triggerCleanup(new CleanupRequest(null, false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRecordsDeleted()).isEqualTo(50L);
    }
}
