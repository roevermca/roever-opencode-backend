package com.ams.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String firebaseUid;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    @NotNull
    @Indexed
    private Role role;

    @Indexed
    private String departmentId;

    @Indexed
    private String courseId;

    private String phone;

    private boolean active = true;

    public User() {
    }

    public User(String firebaseUid, String name, String email, Role role, String departmentId, boolean active) {
        this(firebaseUid, name, email, role, departmentId, null, null, active);
    }

    public User(String firebaseUid, String name, String email, Role role, String departmentId, String courseId, boolean active) {
        this(firebaseUid, name, email, role, departmentId, courseId, null, active);
    }

    public User(String firebaseUid, String name, String email, Role role, String departmentId, String courseId, String phone, boolean active) {
        this.firebaseUid = firebaseUid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
        this.courseId = courseId;
        this.phone = phone;
        this.active = active;
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
