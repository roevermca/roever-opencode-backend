package com.ams.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ams.dto.BulkStudentImportResponse;
import com.ams.dto.PageResponse;
import com.ams.dto.StudentRequest;
import com.ams.dto.StudentResponse;
import com.ams.exception.AccessDeniedException;
import com.ams.exception.DuplicateResourceException;
import com.ams.exception.ResourceNotFoundException;
import com.ams.model.Attendance;
import com.ams.model.AttendanceArchive;
import com.ams.model.Course;
import com.ams.model.Department;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.model.User;
import com.ams.repository.AttendanceArchiveRepository;
import com.ams.repository.AttendanceRepository;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;
import com.ams.security.AuthenticatedUser;
import com.ams.security.SecurityUtils;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceArchiveRepository archiveRepository;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          MongoTemplate mongoTemplate,
                          AttendanceRepository attendanceRepository,
                          AttendanceArchiveRepository archiveRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.attendanceRepository = attendanceRepository;
        this.archiveRepository = archiveRepository;
    }


    public PageResponse<StudentResponse> getStudents(
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section,
            String search,
            int page,
            int size) {

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        List<Criteria> filters = new ArrayList<>();

        if (currentUser != null) {
            if (currentUser.getRole() == Role.HOD && currentUser.getDepartmentId() != null) {
                // Rule 6: Never trust departmentId sent by frontend. Restrict HOD to own department.
                departmentId = currentUser.getDepartmentId();
            } else if (currentUser.getRole() == Role.STAFF) {
                // STAFF ACCESS CONTROL: If staff is assigned to a course (e.g. MCA), force restrict to that course!
                if (currentUser.getCourseId() != null && !currentUser.getCourseId().isBlank()) {
                    courseId = currentUser.getCourseId();
                    departmentId = null; // courseId uniquely isolates the students
                } else if (currentUser.getDepartmentId() != null && !currentUser.getDepartmentId().isBlank()) {
                    departmentId = currentUser.getDepartmentId();
                }
            } else if (currentUser.getRole() == Role.STUDENT) {
                // Rule 7: Student can only access their own data.
                if (currentUser.getStudentId() != null) {
                    filters.add(Criteria.where("id").is(currentUser.getStudentId()));
                } else if (currentUser.getEmail() != null) {
                    filters.add(Criteria.where("email").is(currentUser.getEmail().toLowerCase().trim()));
                }
            }
        }

        if (departmentId != null && !departmentId.isBlank()) {
            List<String> deptMatches = getMatchingDeptIdentifiers(departmentId);
            filters.add(Criteria.where("departmentId").in(deptMatches));
        }
        if (courseId != null && !courseId.isBlank()) {
            List<String> courseMatches = getMatchingCourseIdentifiers(courseId);
            filters.add(Criteria.where("courseId").in(courseMatches));
        }
        if (programType != null) {
            filters.add(Criteria.where("programType").is(programType));
        }
        if (year != null) {
            filters.add(Criteria.where("year").is(year));
        }
        if (section != null && !section.isBlank()) {
            filters.add(Criteria.where("section").is(section.trim()));
        }
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("rollNo").regex("^" + Pattern.quote(term), "i"),
                    Criteria.where("name").regex(Pattern.quote(term), "i")
            );
            filters.add(searchCriteria);
        }

        Criteria criteria = new Criteria();
        if (!filters.isEmpty()) {
            criteria.andOperator(filters.toArray(new Criteria[0]));
        }

        Query countQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(countQuery, Student.class);

        int validatedPage = Math.max(0, page);
        int validatedSize = (size <= 0) ? 20 : Math.min(100, size);

        Query query = new Query(criteria)
                .with(PageRequest.of(validatedPage, validatedSize, Sort.by("rollNo").ascending()));

        List<Student> students = mongoTemplate.find(query, Student.class);
        int totalPages = validatedSize > 0 ? (int) Math.ceil((double) totalElements / validatedSize) : 0;

        List<StudentResponse> data = students.stream()
                .map(StudentResponse::fromEntity)
                .toList();

        return new PageResponse<>(data, validatedPage, validatedSize, totalElements, totalPages);
    }

    private final Map<String, List<String>> courseIdentifierCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, List<String>> deptIdentifierCache = new java.util.concurrent.ConcurrentHashMap<>();

    public List<String> getMatchingCourseIdentifiers(String courseIdentifier) {
        if (courseIdentifier == null || courseIdentifier.isBlank()) return List.of();
        String trimmed = courseIdentifier.trim();
        if (courseIdentifierCache.containsKey(trimmed)) {
            return courseIdentifierCache.get(trimmed);
        }

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
        List<String> distinctIds = ids.stream().distinct().toList();
        courseIdentifierCache.put(trimmed, distinctIds);
        return distinctIds;
    }

    public List<String> getMatchingDeptIdentifiers(String deptIdentifier) {
        if (deptIdentifier == null || deptIdentifier.isBlank()) return List.of();
        String trimmed = deptIdentifier.trim();
        if (deptIdentifierCache.containsKey(trimmed)) {
            return deptIdentifierCache.get(trimmed);
        }

        List<String> ids = new ArrayList<>();
        ids.add(trimmed);
        try {
            Query q = new Query(new Criteria().orOperator(
                    Criteria.where("id").is(trimmed),
                    Criteria.where("code").is(trimmed),
                    Criteria.where("name").regex("^" + Pattern.quote(trimmed) + "$", "i")
            ));
            List<Department> found = mongoTemplate.find(q, Department.class);
            if (found != null) {
                for (Department d : found) {
                    if (d.getId() != null) ids.add(d.getId());
                    if (d.getCode() != null) ids.add(d.getCode());
                    if (d.getName() != null) ids.add(d.getName());
                }
            }
        } catch (Exception ignored) {
        }
        List<String> distinctIds = ids.stream().distinct().toList();
        deptIdentifierCache.put(trimmed, distinctIds);
        return distinctIds;
    }

    public StudentResponse getStudentById(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.STAFF) {
            if (currentUser.getCourseId() != null && !currentUser.getCourseId().isBlank()) {
                List<String> allowedCourses = getMatchingCourseIdentifiers(currentUser.getCourseId());
                if (!allowedCourses.contains(student.getCourseId())) {
                    throw new AccessDeniedException("Forbidden: Staff can only access students in their assigned course (" + currentUser.getCourseId() + ")");
                }
            }
            if (currentUser.getDepartmentId() != null && !currentUser.getDepartmentId().isBlank()) {
                List<String> allowedDepts = getMatchingDeptIdentifiers(currentUser.getDepartmentId());
                if (!allowedDepts.contains(student.getDepartmentId())) {
                    throw new AccessDeniedException("Forbidden: Staff cannot access students from other departments");
                }
            }
        }

        SecurityUtils.enforceStudentReadAccess(student.getId(), student.getDepartmentId());
        return StudentResponse.fromEntity(student);
    }

    public StudentResponse createStudent(StudentRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.HOD) {
            if (currentUser.getDepartmentId() != null && !currentUser.getDepartmentId().equals(request.getDepartmentId())) {
                throw new AccessDeniedException("Forbidden: HOD can only manage students in their own department");
            }
        }

        SecurityUtils.enforceStudentWriteAccess(request.getDepartmentId());

        String rollNo = request.getRollNo().trim();
        if (studentRepository.existsByRollNo(rollNo)) {
            throw new DuplicateResourceException("Student with roll number " + rollNo + " already exists");
        }

        String courseId = (request.getCourseId() != null && !request.getCourseId().isBlank())
                ? request.getCourseId().trim()
                : request.getDepartmentId().trim();
        ProgramType programType = request.getProgramType() != null ? request.getProgramType() : ProgramType.UG;

        Student student = new Student(
                rollNo,
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getPhone() != null ? request.getPhone().trim() : null,
                request.getDepartmentId().trim(),
                courseId,
                programType,
                request.getYear(),
                request.getSection().trim().toUpperCase(),
                request.getActive() != null ? request.getActive() : true
        );

        Student savedStudent = studentRepository.save(student);

        // Sync with UserRepository so student can login via Google
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            String studentEmail = student.getEmail().trim().toLowerCase();
            userRepository.findByEmail(studentEmail).ifPresentOrElse(
                    existing -> {
                        existing.setName(student.getName());
                        existing.setRole(Role.STUDENT);
                        existing.setDepartmentId(student.getDepartmentId());
                        existing.setActive(student.isActive());
                        userRepository.save(existing);
                    },
                    () -> {
                        String uid = "stu_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                        User user = new User(
                                uid,
                                student.getName(),
                                studentEmail,
                                Role.STUDENT,
                                student.getDepartmentId(),
                                student.isActive()
                        );
                        userRepository.save(user);
                    }
            );
        }

        return StudentResponse.fromEntity(savedStudent);
    }

    public BulkStudentImportResponse createStudentsBulk(List<StudentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new BulkStudentImportResponse(0, 0, 0, List.of(), List.of());
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<StudentResponse> saved = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            StudentRequest req = requests.get(i);
            try {
                if (req.getRollNo() == null || req.getRollNo().isBlank()) {
                    throw new IllegalArgumentException("Roll number is required");
                }
                if (req.getName() == null || req.getName().isBlank()) {
                    throw new IllegalArgumentException("Student name is required");
                }
                if (req.getEmail() == null || req.getEmail().isBlank()) {
                    throw new IllegalArgumentException("Email is required");
                }
                if (req.getDepartmentId() == null || req.getDepartmentId().isBlank()) {
                    throw new IllegalArgumentException("Department is required");
                }
                if (req.getYear() == null) {
                    req.setYear(1);
                }
                if (req.getSection() == null || req.getSection().isBlank()) {
                    req.setSection("A");
                }
                if (req.getProgramType() == null) {
                    req.setProgramType(ProgramType.UG);
                }
                StudentResponse res = createStudent(req);
                saved.add(res);
                imported++;
            } catch (Exception ex) {
                skipped++;
                String identifier = (req != null && req.getRollNo() != null && !req.getRollNo().isBlank())
                        ? req.getRollNo()
                        : "Row " + (i + 1);
                errors.add(identifier + ": " + ex.getMessage());
            }
        }

        return new BulkStudentImportResponse(requests.size(), imported, skipped, errors, saved);
    }

    public StudentResponse updateStudent(String id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        SecurityUtils.enforceStudentWriteAccess(student.getDepartmentId());
        SecurityUtils.enforceStudentWriteAccess(request.getDepartmentId());

        String newRollNo = request.getRollNo().trim();
        if (!student.getRollNo().equalsIgnoreCase(newRollNo) && studentRepository.existsByRollNo(newRollNo)) {
            throw new DuplicateResourceException("Student with roll number " + newRollNo + " already exists");
        }

        student.setRollNo(newRollNo);
        student.setName(request.getName().trim());
        student.setEmail(request.getEmail().trim().toLowerCase());
        student.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        student.setDepartmentId(request.getDepartmentId().trim());
        if (request.getCourseId() != null && !request.getCourseId().isBlank()) {
            student.setCourseId(request.getCourseId().trim());
        } else if (student.getCourseId() == null || student.getCourseId().isBlank()) {
            student.setCourseId(request.getDepartmentId().trim());
        }
        if (request.getProgramType() != null) {
            student.setProgramType(request.getProgramType());
        }
        student.setYear(request.getYear());
        student.setSection(request.getSection().trim().toUpperCase());
        if (request.getActive() != null) {
            student.setActive(request.getActive());
        }

        Student updatedStudent = studentRepository.save(student);

        // Sync with UserRepository
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            String studentEmail = student.getEmail().trim().toLowerCase();
            userRepository.findByEmail(studentEmail).ifPresentOrElse(
                    u -> {
                        u.setName(student.getName());
                        u.setActive(student.isActive());
                        u.setDepartmentId(student.getDepartmentId());
                        userRepository.save(u);
                    },
                    () -> {
                        String uid = "stu_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                        User user = new User(
                                uid,
                                student.getName(),
                                studentEmail,
                                Role.STUDENT,
                                student.getDepartmentId(),
                                student.isActive()
                        );
                        userRepository.save(user);
                    }
            );
        }

        return StudentResponse.fromEntity(updatedStudent);
    }

    public void deleteStudent(String id) {
        Student student = studentRepository.findById(id)
                .or(() -> studentRepository.findByRollNo(id))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        SecurityUtils.enforceStudentWriteAccess(student.getDepartmentId());

        // 1. Delete associated User account
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            userRepository.findByEmail(student.getEmail().trim().toLowerCase())
                    .ifPresent(userRepository::delete);
        }

        // 2. Cascade delete all attendance records for this student across all identifier variations
        List<Criteria> attCriteria = new ArrayList<>();
        if (student.getId() != null) {
            attCriteria.add(Criteria.where("studentId").is(student.getId()));
        }
        if (id != null && !id.isBlank()) {
            attCriteria.add(Criteria.where("studentId").is(id.trim()));
        }
        if (student.getRollNo() != null && !student.getRollNo().isBlank()) {
            String roll = student.getRollNo().trim();
            attCriteria.add(Criteria.where("studentId").is(roll));
            attCriteria.add(Criteria.where("studentId").regex("^" + Pattern.quote(roll) + "$", "i"));
        }
        Query attQuery = new Query(new Criteria().orOperator(attCriteria.toArray(new Criteria[0])));

        if (mongoTemplate != null) {
            mongoTemplate.remove(attQuery, Attendance.class);
            mongoTemplate.remove(attQuery, AttendanceArchive.class);
        }

        if (attendanceRepository != null) {
            if (student.getId() != null) {
                attendanceRepository.deleteByStudentId(student.getId());
            }
            if (student.getRollNo() != null && !student.getRollNo().isBlank()) {
                attendanceRepository.deleteByStudentId(student.getRollNo().trim());
            }
        }

        // 3. Cascade delete all archived attendance records for this student
        if (archiveRepository != null) {
            if (student.getId() != null) {
                archiveRepository.deleteByStudentId(student.getId());
            }
            if (student.getRollNo() != null && !student.getRollNo().isBlank()) {
                archiveRepository.deleteByStudentId(student.getRollNo().trim());
            }
        }

        // 4. Delete the student entity
        studentRepository.delete(student);
    }
}

