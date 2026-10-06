package com.ams.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ams.dto.PageResponse;
import com.ams.dto.UserRequest;
import com.ams.dto.UserResponse;
import com.ams.model.Role;
import com.ams.security.AuthenticatedUser;
import com.ams.security.SecurityUtils;
import com.ams.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        AuthenticatedUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "User is not authenticated"));
        }

        String phone = "";
        String name = user.getName() != null ? user.getName() : user.getEmail();
        String deptId = user.getDepartmentId() != null ? user.getDepartmentId() : "";
        String courseId = user.getCourseId() != null ? user.getCourseId() : "";

        if (user.getUserId() != null) {
            var dbUserOpt = userService.findUserEntityById(user.getUserId());
            if (dbUserOpt.isPresent()) {
                var dbUser = dbUserOpt.get();
                if (dbUser.getPhone() != null) phone = dbUser.getPhone();
                if (dbUser.getName() != null) name = dbUser.getName();
                if (dbUser.getDepartmentId() != null) deptId = dbUser.getDepartmentId();
                if (dbUser.getCourseId() != null) courseId = dbUser.getCourseId();
            }
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getUserId() != null ? user.getUserId() : user.getEmail());
        response.put("firebaseUid", user.getFirebaseUid());
        response.put("name", name);
        response.put("email", user.getEmail());
        response.put("phone", phone);
        response.put("displayName", name);
        response.put("role", user.getRole() != null ? user.getRole().name() : "STAFF");
        response.put("departmentId", deptId);
        response.put("courseId", courseId);
        response.put("studentId", user.getStudentId() != null ? user.getStudentId() : "");
        response.put("active", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getUsers(role, departmentId, active, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
