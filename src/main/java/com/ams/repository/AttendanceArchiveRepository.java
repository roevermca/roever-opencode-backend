package com.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ams.model.AttendanceArchive;

@Repository
public interface AttendanceArchiveRepository extends MongoRepository<AttendanceArchive, String> {

    List<AttendanceArchive> findByStudentId(String studentId);

    Optional<AttendanceArchive> findByStudentIdAndDateRange(String studentId, String dateRange);

    long deleteByStudentId(String studentId);
}

