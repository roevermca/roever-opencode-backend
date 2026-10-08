package com.ams.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.ams.model.Attendance;
import com.ams.model.AttendanceArchive;
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
    private final MongoTemplate mongoTemplate;

    public DataInitializer(
            DepartmentRepository departmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            MongoTemplate mongoTemplate) {
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
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

        // 2. Seed 20 Authentic Departments from Roever Arts & Science
        Department tam = seedDept("Tamil", "TAM");
        Department eng = seedDept("English", "ENG");
        Department com = seedDept("Commerce", "COM");
        Department comCaCs = seedDept("Commerce CA & Commerce CS", "COM-CA-CS");
        Department ms = seedDept("Management Studies", "MS");
        Department sw = seedDept("Social Work", "SW");
        Department math = seedDept("Mathematics", "MATH");
        Department phy = seedDept("Physics", "PHY");
        Department chem = seedDept("Chemistry", "CHEM");
        Department ca = seedDept("Computer Applications", "CA");
        Department cs = seedDept("Computer Science, Information Technology and Artificial Intelligence & Machine Learning", "CS");
        Department biotech = seedDept("Biotechnology", "BIOTECH");
        Department micro = seedDept("Microbiology", "MICRO");
        Department nd = seedDept("Nutrition and Dietetics", "ND");
        Department bot = seedDept("Botany", "BOT");
        Department zoo = seedDept("Zoology", "ZOO");
        Department ped = seedDept("Physical Education", "PED");
        Department hmcs = seedDept("Hotel Management & Catering Science", "HMCS");
        Department viscom = seedDept("Visual Communication", "VISCOM");
        Department pa = seedDept("Performing Arts", "PA");
        Department adminDept = seedDept("Administration", "ADMIN");

        // 3. Seed Arts & Science Degree Courses (UG: 3 Years, PG: 2 Years)
        // Tamil
        seedCourse("B.Lit. Tamil", "BLIT-TAM", tam.getId(), ProgramType.UG, 3);
        seedCourse("B.A. Tamil", "BA-TAM", tam.getId(), ProgramType.UG, 3);
        seedCourse("M.A. Tamil", "MA-TAM", tam.getId(), ProgramType.PG, 2);

        // English
        seedCourse("B.A. English", "BA-ENG", eng.getId(), ProgramType.UG, 3);
        seedCourse("M.A. English", "MA-ENG", eng.getId(), ProgramType.PG, 2);

        // Commerce
        seedCourse("B.Com", "BCOM", com.getId(), ProgramType.UG, 3);
        seedCourse("M.Com", "MCOM", com.getId(), ProgramType.PG, 2);

        // Commerce CA & Commerce CS
        seedCourse("B.Com (CA)", "BCOM-CA", comCaCs.getId(), ProgramType.UG, 3);
        seedCourse("B.Com (CS)", "BCOM-CS", comCaCs.getId(), ProgramType.UG, 3);
        seedCourse("M.Com (CA)", "MCOM-CA", comCaCs.getId(), ProgramType.PG, 2);

        // Management Studies
        seedCourse("BBA", "BBA", ms.getId(), ProgramType.UG, 3);
        seedCourse("MBA", "MBA", ms.getId(), ProgramType.PG, 2);

        // Social Work
        seedCourse("BSW", "BSW", sw.getId(), ProgramType.UG, 3);
        seedCourse("MSW", "MSW", sw.getId(), ProgramType.PG, 2);

        // Mathematics
        seedCourse("B.Sc Mathematics", "BSC-MATH", math.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Mathematics", "MSC-MATH", math.getId(), ProgramType.PG, 2);

        // Physics
        seedCourse("B.Sc Physics", "BSC-PHY", phy.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Physics", "MSC-PHY", phy.getId(), ProgramType.PG, 2);

        // Chemistry
        seedCourse("B.Sc Chemistry", "BSC-CHEM", chem.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Chemistry", "MSC-CHEM", chem.getId(), ProgramType.PG, 2);

        // Computer Applications
        seedCourse("BCA", "BCA", ca.getId(), ProgramType.UG, 3);
        seedCourse("MCA", "MCA", ca.getId(), ProgramType.PG, 2);

        // Computer Science, Information Technology and Artificial Intelligence & Machine Learning
        seedCourse("B.Sc Computer Science", "BSC-CS", cs.getId(), ProgramType.UG, 3);
        seedCourse("B.Sc Information Technology", "BSC-IT", cs.getId(), ProgramType.UG, 3);
        seedCourse("B.Sc Artificial Intelligence & Machine Learning", "BSC-AIML", cs.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Computer Science", "MSC-CS", cs.getId(), ProgramType.PG, 2);
        seedCourse("M.Sc Information Technology", "MSC-IT", cs.getId(), ProgramType.PG, 2);

        // Biotechnology
        seedCourse("B.Sc Biotechnology", "BSC-BIOTECH", biotech.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Biotechnology", "MSC-BIOTECH", biotech.getId(), ProgramType.PG, 2);

        // Microbiology
        seedCourse("B.Sc Microbiology", "BSC-MICRO", micro.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Microbiology", "MSC-MICRO", micro.getId(), ProgramType.PG, 2);

        // Nutrition and Dietetics
        seedCourse("B.Sc Nutrition and Dietetics", "BSC-ND", nd.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Nutrition and Dietetics", "MSC-ND", nd.getId(), ProgramType.PG, 2);

        // Botany
        seedCourse("B.Sc Botany", "BSC-BOT", bot.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Botany", "MSC-BOT", bot.getId(), ProgramType.PG, 2);

        // Zoology
        seedCourse("B.Sc Zoology", "BSC-ZOO", zoo.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Zoology", "MSC-ZOO", zoo.getId(), ProgramType.PG, 2);

        // Physical Education
        seedCourse("B.Sc Physical Education", "BSC-PED", ped.getId(), ProgramType.UG, 3);
        seedCourse("M.P.Ed", "MPED", ped.getId(), ProgramType.PG, 2);

        // Hotel Management & Catering Science
        seedCourse("B.Sc Hotel Management & Catering Science", "BSC-HMCS", hmcs.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Hotel Management & Catering Science", "MSC-HMCS", hmcs.getId(), ProgramType.PG, 2);

        // Visual Communication
        seedCourse("B.Sc Visual Communication", "BSC-VISCOM", viscom.getId(), ProgramType.UG, 3);
        seedCourse("M.Sc Visual Communication", "MSC-VISCOM", viscom.getId(), ProgramType.PG, 2);

        // Performing Arts
        seedCourse("B.A. Performing Arts", "BA-PA", pa.getId(), ProgramType.UG, 3);
        seedCourse("M.A. Performing Arts", "MA-PA", pa.getId(), ProgramType.PG, 2);

        // 4. Clean up any legacy mock/default accounts and dummy students
        List<String> legacyMockEmails = List.of(
            "admin@amsportal.edu",
            "vp@amsportal.edu",
            "hod.cs@amsportal.edu",
            "staff@amsportal.edu",
            "student@amsportal.edu",
            "priya.mca@amsportal.edu"
        );
        for (String mockEmail : legacyMockEmails) {
            userRepository.findByEmail(mockEmail).ifPresent(u -> {
                userRepository.delete(u);
                logger.info("Removed legacy mock account: {}", mockEmail);
            });
            studentRepository.findByEmail(mockEmail).ifPresent(s -> {
                studentRepository.delete(s);
                logger.info("Removed legacy mock student: {}", mockEmail);
            });
        }
        studentRepository.findByRollNo("23CA001").ifPresent(studentRepository::delete);
        studentRepository.findByRollNo("23MCA001").ifPresent(studentRepository::delete);

        // 5. Ensure System Master Admin exists ONLY if not already present (never overwrite user edits)
        seedUser("firebase-admin-master", "Roever Administrator", "roevermca09@gmail.com", Role.ADMIN, adminDept.getId());

        // Deduplicate any accidental multiple Master Admin entries from concurrent auto-heals
        List<User> masterAdmins = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && "roevermca09@gmail.com".equalsIgnoreCase(u.getEmail().trim()))
                .toList();
        if (masterAdmins.size() > 1) {
            logger.info("Found {} duplicate master admin records. Retaining primary and purging {} duplicates...",
                    masterAdmins.size(), masterAdmins.size() - 1);
            for (int i = 1; i < masterAdmins.size(); i++) {
                userRepository.delete(masterAdmins.get(i));
            }
        }

        userRepository.findAll().forEach(u ->
            logger.info("AMS_USER_LOADED: email={}, role={}, active={}, id={}", u.getEmail(), u.getRole(), u.isActive(), u.getId())
        );

        // 6. Purge any orphan attendance records left behind from previously deleted students
        List<String> validStudentIds = studentRepository.findAll().stream().map(Student::getId).toList();
        List<String> validRollNos = studentRepository.findAll().stream().map(Student::getRollNo).filter(r -> r != null && !r.isBlank()).toList();
        Set<String> allValidIds = new HashSet<>(validStudentIds);
        allValidIds.addAll(validRollNos);

        Query orphanQuery = new Query(Criteria.where("studentId").nin(allValidIds));
        long purgedAttendance = mongoTemplate.remove(orphanQuery, Attendance.class).getDeletedCount();
        mongoTemplate.remove(orphanQuery, AttendanceArchive.class);
        if (purgedAttendance > 0) {
            logger.info("Purged {} orphan attendance records of deleted students on system startup", purgedAttendance);
        }

        logger.info("Foundational Roever Arts & Science data check completed successfully.");
        } catch (Exception e) {
            logger.warn("Foundational data check deferred or MongoDB Atlas connection pending: {}", e.getMessage());
        }
    }

    private Department seedDept(String name, String code) {
        return departmentRepository.findByCode(code)
                .or(() -> departmentRepository.findByName(name))
                .map(existing -> {
                    if (!existing.getName().equals(name) || !existing.getCode().equals(code) || !existing.isActive()) {
                        existing.setName(name);
                        existing.setCode(code);
                        existing.setActive(true);
                        return departmentRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> departmentRepository.save(new Department(name, code, true)));
    }

    private Course seedCourse(String name, String code, String deptId, ProgramType type, int duration) {
        return courseRepository.findByCode(code)
                .map(existing -> {
                    if (!existing.getName().equals(name) || existing.getDurationYears() != duration || existing.getProgramType() != type || !existing.getDepartmentId().equals(deptId) || !existing.isActive()) {
                        existing.setName(name);
                        existing.setDepartmentId(deptId);
                        existing.setProgramType(type);
                        existing.setDurationYears(duration);
                        existing.setActive(true);
                        return courseRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> courseRepository.save(new Course(name, code, deptId, type, duration, true)));
    }

    private void seedUser(String uid, String name, String email, Role role, String deptId) {
        seedUser(uid, name, email, role, deptId, null);
    }

    private void seedUser(String uid, String name, String email, Role role, String deptId, String courseId) {
        String normalized = email.toLowerCase().trim();
        if (userRepository.findByEmail(normalized).isEmpty()) {
            User user = new User(uid, name, normalized, role, deptId, courseId, true);
            userRepository.save(user);
            logger.info("Created system admin account: {} with role {}", normalized, role);
        }
    }
}
