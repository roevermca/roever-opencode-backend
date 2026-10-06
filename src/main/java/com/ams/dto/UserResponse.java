package com.ams.dto;

import com.ams.model.Role;
import com.ams.model.User;

public class UserResponse {

    private String id;
    private String firebaseUid;
    private String name;
    private String email;
    private Role role;
    private String departmentId;
    private String courseId;
    private String phone;
    private boolean active;

    public UserResponse() {
    }

    public UserResponse(String id, String firebaseUid, String name, String email, Role role, String departmentId, boolean active) {
        this(id, firebaseUid, name, email, role, departmentId, null, null, active);
    }

    public UserResponse(String id, String firebaseUid, String name, String email, Role role, String departmentId, String courseId, boolean active) {
        this(id, firebaseUid, name, email, role, departmentId, courseId, null, active);
    }

    public UserResponse(String id, String firebaseUid, String name, String email, Role role, String departmentId, String courseId, String phone, boolean active) {
        this.id = id;
        this.firebaseUid = firebaseUid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
        this.courseId = courseId;
        this.phone = phone;
        this.active = active;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirebaseUid(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartmentId(),
                user.getCourseId(),
                user.getPhone(),
                user.isActive()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
