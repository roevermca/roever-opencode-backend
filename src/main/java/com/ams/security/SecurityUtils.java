package com.ams.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ams.exception.AccessDeniedException;
import com.ams.model.Role;

import jakarta.servlet.http.HttpServletRequest;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            Object userObj = request.getAttribute(FirebaseAuthenticationFilter.AUTHENTICATED_USER_ATTR);
            if (userObj instanceof AuthenticatedUser authenticatedUser) {
                return authenticatedUser;
            }
        }
        return null;
    }

    public static Role getCurrentUserRole() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    public static void enforceDepartmentManagementAccess() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role != Role.ADMIN && role != Role.VP) {
            throw new AccessDeniedException("Forbidden: Only ADMIN or VP can manage departments");
        }
    }

    public static void checkDepartmentManagementAccess() {
        enforceDepartmentManagementAccess();
    }

    public static void enforceCourseManagementAccess(String courseDepartmentId) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.ADMIN || role == Role.VP) {
            return;
        }
        if (role == Role.HOD) {
            if (user.getDepartmentId() != null && user.getDepartmentId().equals(courseDepartmentId)) {
                return;
            }
            throw new AccessDeniedException("Forbidden: HOD can only manage courses in their own department");
        }
        throw new AccessDeniedException("Forbidden: " + role + " cannot manage courses");
    }

    public static void enforceStaffManagementAccess() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role != Role.ADMIN && role != Role.VP) {
            throw new AccessDeniedException("Forbidden: Only ADMIN or VP can create, update, or delete staff accounts");
        }
    }

    public static void enforceStudentWriteAccess(String studentDepartmentId) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.ADMIN || role == Role.VP) {
            return;
        }
        if (role == Role.HOD) {
            if (user.getDepartmentId() != null && user.getDepartmentId().equals(studentDepartmentId)) {
                return;
            }
            throw new AccessDeniedException("Forbidden: HOD can only manage students in their own department");
        }
        throw new AccessDeniedException("Forbidden: " + role + " is not permitted to manage students");
    }

    public static void enforceStudentReadAccess(String studentId, String studentDepartmentId) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.ADMIN || role == Role.VP) {
            return;
        }
        if (role == Role.HOD) {
            if (user.getDepartmentId() != null && user.getDepartmentId().equals(studentDepartmentId)) {
                return;
            }
            throw new AccessDeniedException("Forbidden: HOD cannot access students from other departments");
        }
        if (role == Role.STAFF) {
            return;
        }
        if (role == Role.STUDENT) {
            if (user.getStudentId() != null && user.getStudentId().equals(studentId)) {
                return;
            }
            throw new AccessDeniedException("Forbidden: Students are only permitted to access their own data");
        }
        throw new AccessDeniedException("Forbidden: Access denied");
    }

    public static void enforceAttendanceMarkAccess() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.STUDENT) {
            throw new AccessDeniedException("Forbidden: Students cannot mark attendance");
        }
    }

    public static void enforceAttendanceEditAccess() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.STAFF) {
            throw new AccessDeniedException("Forbidden: Staff cannot modify or delete submitted attendance records");
        }
        if (role != Role.ADMIN && role != Role.VP) {
            throw new AccessDeniedException("Forbidden: Only ADMIN or VP can correct or modify attendance records");
        }
    }

    public static void enforceStudentAttendanceReadAccess(String studentId, String studentDepartmentId) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.ADMIN || role == Role.VP || role == Role.STAFF) {
            return;
        }
        if (role == Role.HOD) {
            if (user.getDepartmentId() != null && user.getDepartmentId().equals(studentDepartmentId)) {
                return;
            }
            throw new AccessDeniedException("Forbidden: HOD can only view attendance for students in their own department");
        }
        if (role == Role.STUDENT) {
            if (user.getStudentId() != null && user.getStudentId().equals(studentId)) {
                return;
            }
            throw new AccessDeniedException("Forbidden: Students can only access their own attendance");
        }
        throw new AccessDeniedException("Forbidden: Access denied");
    }

    public static void enforceReportsAccess() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role == Role.STAFF || role == Role.STUDENT) {
            throw new AccessDeniedException("Forbidden: " + role + " is not permitted to access reports");
        }
    }
}

