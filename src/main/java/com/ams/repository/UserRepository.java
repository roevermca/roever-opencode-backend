package com.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ams.model.Role;
import com.ams.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);

    List<User> findByDepartmentIdAndActiveTrue(String departmentId);

    List<User> findByRoleAndActiveTrue(Role role);
}
