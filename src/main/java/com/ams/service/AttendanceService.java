package com.ams.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ams.dto.AttendanceResponse;
import com.ams.dto.AttendanceSummaryResponse;
import com.ams.dto.BulkAttendanceRequest;
import com.ams.dto.BulkAttendanceResponse;
import com.ams.dto.PageResponse;
import com.ams.dto.StudentAttendanceRecord;
import com.ams.dto.UpdateAttendanceRequest;
import com.ams.exception.AccessDeniedException;
import com.ams.exception.DuplicateResourceException;
import com.ams.exception.ResourceNotFoundException;
import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;
import com.ams.model.Course;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.repository.AttendanceRepository;
import com.ams.repository.StudentRepository;
import com.ams.security.AuthenticatedUser;
import com.ams.security.SecurityUtils;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final MongoTemplate mongoTemplate;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            StudentRepository studentRepository,
            MongoTemplate mongoTemplate) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public BulkAttendanceResponse markAttendanceBulk(BulkAttendanceRequest request) {
        SecurityUtils.enforceAttendanceMarkAccess();

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        String markedBy = currentUser != null ? currentUser.getEmail() : "SYSTEM";

        List<StudentAttendanceRecord> records = request.getRecords();
        Set<String> studentIds = records.stream()
                .map(StudentAttendanceRecord::getStudentId)
                .collect(Collectors.toSet());

        // 1. Verify all students exist
        List<Student> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            Set<String> foundIds = students.stream().map(Student::getId).collect(Collectors.toSet());
            Set<String> missingIds = new HashSet<>(studentIds);
            missingIds.removeAll(foundIds);
            throw new ResourceNotFoundException("Student not found for IDs: " + missingIds);
        }

        // 2. HOD restriction: all students must belong to HOD's department
        if (currentUser != null && currentUser.getRole() == Role.HOD) {
            String hodDept = currentUser.getDepartmentId();
            for (Student student : students) {
                if (!student.getDepartmentId().equals(hodDept)) {
                    throw new AccessDeniedException(
                            "Forbidden: HOD can only mark attendance for students in their own department (" + hodDept + ")");
                }
            }
        }

        // Staff restriction: all students must belong to Staff's assigned course!
        if (currentUser != null && currentUser.getRole() == Role.STAFF) {
            String staffCourse = currentUser.getCourseId();
            if (staffCourse != null && !staffCourse.isBlank()) {
                List<String> allowedCourses = getMatchingCourseIdentifiers(staffCourse);
                for (Student student : students) {
                    if (!allowedCourses.contains(student.getCourseId())) {
                        throw new AccessDeniedException(
                                "Forbidden: Staff can only mark attendance for students in their assigned course (" + staffCourse + ")");
                    }
                }
            }
        }

        // 3. Prevent duplicate attendance and enforce locked state for STAFF
        List<Attendance> existingRecords = attendanceRepository.findByStudentIdInAndDateAndPeriod(
                studentIds, request.getDate(), request.getPeriod()
        );

        if (!existingRecords.isEmpty()) {
            if (currentUser != null && currentUser.getRole() == Role.STAFF) {
                throw new DuplicateResourceException(
                        "Attendance has already been submitted and locked for period " + request.getPeriod() +
                        " on " + request.getDate() + ". Staff cannot edit submitted attendance.");
            }
            throw new DuplicateResourceException(
                    "Attendance already exists for " + existingRecords.size() +
                    " student(s) on " + request.getDate() + " period " + request.getPeriod());
        }

        // 4. Server-generated timestamp and batch creation
        Instant markedAt = Instant.now();
        List<Attendance> toInsert = new ArrayList<>(records.size());
        int presentCount = 0;
        int absentCount = 0;

        for (StudentAttendanceRecord record : records) {
            if (record.getStatus() == AttendanceStatus.PRESENT || record.getStatus() == AttendanceStatus.ON_DUTY) {
                presentCount++;
            } else {
                absentCount++;
            }
            toInsert.add(new Attendance(
                    record.getStudentId(),
                    request.getDate(),
                    request.getPeriod(),
                    record.getStatus(),
                    markedBy,
                    markedAt
            ));
        }

        // Single bulk insert operation
        mongoTemplate.insert(toInsert, Attendance.class);

        return new BulkAttendanceResponse(
                true,
                "Attendance successfully recorded for period " + request.getPeriod() + " on " + request.getDate(),
                request.getDate(),
                request.getPeriod(),
                toInsert.size(),
                presentCount,
                absentCount
        );
    }

    public PageResponse<AttendanceResponse> getAttendanceHistory(
            String studentId,
            LocalDate date,
            LocalDate startDate,
            LocalDate endDate,
            Integer period,
            AttendanceStatus status,
            int page,
            int size) {

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        List<Criteria> filters = new ArrayList<>();

        // Role-based scoping
        if (currentUser != null) {
            if (currentUser.getRole() == Role.STUDENT) {
                // Students can only see their own attendance
                studentId = currentUser.getStudentId();
                if (studentId == null) {
                    return new PageResponse<>(List.of(), page, size, 0L, 0);
                }
            } else if (currentUser.getRole() == Role.HOD) {
                // HOD can only see attendance of students in their own department
                if (studentId != null) {
                    Student student = studentRepository.findById(studentId).orElse(null);
                    if (student == null || !student.getDepartmentId().equals(currentUser.getDepartmentId())) {
                        throw new AccessDeniedException("Forbidden: HOD can only view attendance for own department");
                    }
                } else {
                    List<String> deptStudentIds = studentRepository.findByDepartmentIdAndActiveTrue(currentUser.getDepartmentId())
                            .stream().map(Student::getId).toList();
                    filters.add(Criteria.where("studentId").in(deptStudentIds));
                }
            } else if (currentUser.getRole() == Role.STAFF) {
                if (currentUser.getCourseId() != null && !currentUser.getCourseId().isBlank()) {
                    List<String> allowedCourses = getMatchingCourseIdentifiers(currentUser.getCourseId());
                    if (studentId != null && !studentId.isBlank()) {
                        Student student = studentRepository.findById(studentId.trim()).orElse(null);
                        if (student == null || student.getCourseId() == null || !allowedCourses.contains(student.getCourseId())) {
                            throw new AccessDeniedException("Forbidden: Staff can only view attendance for students in their assigned course (" + currentUser.getCourseId() + ")");
                        }
                    } else {
                        List<String> courseStudentIds = studentRepository.findAll().stream()
                                .filter(s -> s.isActive() && s.getCourseId() != null && allowedCourses.contains(s.getCourseId()))
                                .map(Student::getId).toList();
                        filters.add(Criteria.where("studentId").in(courseStudentIds));
                    }
                }
            }
        }

        if (studentId != null && !studentId.isBlank()) {
            filters.add(Criteria.where("studentId").is(studentId.trim()));
        }
        if (date != null) {
            filters.add(Criteria.where("date").is(date));
        } else if (startDate != null && endDate != null) {
            filters.add(Criteria.where("date").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            filters.add(Criteria.where("date").gte(startDate));
        } else if (endDate != null) {
            filters.add(Criteria.where("date").lte(endDate));
        }
        if (period != null) {
            filters.add(Criteria.where("period").is(period));
        }
        if (status != null) {
            filters.add(Criteria.where("status").is(status));
        }

        Criteria criteria = new Criteria();
        if (!filters.isEmpty()) {
            criteria.andOperator(filters.toArray(new Criteria[0]));
        }

        Query countQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(countQuery, Attendance.class);

        int validatedPage = Math.max(0, page);
        int validatedSize = (size <= 0) ? 20 : Math.min(100, size);

        Query query = new Query(criteria)
                .with(PageRequest.of(validatedPage, validatedSize, Sort.by("date").descending().and(Sort.by("period").descending())));

        List<Attendance> attendanceList = mongoTemplate.find(query, Attendance.class);
        int totalPages = validatedSize > 0 ? (int) Math.ceil((double) totalElements / validatedSize) : 0;

        List<AttendanceResponse> data = attendanceList.stream()
                .map(AttendanceResponse::fromEntity)
                .toList();

        return new PageResponse<>(data, validatedPage, validatedSize, totalElements, totalPages);
    }

    public PageResponse<AttendanceResponse> getStudentAttendance(String studentId, int page, int size) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.STAFF) {
            if (currentUser.getCourseId() != null && !currentUser.getCourseId().isBlank()) {
                List<String> allowedCourses = getMatchingCourseIdentifiers(currentUser.getCourseId());
                if (student.getCourseId() == null || !allowedCourses.contains(student.getCourseId())) {
                    throw new AccessDeniedException("Forbidden: Staff can only access attendance of students in their assigned course (" + currentUser.getCourseId() + ")");
                }
            }
        }

        SecurityUtils.enforceStudentAttendanceReadAccess(student.getId(), student.getDepartmentId());

        int validatedPage = Math.max(0, page);
        int validatedSize = (size <= 0) ? 20 : Math.min(100, size);

        Query countQuery = new Query(Criteria.where("studentId").is(studentId));
        long totalElements = mongoTemplate.count(countQuery, Attendance.class);

        Query query = new Query(Criteria.where("studentId").is(studentId))
                .with(PageRequest.of(validatedPage, validatedSize, Sort.by("date").descending().and(Sort.by("period").descending())));

        List<Attendance> list = mongoTemplate.find(query, Attendance.class);
        int totalPages = validatedSize > 0 ? (int) Math.ceil((double) totalElements / validatedSize) : 0;

        List<AttendanceResponse> data = list.stream()
                .map(AttendanceResponse::fromEntity)
                .toList();

        return new PageResponse<>(data, validatedPage, validatedSize, totalElements, totalPages);
    }

    public AttendanceSummaryResponse getStudentAttendanceSummary(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        SecurityUtils.enforceStudentAttendanceReadAccess(student.getId(), student.getDepartmentId());

        long totalClasses = attendanceRepository.countByStudentId(studentId);
        long present = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT)
                     + attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ON_DUTY);
        long absent = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT);

        double percentage = totalClasses > 0
                ? Math.round(((double) present / totalClasses) * 1000.0) / 10.0
                : 0.0;

        return new AttendanceSummaryResponse(totalClasses, present, absent, percentage);
    }

    public AttendanceResponse updateAttendanceRecord(String id, UpdateAttendanceRequest request) {
        SecurityUtils.enforceAttendanceEditAccess();

        Attendance record = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        String editor = currentUser != null ? currentUser.getEmail() : "ADMIN";

        record.setStatus(request.getStatus());
        record.setMarkedBy(editor + " (Corrected)");
        record.setMarkedAt(Instant.now());

        Attendance saved = attendanceRepository.save(record);
        return AttendanceResponse.fromEntity(saved);
    }

    public List<String> getMatchingCourseIdentifiers(String courseIdentifier) {
        if (courseIdentifier == null || courseIdentifier.isBlank()) return List.of();
        String trimmed = courseIdentifier.trim();
        List<String> ids = new ArrayList<>();
        ids.add(trimmed);
        try {
            Query q = new Query(new Criteria().orOperator(
                    Criteria.where("id").is(trimmed),
                    Criteria.where("code").is(trimmed),
                    Criteria.where("name").regex("^" + Pattern.quote(trimmed) + "$", "i")
            ));
            List<Course> found = mongoTemplate.find(q, Course.class);
            if (found != null) {
                for (Course c : found) {
                    if (c.getId() != null) ids.add(c.getId());
                    if (c.getCode() != null) ids.add(c.getCode());
                    if (c.getName() != null) ids.add(c.getName());
                }
            }
        } catch (Exception ignored) {
        }
        return ids.stream().distinct().toList();
    }
}
