package com.ams.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ams.dto.PageResponse;
import com.ams.dto.UserRequest;
import com.ams.dto.UserResponse;
import com.ams.exception.AccessDeniedException;
import com.ams.exception.DuplicateResourceException;
import com.ams.exception.ResourceNotFoundException;
import com.ams.model.Role;
import com.ams.model.User;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;
import com.ams.security.AuthenticatedUser;
import com.ams.security.SecurityUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final MongoTemplate mongoTemplate;

    public UserService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public PageResponse<UserResponse> getUsers(
            Role role,
            String departmentId,
            Boolean active,
            int page,
            int size) {

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            Role userRole = currentUser.getRole();
            if (userRole == Role.STAFF || userRole == Role.STUDENT) {
                throw new AccessDeniedException("Forbidden: " + userRole + " is not permitted to view user directory");
            }
            if (userRole == Role.HOD && currentUser.getDepartmentId() != null) {
                departmentId = currentUser.getDepartmentId();
            }
        }

        List<Criteria> filters = new ArrayList<>();

        if (role != null) {
            filters.add(Criteria.where("role").is(role));
        }
        if (departmentId != null && !departmentId.isBlank()) {
            filters.add(Criteria.where("departmentId").is(departmentId.trim()));
        }
        if (active != null) {
            filters.add(Criteria.where("active").is(active));
        }

        Criteria criteria = new Criteria();
        if (!filters.isEmpty()) {
            criteria.andOperator(filters.toArray(new Criteria[0]));
        }

        Query countQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(countQuery, User.class);

        int validatedPage = Math.max(0, page);
        int validatedSize = (size <= 0) ? 20 : Math.min(100, size);

        Query query = new Query(criteria)
                .with(PageRequest.of(validatedPage, validatedSize, Sort.by("name").ascending()));

        List<User> users = mongoTemplate.find(query, User.class);
        int totalPages = validatedSize > 0 ? (int) Math.ceil((double) totalElements / validatedSize) : 0;

        List<UserResponse> data = users.stream()
                .map(UserResponse::fromEntity)
                .toList();

        return new PageResponse<>(data, validatedPage, validatedSize, totalElements, totalPages);
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            Role userRole = currentUser.getRole();
            if (userRole == Role.STAFF || userRole == Role.STUDENT) {
                if (!currentUser.getUserId().equals(user.getId())) {
                    throw new AccessDeniedException("Forbidden: Users can only view their own profile");
                }
            } else if (userRole == Role.HOD) {
                if (currentUser.getDepartmentId() != null && !currentUser.getDepartmentId().equals(user.getDepartmentId())) {
                    throw new AccessDeniedException("Forbidden: HOD can only view users in their own department");
                }
            }
        }

        return UserResponse.fromEntity(user);
    }

    public UserResponse createUser(UserRequest request) {
        SecurityUtils.enforceStaffManagementAccess();

        String email = request.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (!existing.isActive()) {
                if (request.getRole() == Role.VP) {
                    boolean vpExists = userRepository.findAll().stream()
                            .anyMatch(u -> u.getRole() == Role.VP && u.isActive() && !u.getId().equals(existing.getId()));
                    if (vpExists) {
                        throw new DuplicateResourceException("Only 1 active Vice Principal (VP) is permitted in the institution.");
                    }
                }
                existing.setName(request.getName().trim());
                existing.setRole(request.getRole());
                existing.setDepartmentId(request.getDepartmentId() != null ? request.getDepartmentId().trim() : null);
                existing.setCourseId(request.getCourseId() != null ? request.getCourseId().trim() : null);
                existing.setActive(true);
                User reactivated = userRepository.save(existing);
                return UserResponse.fromEntity(reactivated);
            }
            throw new DuplicateResourceException("User with email " + email + " already exists");
        }

        String firebaseUid = request.getFirebaseUid();
        if (firebaseUid == null || firebaseUid.isBlank()) {
            firebaseUid = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        } else {
            firebaseUid = firebaseUid.trim();
            if (userRepository.existsByFirebaseUid(firebaseUid)) {
                throw new DuplicateResourceException("User with Firebase UID " + firebaseUid + " already exists");
            }
        }

        if (request.getRole() == Role.VP) {
            boolean vpExists = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRole() == Role.VP && u.isActive());
            if (vpExists) {
                throw new DuplicateResourceException("Only 1 active Vice Principal (VP) is permitted in the institution.");
            }
        }

        User user = new User(
                firebaseUid,
                request.getName().trim(),
                email,
                request.getRole(),
                request.getDepartmentId() != null ? request.getDepartmentId().trim() : null,
                request.getCourseId() != null ? request.getCourseId().trim() : null,
                request.getActive() != null ? request.getActive() : true
        );

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    public UserResponse updateUser(String id, UserRequest request) {
        SecurityUtils.enforceStaffManagementAccess();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (request.getRole() == Role.VP) {
            boolean isActivating = request.getActive() == null || Boolean.TRUE.equals(request.getActive());
            if (isActivating) {
                boolean otherVpExists = userRepository.findAll().stream()
                        .anyMatch(u -> u.getRole() == Role.VP && u.isActive() && !u.getId().equals(id));
                if (otherVpExists) {
                    throw new DuplicateResourceException("Only 1 active Vice Principal (VP) is permitted in the institution.");
                }
            }
        }

        String newEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("User with email " + newEmail + " already exists");
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);
        user.setRole(request.getRole());
        user.setDepartmentId(request.getDepartmentId() != null ? request.getDepartmentId().trim() : null);
        user.setCourseId(request.getCourseId() != null ? request.getCourseId().trim() : null);
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    public void deleteUser(String id) {
        SecurityUtils.enforceStaffManagementAccess();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String normEmail = user.getEmail().trim().toLowerCase();
            studentRepository.findByEmail(normEmail).ifPresent(studentRepository::delete);
        }

        userRepository.delete(user);
    }
}
