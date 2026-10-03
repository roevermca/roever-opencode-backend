package com.ams.security;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ams.model.Role;
import com.ams.model.User;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FirebaseAuthenticationFilterTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @Mock
    private ObjectProvider<UserRepository> userRepositoryProvider;

    @Mock
    private ObjectProvider<StudentRepository> studentRepositoryProvider;

    private CustomAuthenticationEntryPoint authenticationEntryPoint;
    private FirebaseAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint = new CustomAuthenticationEntryPoint();

        given(firebaseAuthProvider.getIfAvailable()).willReturn(firebaseAuth);
        given(userRepositoryProvider.getIfAvailable()).willReturn(userRepository);
        given(studentRepositoryProvider.getIfAvailable()).willReturn(studentRepository);

        filter = new FirebaseAuthenticationFilter(
                firebaseAuthProvider,
                userRepositoryProvider,
                studentRepositoryProvider,
                authenticationEntryPoint
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid Firebase token sets SecurityContext with AuthenticatedUser")
    void validToken_setsSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-firebase-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        FirebaseToken mockToken = mock(FirebaseToken.class);
        given(mockToken.getUid()).willReturn("firebase-uid-admin");
        given(mockToken.getEmail()).willReturn("admin@amsportal.edu");
        given(mockToken.getName()).willReturn("Admin User");

        given(firebaseAuth.verifyIdToken("valid-firebase-token")).willReturn(mockToken);

        User mockUser = new User(
                "firebase-uid-admin",
                "Admin User",
                "admin@amsportal.edu",
                Role.ADMIN,
                "dept-admin",
                true
        );
        mockUser.setId("user-id-admin");

        given(userRepository.findByFirebaseUid("firebase-uid-admin")).willReturn(Optional.of(mockUser));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) auth.getPrincipal();
        assertThat(authenticatedUser.getFirebaseUid()).isEqualTo("firebase-uid-admin");
        assertThat(authenticatedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Invalid Firebase token returns 401 Unauthorized JSON")
    void invalidToken_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/students");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        given(firebaseAuth.verifyIdToken("invalid-token")).willThrow(new IllegalArgumentException("Invalid token signature"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false");
        assertThat(response.getContentAsString()).contains("Invalid or expired Firebase token");
    }

    @Test
    @DisplayName("Missing Authorization header allows filter chain to proceed for public endpoints")
    void missingToken_proceedsDownFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = mock(MockFilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Valid token but inactive user in MongoDB returns 401 Unauthorized")
    void inactiveUser_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/students");
        request.addHeader("Authorization", "Bearer valid-token-inactive-user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        FirebaseToken mockToken = mock(FirebaseToken.class);
        given(mockToken.getUid()).willReturn("firebase-uid-inactive");
        given(mockToken.getEmail()).willReturn("inactive@amsportal.edu");

        given(firebaseAuth.verifyIdToken("valid-token-inactive-user")).willReturn(mockToken);

        User inactiveUser = new User(
                "firebase-uid-inactive",
                "Inactive User",
                "inactive@amsportal.edu",
                Role.STAFF,
                "dept-cs",
                false // inactive!
        );
        given(userRepository.findByFirebaseUid("firebase-uid-inactive")).willReturn(Optional.of(inactiveUser));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false");
        assertThat(response.getContentAsString()).contains("inactive");
    }
}
