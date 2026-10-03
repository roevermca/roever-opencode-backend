package com.ams.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    Optional<Attendance> findByStudentIdAndDateAndPeriod(String studentId, LocalDate date, int period);

    boolean existsByStudentIdAndDateAndPeriod(String studentId, LocalDate date, int period);

    List<Attendance> findByDateAndPeriod(LocalDate date, int period);

    List<Attendance> findByStudentIdAndDateBetween(String studentId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByStudentId(String studentId);

    Page<Attendance> findByStudentId(String studentId, Pageable pageable);

    long countByStudentId(String studentId);

    long countByStudentIdAndStatus(String studentId, AttendanceStatus status);

    List<Attendance> findByStudentIdInAndDateAndPeriod(Collection<String> studentIds, LocalDate date, int period);
}
