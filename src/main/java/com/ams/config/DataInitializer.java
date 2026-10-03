package com.ams.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ams.model.Course;
import com.ams.model.Department;
import com.ams.model.ProgramType;
import com.ams.model.Role;
import com.ams.model.Student;
import com.ams.model.User;
import com.ams.repository.CourseRepository;
import com.ams.repository.DepartmentRepository;
import com.ams.repository.StudentRepository;
import com.ams.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public DataInitializer(
            DepartmentRepository departmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            StudentRepository studentRepository) {
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("Initializing/validating foundational data for Roever Arts & Science College in MongoDB...");

        // 1. Deactivate legacy engineering departments and courses if previously created
        List<String> legacyEngineeringCodes = List.of("EEE", "ME", "CE", "CSE", "MBA");
        for (String legacyCode : legacyEngineeringCodes) {
            departmentRepository.findByCode(legacyCode).ifPresent(d -> {
                if (d.isActive()) {
                    d.setActive(false);
                    departmentRepository.save(d);
                    logger.info("Deactivated legacy engineering department: {}", legacyCode);
                }
            });
        }

        List<String> legacyCourseCodes = List.of("CSE-UG", "EEE-UG", "ME-UG", "CE-UG", "CSE-PG", "MBA-PG");
        for (String legacyCode : legacyCourseCodes) {
            courseRepository.findByCode(legacyCode).ifPresent(c -> {
                if (c.isActive()) {
                    c.setActive(false);
                    courseRepository.save(c);
                    logger.info("Deactivated legacy engineering course: {}", legacyCode);
                }
            });
        }

        // 2. Seed 18 Authentic Thanthai Hans Roever College Arts & Science Departments
        Department ca = seedDept("Computer Applications", "CA");
        Department cs = seedDept("Computer Science & IT", "CS");
        Department com = seedDept("Commerce", "COM");
        Department ms = seedDept("Management Studies", "MS");
        Department math = seedDept("Mathematics", "MATH");
        Department phy = seedDept("Physics", "PHY");
        Department chem = seedDept("Chemistry", "CHEM");
        Department biotech = seedDept("Biotechnology", "BIOTECH");
        Department bot = seedDept("Botany", "BOT");
        Department zoo = seedDept("Zoology", "ZOO");
        Department micro = seedDept("Microbiology", "MICRO");
        Department nd = seedDept("Nutrition and Dietetics", "ND");
        Department tam = seedDept("Tamil", "TAM");
        Department eng = seedDept("English", "ENG");
        Department sw = seedDept("Social Work", "SW");
        Department viscom = seedDept("Visual Communication", "VISCOM");
        Department hmcs = seedDept("Hotel Management", "HMCS");
        Department ped = seedDept("Physical Education", "PED");
        Department adminDept = seedDept("Administration", "ADMIN");

        // 3. Seed Arts & Science Degree Courses (UG: 3 Yrs, PG: 2 Yrs)
        Course bca = seedCourse("BCA", "BCA", ca.getId(), ProgramType.UG, 3);
        Course mca = seedCourse("MCA", "MCA", ca.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Computer Science", "BSC-CS", cs.getId(), ProgramType.UG, 3);
        seedCourse("B.Sc Information Technology", "BSC-IT", cs.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Computer Science", "MSC-CS", cs.getId(), ProgramType.PG, 2);

        seedCourse("B.Com", "BCOM", com.getId(), ProgramType.UG, 3);
        seedCourse("B.Com (CA)", "BCOM-CA", com.getId(), ProgramType.UG, 3);
        seedCourse("B.Com (CS)", "BCOM-CS", com.getId(), ProgramType.UG, 3);
        seedCourse("M.Com", "MCOM", com.getId(), ProgramType.PG, 2);

        seedCourse("BBA", "BBA", ms.getId(), ProgramType.UG, 3);
        seedCourse("MBA", "MBA", ms.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Mathematics", "BSC-MATH", math.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Mathematics", "MSC-MATH", math.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Physics", "BSC-PHY", phy.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Physics", "MSC-PHY", phy.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Chemistry", "BSC-CHEM", chem.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Chemistry", "MSC-CHEM", chem.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Biotechnology", "BSC-BIOTECH", biotech.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Biotechnology", "MSC-BIOTECH", biotech.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Botany", "BSC-BOT", bot.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Botany", "MSC-BOT", bot.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Zoology", "BSC-ZOO", zoo.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Zoology", "MSC-ZOO", zoo.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Microbiology", "BSC-MICRO", micro.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Microbiology", "MSC-MICRO", micro.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Nutrition & Dietetics", "BSC-ND", nd.getId(), ProgramType.UG, 3);

        seedCourse("B.Lit. Tamil", "BLIT-TAM", tam.getId(), ProgramType.UG, 3);
        seedCourse("M.A. Tamil", "MA-TAM", tam.getId(), ProgramType.PG, 2);

        seedCourse("B.A. English", "BA-ENG", eng.getId(), ProgramType.UG, 3);
        seedCourse("M.A. English", "MA-ENG", eng.getId(), ProgramType.PG, 2);

        seedCourse("BSW", "BSW", sw.getId(), ProgramType.UG, 3);
        seedCourse("MSW", "MSW", sw.getId(), ProgramType.PG, 2);

        seedCourse("B.Sc Visual Communication", "BSC-VISCOM", viscom.getId(), ProgramType.UG, 3);
        seedCourse("B.Sc Hotel Management & Catering Science", "BSC-HMCS", hmcs.getId(), ProgramType.UG, 3);
        seedCourse("B.Sc Physical Education", "BSC-PED", ped.getId(), ProgramType.UG, 3);

        // 4. Seed Foundational Accounts (Roever Master Admin, VP, HOD, Faculty, Student)
        seedUser("firebase-admin-01", "Dr. Rajesh Sharma", "admin@amsportal.edu", Role.ADMIN, adminDept.getId());
        seedUser("firebase-admin-master", "Roever Administrator", "roevermca09@gmail.com", Role.ADMIN, adminDept.getId());
        seedUser("firebase-vp-01", "Prof. K. Narayanan", "vp@amsportal.edu", Role.VP, adminDept.getId());
        seedUser("firebase-hod-01", "Dr. S. Venkatesh", "hod.cs@amsportal.edu", Role.HOD, ca.getId());
        seedUser("firebase-staff-01", "Mrs. Anitha R (MCA Faculty)", "staff@amsportal.edu", Role.STAFF, ca.getId(), mca.getId());
        seedUser("firebase-student-01", "Aravind Kumar (BCA)", "student@amsportal.edu", Role.STUDENT, ca.getId(), bca.getId());

        if (studentRepository.findByEmail("student@amsportal.edu").isEmpty()) {
            Student s = new Student("23CA001", "Aravind Kumar", "student@amsportal.edu", "9876543210", ca.getId(), bca.getId(), ProgramType.UG, 2, "A", true);
            studentRepository.save(s);
            logger.info("Created foundational student record for student@amsportal.edu in BCA");
        }

        if (studentRepository.findByEmail("priya.mca@amsportal.edu").isEmpty()) {
            Student s2 = new Student("23MCA001", "Priya V", "priya.mca@amsportal.edu", "9876543211", ca.getId(), mca.getId(), ProgramType.PG, 1, "A", true);
            studentRepository.save(s2);
            logger.info("Created foundational student record for priya.mca@amsportal.edu in MCA");
        }

        userRepository.findAll().forEach(u ->
            logger.info("AMS_USER_LOADED: email={}, role={}, active={}, id={}", u.getEmail(), u.getRole(), u.isActive(), u.getId())
        );

        logger.info("Foundational Roever Arts & Science data check completed successfully.");
    }

    private Department seedDept(String name, String code) {
        return departmentRepository.findByCode(code)
                .map(existing -> {
                    boolean updated = false;
                    if (!name.equalsIgnoreCase(existing.getName())) {
                        existing.setName(name);
                        updated = true;
                    }
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        updated = true;
                    }
                    return updated ? departmentRepository.save(existing) : existing;
                })
                .orElseGet(() -> departmentRepository.save(new Department(name, code, true)));
    }

    private Course seedCourse(String name, String code, String deptId, ProgramType type, int duration) {
        return courseRepository.findByCode(code)
                .map(existing -> {
                    boolean updated = false;
                    if (!name.equalsIgnoreCase(existing.getName())) {
                        existing.setName(name);
                        updated = true;
                    }
                    if (!deptId.equals(existing.getDepartmentId())) {
                        existing.setDepartmentId(deptId);
                        updated = true;
                    }
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        updated = true;
                    }
                    return updated ? courseRepository.save(existing) : existing;
                })
                .orElseGet(() -> courseRepository.save(new Course(name, code, deptId, type, duration, true)));
    }

    private void seedUser(String uid, String name, String email, Role role, String deptId) {
        seedUser(uid, name, email, role, deptId, null);
    }

    private void seedUser(String uid, String name, String email, Role role, String deptId, String courseId) {
        String normalized = email.toLowerCase().trim();
        userRepository.findByEmail(normalized).ifPresentOrElse(
            existing -> {
                boolean changed = false;
                if (existing.getRole() != role) {
                    existing.setRole(role);
                    changed = true;
                }
                if (deptId != null && !deptId.equals(existing.getDepartmentId())) {
                    existing.setDepartmentId(deptId);
                    changed = true;
                }
                if (courseId != null && !courseId.equals(existing.getCourseId())) {
                    existing.setCourseId(courseId);
                    changed = true;
                }
                if (!existing.isActive()) {
                    existing.setActive(true);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(existing);
                    logger.info("Updated existing system user {} to role {}, course {}", normalized, role, courseId);
                }
            },
            () -> {
                User user = new User(uid, name, normalized, role, deptId, courseId, true);
                userRepository.save(user);
                logger.info("Created foundational system account: {} with role {}, course {}", normalized, role, courseId);
            }
        );
    }
}
