package com.ams.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ams.dto.DepartmentReportResponse;
import com.ams.dto.OverallReportResponse;
import com.ams.dto.StudentReportResponse;
import com.ams.model.Attendance;
import com.ams.model.AttendanceStatus;
import com.ams.model.Course;
import com.ams.model.Department;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.repository.CourseRepository;
import com.ams.repository.DepartmentRepository;
import com.ams.repository.StudentRepository;
import com.ams.security.AuthenticatedUser;
import com.ams.security.SecurityUtils;

@Service
public class ReportService {

    private final MongoTemplate mongoTemplate;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;

    public ReportService(
            MongoTemplate mongoTemplate,
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository,
            CourseRepository courseRepository) {
        this.mongoTemplate = mongoTemplate;
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
    }

    private String resolveDepartmentId(String departmentId) {
        SecurityUtils.enforceReportsAccess();
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.HOD && currentUser.getDepartmentId() != null) {
            return currentUser.getDepartmentId();
        }
        return departmentId;
    }

    private List<Student> findFilteredStudents(
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section) {

        String scopedDeptId = resolveDepartmentId(departmentId);
        List<Criteria> criteriaList = new ArrayList<>();

        if (scopedDeptId != null && !scopedDeptId.isBlank()) {
            criteriaList.add(Criteria.where("departmentId").is(scopedDeptId));
        }
        if (courseId != null && !courseId.isBlank()) {
            criteriaList.add(Criteria.where("courseId").is(courseId));
        }
        if (programType != null) {
            criteriaList.add(Criteria.where("programType").is(programType));
        }
        if (year != null) {
            criteriaList.add(Criteria.where("year").is(year));
        }
        if (section != null && !section.isBlank()) {
            criteriaList.add(Criteria.where("section").is(section));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return mongoTemplate.find(query, Student.class);
    }

    private List<Attendance> findFilteredAttendance(
            List<String> studentIds,
            LocalDate startDate,
            LocalDate endDate) {

        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("studentId").in(studentIds));

        if (startDate != null && endDate != null) {
            criteriaList.add(Criteria.where("date").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            criteriaList.add(Criteria.where("date").gte(startDate));
        } else if (endDate != null) {
            criteriaList.add(Criteria.where("date").lte(endDate));
        }

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.find(query, Attendance.class);
    }

    public OverallReportResponse getOverallReport(
            LocalDate startDate,
            LocalDate endDate,
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section) {

        List<Student> students = findFilteredStudents(departmentId, courseId, programType, year, section);
        List<String> studentIds = students.stream().map(Student::getId).toList();
        List<Attendance> records = findFilteredAttendance(studentIds, startDate, endDate);

        long totalRecords = records.size();
        long presentRecords = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.ON_DUTY)
                .count();
        long absentRecords = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        double overallPercentage = totalRecords > 0
                ? Math.round((presentRecords * 100.0 / totalRecords) * 10.0) / 10.0
                : 0.0;

        return new OverallReportResponse(
                students.size(),
                totalRecords,
                presentRecords,
                absentRecords,
                overallPercentage
        );
    }

    public List<StudentReportResponse> getStudentReports(
            LocalDate startDate,
            LocalDate endDate,
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section) {

        List<Student> students = findFilteredStudents(departmentId, courseId, programType, year, section);
        List<String> studentIds = students.stream().map(Student::getId).toList();
        List<Attendance> records = findFilteredAttendance(studentIds, startDate, endDate);

        Map<String, List<Attendance>> attendanceByStudent = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        Map<String, String> departmentNames = new HashMap<>();
        Map<String, String> courseNames = new HashMap<>();

        return students.stream().map(s -> {
            List<Attendance> studentRecords = attendanceByStudent.getOrDefault(s.getId(), Collections.emptyList());
            long present = studentRecords.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.ON_DUTY)
                    .count();
            long absent = studentRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
            long total = studentRecords.size();
            double rate = total > 0 ? Math.round((present * 100.0 / total) * 10.0) / 10.0 : 0.0;

            String deptName = departmentNames.computeIfAbsent(s.getDepartmentId(),
                    id -> departmentRepository.findById(id).map(Department::getName).orElse(id));
            String crsName = courseNames.computeIfAbsent(s.getCourseId(),
                    id -> courseRepository.findById(id).map(Course::getName).orElse(id));

            return new StudentReportResponse(
                    s.getId(),
                    s.getRollNo(),
                    s.getName(),
                    s.getDepartmentId(),
                    deptName,
                    s.getCourseId(),
                    crsName,
                    s.getProgramType() != null ? s.getProgramType().name() : "UG",
                    s.getYear(),
                    s.getSection(),
                    present,
                    absent,
                    total,
                    rate
            );
        }).toList();
    }

    public List<DepartmentReportResponse> getDepartmentReports(
            LocalDate startDate,
            LocalDate endDate,
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section) {

        List<StudentReportResponse> studentReports = getStudentReports(
                startDate, endDate, departmentId, courseId, programType, year, section
        );

        Map<String, List<StudentReportResponse>> byDept = studentReports.stream()
                .collect(Collectors.groupingBy(StudentReportResponse::getDepartmentId));

        List<Department> activeDepts = departmentRepository.findByActiveTrue();
        String scopedDeptId = resolveDepartmentId(departmentId);
        if (scopedDeptId != null && !scopedDeptId.isBlank()) {
            activeDepts = activeDepts.stream().filter(d -> d.getId().equals(scopedDeptId)).toList();
        }

        Map<String, Department> deptMap = activeDepts != null
                ? activeDepts.stream().collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a))
                : Collections.emptyMap();

        Set<String> allDeptIds = new LinkedHashSet<>(deptMap.keySet());
        allDeptIds.addAll(byDept.keySet());

        return allDeptIds.stream().map(deptId -> {
            List<StudentReportResponse> list = byDept.getOrDefault(deptId, Collections.emptyList());
            Department deptObj = deptMap.get(deptId);
            String deptName = deptObj != null ? deptObj.getName() : (list.isEmpty() ? deptId : list.get(0).getDepartmentName());
            long totalStudents = list.size();
            long present = list.stream().mapToLong(StudentReportResponse::getPresent).sum();
            long absent = list.stream().mapToLong(StudentReportResponse::getAbsent).sum();
            long total = present + absent;
            double rate = total > 0 ? Math.round((present * 100.0 / total) * 10.0) / 10.0 : 0.0;

            return new DepartmentReportResponse(
                    deptId,
                    deptName,
                    totalStudents,
                    present,
                    absent,
                    total,
                    rate
            );
        }).sorted(Comparator.comparing(DepartmentReportResponse::getDepartment)).toList();
    }

    public List<StudentReportResponse> getLowAttendanceReports(
            LocalDate startDate,
            LocalDate endDate,
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section,
            Double threshold) {

        double cutoff = (threshold != null && threshold > 0) ? threshold : 75.0;
        return getStudentReports(startDate, endDate, departmentId, courseId, programType, year, section)
                .stream()
                .filter(s -> s.getTotalPeriods() > 0 && s.getAttendanceRate() < cutoff)
                .toList();
    }
}
