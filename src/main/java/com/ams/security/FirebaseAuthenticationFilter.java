package com.ams.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.model.User;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String AUTHENTICATED_USER_ATTR = "AUTHENTICATED_USER";

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectProvider<StudentRepository> studentRepositoryProvider;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    private static class CachedUserEntry {
        final AuthenticatedUser authenticatedUser;
        final long timestamp;

        CachedUserEntry(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return (System.currentTimeMillis() - timestamp) > ttlMs;
        }
    }

    private static final Object ADMIN_PROVISION_LOCK = new Object();
    private final java.util.concurrent.ConcurrentHashMap<String, CachedUserEntry> userAuthCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final long AUTH_CACHE_TTL_MS = 2 * 60 * 1000L; // 2 minutes

    public FirebaseAuthenticationFilter(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectProvider<StudentRepository> studentRepositoryProvider,
            CustomAuthenticationEntryPoint authenticationEntryPoint) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.userRepositoryProvider = userRepositoryProvider;
        this.studentRepositoryProvider = studentRepositoryProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || "null".equalsIgnoreCase(token) || "undefined".equalsIgnoreCase(token)) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Bearer token cannot be empty"));
            return;
        }

        CachedUserEntry cached = userAuthCache.get(token);
        if (cached != null && !cached.isExpired(AUTH_CACHE_TTL_MS)) {
            AuthenticatedUser authenticatedUser = cached.authenticatedUser;
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(AUTHENTICATED_USER_ATTR, authenticatedUser);

            filterChain.doFilter(request, response);
            return;
        }

        if (userAuthCache.size() > 500) {
            userAuthCache.entrySet().removeIf(e -> e.getValue().isExpired(AUTH_CACHE_TTL_MS));
        }

        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            UserRepository userRepository = userRepositoryProvider.getIfAvailable();
            if (userRepository != null) {
                String normalizedToken = token.trim();
                String email = null;
                String firebaseUid = null;
                String tokenName = null;

                // 1. If token is a JWT (e.g. Firebase ID Token)
                if (normalizedToken.contains(".")) {
                    String[] parts = normalizedToken.split("\\.");
                    if (parts.length >= 2) {
                        try {
                            String payloadPart = parts[1].trim();
                            byte[] decodedBytes;
                            try {
                                decodedBytes = Base64.getUrlDecoder().decode(payloadPart);
                            } catch (Exception e1) {
                                try {
                                    decodedBytes = Base64.getDecoder().decode(payloadPart);
                                } catch (Exception e2) {
                                    String padded = payloadPart + "=".repeat((4 - payloadPart.length() % 4) % 4);
                                    decodedBytes = Base64.getUrlDecoder().decode(padded);
                                }
                            }
                            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                            JsonNode node = objectMapper.readTree(payloadJson);
                            if (node.hasNonNull("email")) {
                                email = node.get("email").asText().toLowerCase().trim();
                            }
                            if (node.hasNonNull("user_id")) {
                                firebaseUid = node.get("user_id").asText().trim();
                            } else if (node.hasNonNull("sub")) {
                                firebaseUid = node.get("sub").asText().trim();
                            }
                            if (node.hasNonNull("name")) {
                                tokenName = node.get("name").asText().trim();
                            }
                        } catch (Exception e) {
                            logger.warn("Could not decode JWT payload in dev mode: {}", e.getMessage());
                        }
                    }
                }

                // 2. Direct email or UID passed in Bearer token (for dev/curl test)
                if (email == null && normalizedToken.contains("@")) {
                    email = normalizedToken.toLowerCase().trim();
                }
                if (firebaseUid == null && !normalizedToken.contains(".")) {
                    firebaseUid = normalizedToken.trim();
                }

                // 3. Exact matching shortcuts for local curl tests (e.g. Bearer admin / Bearer vp)
                if (email == null && "admin".equalsIgnoreCase(normalizedToken)) {
                    email = "admin@amsportal.edu";
                } else if (email == null && "vp".equalsIgnoreCase(normalizedToken)) {
                    email = "vp@amsportal.edu";
                } else if (email == null && "hod".equalsIgnoreCase(normalizedToken)) {
                    email = "hod.cs@amsportal.edu";
                } else if (email == null && "staff".equalsIgnoreCase(normalizedToken)) {
                    email = "staff@amsportal.edu";
                } else if (email == null && "student".equalsIgnoreCase(normalizedToken)) {
                    email = "student@amsportal.edu";
                }

                Optional<User> devUser = Optional.empty();
                if (firebaseUid != null) {
                    devUser = userRepository.findByFirebaseUid(firebaseUid);
                }
                if (devUser.isEmpty() && email != null) {
                    devUser = userRepository.findByEmail(email);
                }

                // Master Admin Auto-Heal: guarantee roevermca09@gmail.com is ALWAYS registered as ADMIN (thread-safe)
                if (devUser.isEmpty() && email != null && ("roevermca09@gmail.com".equalsIgnoreCase(email) || email.startsWith("roevermca09@"))) {
                    synchronized (ADMIN_PROVISION_LOCK) {
                        devUser = userRepository.findByEmail(email.toLowerCase().trim());
                        if (devUser.isEmpty()) {
                            logger.info("Auto-provisioning Master Admin account for {}", email);
                            User adminUser = new User(
                                    firebaseUid != null ? firebaseUid : "firebase-admin-master",
                                    tokenName != null ? tokenName : "Roever Administrator",
                                    email.toLowerCase().trim(),
                                    Role.ADMIN,
                                    "Administration",
                                    null,
                                    true
                            );
                            devUser = Optional.of(userRepository.save(adminUser));
                        }
                    }
                }

                if (devUser.isEmpty()) {
                    logger.warn("Dev mode authentication failed for unknown user: email={}, uid={}", email, firebaseUid);
                    authenticationEntryPoint.commence(request, response,
                            new BadCredentialsException("User account not registered in system"));
                    return;
                }

                if (devUser.isPresent()) {
                    User user = devUser.get();
                    if (!user.isActive()) {
                        logger.warn("Inactive user attempted to authenticate in dev mode: {}", user.getEmail());
                        authenticationEntryPoint.commence(request, response,
                                new DisabledException("User account is inactive"));
                        return;
                    }

                    // Link actual Firebase UID if not yet linked
                    if (firebaseUid != null && (user.getFirebaseUid() == null || user.getFirebaseUid().startsWith("usr_") || user.getFirebaseUid().startsWith("stu_") || user.getFirebaseUid().startsWith("firebase-"))) {
                        user.setFirebaseUid(firebaseUid);
                        userRepository.save(user);
                    }

                    String studentId = null;
                    if (user.getRole() == Role.STUDENT) {
                        StudentRepository studentRepository = studentRepositoryProvider.getIfAvailable();
                        if (studentRepository != null && user.getEmail() != null) {
                            studentId = studentRepository.findByEmail(user.getEmail().toLowerCase().trim())
                                    .map(Student::getId)
                                    .orElse(null);
                        }
                    }

                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                            user.getId(),
                            user.getFirebaseUid(),
                            user.getEmail(),
                            user.getName() != null ? user.getName() : (tokenName != null ? tokenName : user.getEmail()),
                            user.getRole(),
                            user.getDepartmentId(),
                            user.getCourseId(),
                            studentId
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    request.setAttribute(AUTHENTICATED_USER_ATTR, authenticatedUser);
                    userAuthCache.put(token, new CachedUserEntry(authenticatedUser));

                    filterChain.doFilter(request, response);
                    return;
                }
            }

            logger.warn("FirebaseAuth instance not available. Firebase credentials may be missing.");
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Firebase authentication service is not configured"));
            return;
        }

        FirebaseToken decodedToken;
        try {
            decodedToken = firebaseAuth.verifyIdToken(token);
        } catch (Exception e) {
            logger.warn("Firebase ID token verification failed: {}", e.getMessage());
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Invalid or expired Firebase token"));
            return;
        }

        String firebaseUid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = decodedToken.getName();

        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userRepository == null) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Database service unavailable"));
            return;
        }

        Optional<User> userOptional = userRepository.findByFirebaseUid(firebaseUid);
        if (userOptional.isEmpty() && email != null) {
            userOptional = userRepository.findByEmail(email.toLowerCase().trim());
        }

        // Master Admin Auto-Heal: guarantee roevermca09@gmail.com is ALWAYS registered as ADMIN (thread-safe)
        if (userOptional.isEmpty() && email != null && ("roevermca09@gmail.com".equalsIgnoreCase(email) || email.startsWith("roevermca09@"))) {
            synchronized (ADMIN_PROVISION_LOCK) {
                userOptional = userRepository.findByEmail(email.toLowerCase().trim());
                if (userOptional.isEmpty()) {
                    logger.info("Auto-provisioning Master Admin account for {} with Firebase UID {}", email, firebaseUid);
                    User adminUser = new User(
                            firebaseUid,
                            name != null ? name : "Roever Administrator",
                            email.toLowerCase().trim(),
                            Role.ADMIN,
                            "Administration",
                            null,
                            true
                    );
                    userOptional = Optional.of(userRepository.save(adminUser));
                }
            }
        }

        if (userOptional.isEmpty()) {
            logger.warn("User with Firebase UID {} not found in database", firebaseUid);
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("User account not registered in system"));
            return;
        }

        User user = userOptional.get();
        if (!user.isActive()) {
            logger.warn("Inactive user attempted to authenticate: {}", email);
            authenticationEntryPoint.commence(request, response,
                    new DisabledException("User account is inactive"));
            return;
        }

        // Link actual Firebase UID if not yet linked
        if (firebaseUid != null && (user.getFirebaseUid() == null || user.getFirebaseUid().startsWith("usr_") || user.getFirebaseUid().startsWith("stu_") || user.getFirebaseUid().startsWith("firebase-"))) {
            user.setFirebaseUid(firebaseUid);
            userRepository.save(user);
        }

        String studentId = null;
        if (user.getRole() == Role.STUDENT) {
            StudentRepository studentRepository = studentRepositoryProvider.getIfAvailable();
            if (studentRepository != null && email != null) {
                studentId = studentRepository.findByEmail(email.toLowerCase().trim())
                        .map(Student::getId)
                        .orElse(null);
            }
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                firebaseUid,
                email != null ? email : user.getEmail(),
                name != null ? name : user.getName(),
                user.getRole(),
                user.getDepartmentId(),
                user.getCourseId(),
                studentId
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(AUTHENTICATED_USER_ATTR, authenticatedUser);
        userAuthCache.put(token, new CachedUserEntry(authenticatedUser));

        filterChain.doFilter(request, response);
    }
}
