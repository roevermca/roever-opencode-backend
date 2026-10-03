package com.ams.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ams.model.Role;

public class AuthenticatedUser implements UserDetails {

    private final String userId;
    private final String firebaseUid;
    private final String email;
    private final String name;
    private final Role role;
    private final String departmentId;
    private final String courseId;
    private final String studentId;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(
            String userId,
            String firebaseUid,
            String email,
            String name,
            Role role,
            String departmentId,
            String studentId) {
        this(userId, firebaseUid, email, name, role, departmentId, null, studentId);
    }

    public AuthenticatedUser(
            String userId,
            String firebaseUid,
            String email,
            String name,
            Role role,
            String departmentId,
            String courseId,
            String studentId) {
        this.userId = userId;
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.name = name;
        this.role = role;
        this.departmentId = departmentId;
        this.courseId = courseId;
        this.studentId = studentId;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public String getUserId() {
        return userId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email != null ? email : firebaseUid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
