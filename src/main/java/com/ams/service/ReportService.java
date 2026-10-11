package com.ams.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ams.dto.AttendanceLedgerResponse;
import com.ams.dto.DepartmentReportResponse;
import com.ams.dto.OverallReportResponse;
import com.ams.dto.StudentLedgerRowResponse;
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

    public AttendanceLedgerResponse getAttendanceLedger(
            LocalDate startDate,
            LocalDate endDate,
            String departmentId,
            String courseId,
            ProgramType programType,
            Integer year,
            String section) {

        String scopedDeptId = resolveDepartmentId(departmentId);
        List<Student> students = new ArrayList<>(findFilteredStudents(scopedDeptId, courseId, programType, year, section));
        students.sort((a, b) -> {
            String r1 = a.getRollNo() != null ? a.getRollNo() : "";
            String r2 = b.getRollNo() != null ? b.getRollNo() : "";
            return r1.compareToIgnoreCase(r2);
        });

        List<String> studentIds = students.stream().map(Student::getId).toList();
        List<Attendance> records = findFilteredAttendance(studentIds, startDate, endDate);

        Map<String, Map<LocalDate, List<Attendance>>> recordsByStudentAndDate = new HashMap<>();
        Set<LocalDate> datesWithAttendance = new TreeSet<>();

        for (Attendance att : records) {
            if (att.getDate() != null && att.getStudentId() != null) {
                recordsByStudentAndDate
                        .computeIfAbsent(att.getStudentId(), k -> new HashMap<>())
                        .computeIfAbsent(att.getDate(), k -> new ArrayList<>())
                        .add(att);
                datesWithAttendance.add(att.getDate());
            }
        }

        Set<LocalDate> dateSet = new TreeSet<>(datesWithAttendance);
        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                if (cur.getDayOfWeek() != DayOfWeek.SATURDAY && cur.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    dateSet.add(cur);
                }
                cur = cur.plusDays(1);
            }
        }
        List<LocalDate> sortedDates = new ArrayList<>(dateSet);

        int totalWorkingDays = datesWithAttendance.size();
        List<StudentLedgerRowResponse> studentRows = new ArrayList<>();

        for (Student s : students) {
            Map<String, String> attendanceByDate = new HashMap<>();
            double presentDays = 0.0;
            double absentDays = 0.0;
            int studentWorkingDays = 0;

            Map<LocalDate, List<Attendance>> studentMap = recordsByStudentAndDate.getOrDefault(s.getId(), Collections.emptyMap());

            for (LocalDate d : sortedDates) {
                String dStr = d.toString();
                List<Attendance> dateRecs = studentMap.get(d);

                if (dateRecs == null || dateRecs.isEmpty()) {
                    if (datesWithAttendance.contains(d)) {
                        attendanceByDate.put(dStr, "A");
                        absentDays += 1.0;
                        studentWorkingDays++;
                    } else {
                        attendanceByDate.put(dStr, "");
                    }
                    continue;
                }

                studentWorkingDays++;

                List<Attendance> morningRecs = dateRecs.stream()
                        .filter(r -> r.getPeriod() >= 1 && r.getPeriod() <= 3)
                        .toList();
                List<Attendance> afternoonRecs = dateRecs.stream()
                        .filter(r -> r.getPeriod() >= 4 && r.getPeriod() <= 5)
                        .toList();

                int morningTotal = morningRecs.size();
                long morningPresentCount = morningRecs.stream()
                        .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.ON_DUTY)
                        .count();

                int afternoonTotal = afternoonRecs.size();
                long afternoonPresentCount = afternoonRecs.stream()
                        .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.ON_DUTY)
                        .count();

                boolean morningPresent = morningTotal > 0 && morningPresentCount >= Math.ceil(morningTotal / 2.0);
                boolean afternoonPresent = afternoonTotal > 0 && afternoonPresentCount >= Math.ceil(afternoonTotal / 2.0);

                String dayStatus;
                if (morningTotal > 0 && afternoonTotal > 0) {
                    if (morningPresent && afternoonPresent) {
                        dayStatus = "P";
                    } else if (morningPresent || afternoonPresent) {
                        dayStatus = "H";
                    } else {
                        dayStatus = "A";
                    }
                } else if (morningTotal > 0) {
                    dayStatus = morningPresent ? "H" : "A";
                } else if (afternoonTotal > 0) {
                    dayStatus = afternoonPresent ? "H" : "A";
                } else {
                    boolean anyPresent = dateRecs.stream()
                            .anyMatch(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.ON_DUTY);
                    dayStatus = anyPresent ? "P" : "A";
                }

                attendanceByDate.put(dStr, dayStatus);

                if ("P".equals(dayStatus)) {
                    presentDays += 1.0;
                } else if ("H".equals(dayStatus)) {
                    presentDays += 0.5;
                    absentDays += 0.5;
                } else if ("A".equals(dayStatus)) {
                    absentDays += 1.0;
                }
            }

            int effectiveWorkingDays = totalWorkingDays > 0 ? totalWorkingDays : studentWorkingDays;
            double attendanceRate = effectiveWorkingDays > 0
                    ? Math.round((presentDays * 100.0 / effectiveWorkingDays) * 10.0) / 10.0
                    : 0.0;

            studentRows.add(new StudentLedgerRowResponse(
                    s.getId(),
                    s.getRollNo(),
                    s.getName(),
                    attendanceByDate,
                    effectiveWorkingDays,
                    presentDays,
                    absentDays,
                    attendanceRate
            ));
        }

        Map<String, Double> dailyPresentTotals = new HashMap<>();
        Map<String, Double> dailyAbsentTotals = new HashMap<>();
        Map<String, Integer> dailyStudentTotals = new HashMap<>();

        for (LocalDate d : sortedDates) {
            String dStr = d.toString();
            if (!datesWithAttendance.contains(d)) {
                dailyPresentTotals.put(dStr, 0.0);
                dailyAbsentTotals.put(dStr, 0.0);
                dailyStudentTotals.put(dStr, 0);
                continue;
            }

            double dayPres = 0.0;
            double dayAbs = 0.0;
            int dayTotal = 0;

            for (StudentLedgerRowResponse row : studentRows) {
                String st = row.getAttendanceByDate().get(dStr);
                if ("P".equals(st)) {
                    dayPres += 1.0;
                    dayTotal++;
                } else if ("H".equals(st)) {
                    dayPres += 0.5;
                    dayAbs += 0.5;
                    dayTotal++;
                } else if ("A".equals(st)) {
                    dayAbs += 1.0;
                    dayTotal++;
                }
            }

            dailyPresentTotals.put(dStr, dayPres);
            dailyAbsentTotals.put(dStr, dayAbs);
            dailyStudentTotals.put(dStr, dayTotal);
        }

        int classYear = year != null ? year : (students.isEmpty() ? 1 : students.get(0).getYear());
        String romanYear = switch (classYear) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(classYear);
        };

        String resolvedDeptName = "";
        if (scopedDeptId != null && !scopedDeptId.isBlank()) {
            resolvedDeptName = departmentRepository.findById(scopedDeptId).map(Department::getName).orElse(scopedDeptId);
        } else if (!students.isEmpty() && students.get(0).getDepartmentId() != null) {
            resolvedDeptName = departmentRepository.findById(students.get(0).getDepartmentId())
                    .map(Department::getName).orElse(students.get(0).getDepartmentId());
        }

        String resolvedCourseName = "";
        if (courseId != null && !courseId.isBlank()) {
            resolvedCourseName = courseRepository.findById(courseId).map(Course::getName).orElse(courseId);
        } else if (!students.isEmpty() && students.get(0).getCourseId() != null) {
            resolvedCourseName = courseRepository.findById(students.get(0).getCourseId())
                    .map(Course::getName).orElse(students.get(0).getCourseId());
        }

        String programLabel = !resolvedCourseName.isBlank() ? resolvedCourseName : (!resolvedDeptName.isBlank() ? resolvedDeptName : "MCA");

        LocalDate refDate = (startDate != null) ? startDate : (endDate != null ? endDate : LocalDate.now());
        int yr = refDate.getYear();
        int m = refDate.getMonthValue();
        String academicYear = (m >= 6) ? yr + "-" + (yr + 1) : (yr - 1) + "-" + yr;

        String className = "Class : " + romanYear + " " + programLabel + " " + academicYear;
        List<String> dateStrings = sortedDates.stream().map(LocalDate::toString).toList();

        return new AttendanceLedgerResponse(
                className,
                resolvedDeptName,
                resolvedCourseName,
                classYear,
                section,
                dateStrings,
                studentRows,
                dailyPresentTotals,
                dailyAbsentTotals,
                dailyStudentTotals,
                totalWorkingDays,
                students.size()
        );
    }
}
