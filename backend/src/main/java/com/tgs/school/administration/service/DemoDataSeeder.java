package com.tgs.school.administration.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.entity.Subject;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SchoolClassRepository;
import com.tgs.school.academics.repository.SubjectRepository;
import com.tgs.school.academics.service.AcademicService;
import com.tgs.school.academics.service.AcademicService.*;
import com.tgs.school.administration.service.WebsiteCmsService.CreateEventRequest;
import com.tgs.school.administration.service.WebsiteCmsService.CreateGalleryRequest;
import com.tgs.school.attendance.service.AttendanceService;
import com.tgs.school.attendance.service.AttendanceService.*;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.CustomUserDetails;
import com.tgs.school.communication.service.AnnouncementService;
import com.tgs.school.communication.service.AnnouncementService.CreateAnnouncementRequest;
import com.tgs.school.examinations.service.ExaminationService;
import com.tgs.school.examinations.service.ExaminationService.*;
import com.tgs.school.fees.service.FeeService;
import com.tgs.school.fees.service.FeeService.*;
import com.tgs.school.homework.service.HomeworkService;
import com.tgs.school.homework.service.HomeworkService.CreateHomeworkRequest;
import com.tgs.school.library.entity.Book;
import com.tgs.school.library.service.LibraryService;
import com.tgs.school.library.service.LibraryService.*;
import com.tgs.school.students.service.StudentService;
import com.tgs.school.students.service.StudentService.*;
import com.tgs.school.teachers.service.TeacherService;
import com.tgs.school.teachers.service.TeacherService.*;
import com.tgs.school.timetable.service.TimetableService;
import com.tgs.school.timetable.service.TimetableService.SaveTimetableSlotRequest;
import com.tgs.school.transport.entity.Bus;
import com.tgs.school.transport.entity.Driver;
import com.tgs.school.transport.service.TransportService;
import com.tgs.school.transport.service.TransportService.*;
import com.tgs.school.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Populates fictional development/demo data across all TGS modules on first boot
 * when app.seed.demo-data=true and no users exist yet.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicService academicService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final ExaminationService examinationService;
    private final FeeService feeService;
    private final HomeworkService homeworkService;
    private final TimetableService timetableService;
    private final LibraryService libraryService;
    private final TransportService transportService;
    private final AnnouncementService announcementService;
    private final WebsiteCmsService websiteCmsService;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Seeding Telangana Grammar School fictional development dataset...");

        User superAdmin = userService.createUserAccount(
                "superadmin", "superadmin@tgs-dev.example", "9000000001", "System Super Admin", "Admin@123", Set.of(RoleName.SUPER_ADMIN));
        userService.createUserAccount(
                "principal", "principal@tgs-dev.example", "9000000002", "Dr. Kavitha Rao (Demo)", "Principal@123", Set.of(RoleName.PRINCIPAL));
        userService.createUserAccount(
                "admin", "admin@tgs-dev.example", "9000000003", "Ramesh Babu (Demo)", "Admin@123", Set.of(RoleName.ADMIN));
        userService.createUserAccount(
                "accountant", "accountant@tgs-dev.example", "9000000004", "Sujatha Devi (Demo)", "Accountant@123", Set.of(RoleName.ACCOUNTANT));
        userService.createUserAccount(
                "librarian", "librarian@tgs-dev.example", "9000000005", "Venkatesh K (Demo)", "Librarian@123", Set.of(RoleName.LIBRARIAN));

        CustomUserDetails adminPrincipal = new CustomUserDetails(superAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));

        try {
            seedAcademicAndOperations();
        } finally {
            SecurityContextHolder.clearContext();
        }
        log.info("Telangana Grammar School fictional seed complete.");
    }

    private void seedAcademicAndOperations() {
        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElseThrow();
        List<SchoolClass> classes = classRepository.findAllByOrderByLevelOrderAsc();
        Map<String, Subject> subjectByCode = new HashMap<>();
        subjectRepository.findAll().forEach(s -> subjectByCode.put(s.getCode(), s));

        // 1. Teachers (fictional sample faculty)
        TeacherDto t1 = createTeacher("TGS-EMP-001", "Anitha Sharma (Demo)", "anitha.teacher@tgs-dev.example", "9000000101", "teacher1", "Teacher@123", "M.Sc, B.Ed", "Mathematics & Science");
        TeacherDto t2 = createTeacher("TGS-EMP-002", "Srinivas Reddy (Demo)", "srinivas.teacher@tgs-dev.example", "9000000102", "teacher2", "Teacher@123", "M.A, B.Ed", "English & Social Studies");
        TeacherDto t3 = createTeacher("TGS-EMP-003", "Lakshmi Narayana (Demo)", "lakshmi.teacher@tgs-dev.example", "9000000103", "teacher3", "Teacher@123", "B.Ed, Montessori", "Pre-Primary & Telugu");

        // Non-teaching staff
        createStaff("TGS-STF-001", "Mallesh G (Demo)", "Office Clerk", "Administration", "9000000201", "18000");
        createStaff("TGS-STF-002", "Padma R (Demo)", "Campus Attendant", "Support", "9000000202", "12000");

        // 2. Sections (A & B for every class LKG - 10th) & Class Subjects
        Map<String, SectionDto> sectionMap = new LinkedHashMap<>();
        for (int i = 0; i < classes.size(); i++) {
            SchoolClass sc = classes.get(i);
            Long ctId = sc.isActivityGraded() ? t3.getId() : (i % 2 == 0 ? t1.getId() : t2.getId());
            SectionDto secA = createSection(sc.getId(), currentYear.getId(), "A", 40, "R-" + (100 + sc.getLevelOrder()), ctId);
            SectionDto secB = createSection(sc.getId(), currentYear.getId(), "B", 40, "R-" + (200 + sc.getLevelOrder()), null);
            sectionMap.put(sc.getName() + "-A", secA);
            sectionMap.put(sc.getName() + "-B", secB);

            if (sc.isActivityGraded()) {
                assignClassSubj(sc.getId(), subjectByCode.get("ELR").getId(), 50, 20);
                assignClassSubj(sc.getId(), subjectByCode.get("NPS").getId(), 50, 20);
                assignClassSubj(sc.getId(), subjectByCode.get("ACM").getId(), 50, 20);
            } else if (sc.getLevelOrder() <= 7) {
                assignClassSubj(sc.getId(), subjectByCode.get("ENG").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("TEL").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("MAT").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("EVS").getId(), 100, 35);
            } else {
                assignClassSubj(sc.getId(), subjectByCode.get("ENG").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("TEL").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("HIN").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("MAT").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("SCI").getId(), 100, 35);
                assignClassSubj(sc.getId(), subjectByCode.get("SOC").getId(), 100, 35);
            }
        }

        // 3. Assign teachers to key sections
        SectionDto lkgA = sectionMap.get("LKG-A");
        SectionDto class5A = sectionMap.get("5th Class-A");
        SectionDto class8A = sectionMap.get("8th Class-A");
        SectionDto class10A = sectionMap.get("10th Class-A");

        assignTeacher(t3.getId(), lkgA.getId(), subjectByCode.get("ELR").getId(), true);
        assignTeacher(t3.getId(), lkgA.getId(), subjectByCode.get("NPS").getId(), false);
        assignTeacher(t1.getId(), class5A.getId(), subjectByCode.get("MAT").getId(), true);
        assignTeacher(t2.getId(), class5A.getId(), subjectByCode.get("ENG").getId(), false);
        assignTeacher(t1.getId(), class8A.getId(), subjectByCode.get("MAT").getId(), false);
        assignTeacher(t2.getId(), class8A.getId(), subjectByCode.get("ENG").getId(), true);
        assignTeacher(t1.getId(), class10A.getId(), subjectByCode.get("SCI").getId(), true);
        assignTeacher(t2.getId(), class10A.getId(), subjectByCode.get("SOC").getId(), false);

        // 4. Admit fictional students (including 2 siblings under one parent account "parent1")
        StudentDetailDto stu1 = admitStudent("Aarav Kumar (Demo)", LocalDate.of(2016, 4, 12), "MALE", "B+",
                class5A.getClassId(), class5A.getId(), "student1", "Student@123",
                null, "Venkat Kumar (Demo)", "parent1@tgs-dev.example", "9000000301", "parent1", "Parent@123", "FATHER");

        StudentDetailDto stu2 = admitStudent("Ananya Kumar (Demo)", LocalDate.of(2021, 7, 19), "FEMALE", "O+",
                lkgA.getClassId(), lkgA.getId(), "student2", "Student@123",
                stu1.getParents().get(0).getParentId(), null, null, "9000000301", null, null, "FATHER");

        StudentDetailDto stu3 = admitStudent("Rohan Verma (Demo)", LocalDate.of(2013, 2, 8), "MALE", "A+",
                class8A.getClassId(), class8A.getId(), "student3", "Student@123",
                null, "Suresh Verma (Demo)", "parent2@tgs-dev.example", "9000000302", "parent2", "Parent@123", "FATHER");

        StudentDetailDto stu4 = admitStudent("Divya Sri (Demo)", LocalDate.of(2011, 9, 25), "FEMALE", "AB+",
                class10A.getClassId(), class10A.getId(), "student4", "Student@123",
                null, "Padmavathi S (Demo)", "parent3@tgs-dev.example", "9000000303", "parent3", "Parent@123", "MOTHER");

        // 5. Attendance sample (yesterday and today for Class 5A)
        MarkStudentAttendanceRequest attReq = new MarkStudentAttendanceRequest();
        attReq.setSectionId(class5A.getId());
        attReq.setAttendanceDate(LocalDate.now());
        StudentAttendanceEntry e1 = new StudentAttendanceEntry();
        e1.setStudentId(stu1.getId());
        e1.setStatus("PRESENT");
        attReq.setRecords(List.of(e1));
        attendanceService.markStudentAttendance(attReq);

        MarkStudentAttendanceRequest attReqLkg = new MarkStudentAttendanceRequest();
        attReqLkg.setSectionId(lkgA.getId());
        attReqLkg.setAttendanceDate(LocalDate.now());
        StudentAttendanceEntry e2 = new StudentAttendanceEntry();
        e2.setStudentId(stu2.getId());
        e2.setStatus("PRESENT");
        attReqLkg.setRecords(List.of(e2));
        attendanceService.markStudentAttendance(attReqLkg);

        // 6. Fee structures, assignments & sample payment
        FeeStructureDto fs5 = createFee(class5A.getClassId(), "Annual Tuition Fee - 5th Class", "TUITION", "18000");
        FeeStructureDto fsLkg = createFee(lkgA.getClassId(), "Annual Tuition Fee - LKG", "TUITION", "14000");
        FeeStructureDto fs8 = createFee(class8A.getClassId(), "Annual Tuition Fee - 8th Class", "TUITION", "22000");
        FeeStructureDto fs10 = createFee(class10A.getClassId(), "Annual Tuition Fee - 10th Class", "TUITION", "25000");

        List<StudentFeeDto> assigned5 = assignFee(fs5.getId());
        assignFee(fsLkg.getId());
        assignFee(fs8.getId());
        assignFee(fs10.getId());

        if (!assigned5.isEmpty()) {
            RecordPaymentRequest payReq = new RecordPaymentRequest();
            payReq.setStudentFeeId(assigned5.get(0).getId());
            payReq.setAmount(new BigDecimal("9000"));
            payReq.setPaymentMode("UPI");
            payReq.setTransactionReference("UPI-DEMO-1001");
            payReq.setRemarks("First installment");
            feeService.recordPayment(payReq);
        }

        // 7. Examination + Subjects + Marks (including LKG activity-based)
        CreateExamRequest exReq = new CreateExamRequest();
        exReq.setName("Formative Assessment 1 (2026-27)");
        exReq.setExamType("FORMATIVE");
        exReq.setStartDate(LocalDate.now().minusDays(10));
        exReq.setEndDate(LocalDate.now().minusDays(4));
        exReq.setDescription("First term formative assessment across LKG to 10th Class");
        ExamDto exam = examinationService.createExamination(exReq);

        ExamSubjectDto es5Mat = configureExamSubject(exam.getId(), class5A.getClassId(), subjectByCode.get("MAT").getId(), false, "100", "35");
        ExamSubjectDto es5Eng = configureExamSubject(exam.getId(), class5A.getClassId(), subjectByCode.get("ENG").getId(), false, "100", "35");
        ExamSubjectDto esLkgElr = configureExamSubject(exam.getId(), lkgA.getClassId(), subjectByCode.get("ELR").getId(), true, "50", "20");

        enterSingleMark(es5Mat.getId(), stu1.getId(), new BigDecimal("88"), null, "Strong problem solving");
        enterSingleMark(es5Eng.getId(), stu1.getId(), new BigDecimal("82"), null, "Good reading comprehension");
        enterSingleMark(esLkgElr.getId(), stu2.getId(), null, "EXCELLENT", "Participates enthusiastically in rhymes and storytelling");
        examinationService.updateExamStatus(exam.getId(), "PUBLISHED");

        // 8. Timetable slots
        saveTimetableSlot(class5A.getId(), "MONDAY", 1, LocalTime.of(9, 0), LocalTime.of(9, 45), false, null, subjectByCode.get("MAT").getId(), t1.getId(), "R-107");
        saveTimetableSlot(class5A.getId(), "MONDAY", 2, LocalTime.of(9, 45), LocalTime.of(10, 30), false, null, subjectByCode.get("ENG").getId(), t2.getId(), "R-107");
        saveTimetableSlot(class5A.getId(), "MONDAY", 3, LocalTime.of(10, 30), LocalTime.of(10, 45), true, "Morning Break", null, null, null);

        // 9. Homework (as teacher1)
        User t1User = userRepository.findById(t1.getUserId()).orElseThrow();
        CustomUserDetails t1Details = new CustomUserDetails(t1User);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(t1Details, null, t1Details.getAuthorities()));
        CreateHomeworkRequest hwReq = new CreateHomeworkRequest();
        hwReq.setSectionId(class5A.getId());
        hwReq.setSubjectId(subjectByCode.get("MAT").getId());
        hwReq.setTitle("Fractions Practice Worksheet #1");
        hwReq.setDescription("Solve exercises 1 to 10 from Chapter 4 on equivalent fractions.");
        hwReq.setDueDate(LocalDate.now().plusDays(3));
        hwReq.setAllowSubmission(true);
        homeworkService.createHomework(hwReq, null);

        // Switch back to superadmin for remaining seeding
        User adminUser = userRepository.findByUsernameAndDeletedAtIsNull("superadmin").orElseThrow();
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities()));

        // 10. Library books & issue
        Book b1 = createBook("978-0143440000", "Science Explorer - Class 5", "N. Sharma", "Science", 12, "S-01");
        createBook("978-0143440001", "Telangana Heritage & Stories", "R. Rao", "Literature", 8, "L-04");
        createBook("978-0143440002", "Foundation Mathematics", "K. Reddy", "Mathematics", 15, "M-02");

        IssueBookRequest issueReq = new IssueBookRequest();
        issueReq.setBookId(b1.getId());
        issueReq.setStudentId(stu1.getId());
        issueReq.setDueDate(LocalDate.now().plusDays(14));
        issueReq.setRemarks("Issued for weekly reading");
        libraryService.issueBook(issueReq);

        // 11. Transport bus, driver, route, allocation
        CreateBusRequest busReq = new CreateBusRequest();
        busReq.setBusNumber("BUS-01");
        busReq.setRegistrationNumber("TS-04-TGS-1001");
        busReq.setCapacity(42);
        busReq.setModel("School Star 42-Seater");
        Bus bus = transportService.createBus(busReq);

        CreateDriverRequest drvReq = new CreateDriverRequest();
        drvReq.setFullName("Nagesh P (Demo)");
        drvReq.setPhone("9000000401");
        drvReq.setLicenseNumber("TS04-2020-001122");
        drvReq.setLicenseExpiry(LocalDate.now().plusYears(3));
        Driver driver = transportService.createDriver(drvReq);

        CreateRouteRequest rtReq = new CreateRouteRequest();
        rtReq.setRouteName("Kalluru Town – Tallada Road");
        rtReq.setRouteCode("RT-01");
        rtReq.setBusId(bus.getId());
        rtReq.setDriverId(driver.getId());
        rtReq.setPickupPointsJson("[\"Kalluru Bus Stand\",\"Main Bazar\",\"Tallada Cross\"]");
        rtReq.setMonthlyFee(new BigDecimal("1200"));
        rtReq.setMorningStartTime(LocalTime.of(7, 45));
        rtReq.setAfternoonDepartureTime(LocalTime.of(16, 15));
        RouteDto route = transportService.createRoute(rtReq);

        AssignTransportRequest trAssign = new AssignTransportRequest();
        trAssign.setStudentId(stu1.getId());
        trAssign.setRouteId(route.getId());
        trAssign.setPickupPoint("Kalluru Bus Stand");
        trAssign.setDropPoint("Kalluru Bus Stand");
        transportService.assignStudent(trAssign);

        // 12. Announcements, Events, Gallery placeholders
        CreateAnnouncementRequest ann = new CreateAnnouncementRequest();
        ann.setTitle("Welcome to Academic Session 2026–2027");
        ann.setContent("Classes from LKG to 10th Class are active for the 2026–2027 session at Telangana Grammar School, Kalluru.");
        ann.setCategory("GENERAL");
        ann.setTargetAudience("ALL");
        ann.setPublic(true);
        announcementService.create(ann);

        CreateEventRequest ev = new CreateEventRequest();
        ev.setTitle("Annual Science & Art Exhibition (Editable Demo)");
        ev.setDescription("Students present science models and creative crafts. Details can be updated by Admin in Website CMS.");
        ev.setEventDate(LocalDate.now().plusDays(21));
        ev.setStartTime(LocalTime.of(9, 30));
        ev.setEndTime(LocalTime.of(14, 0));
        ev.setVenue("TGS Campus Ground, Kalluru");
        ev.setCategory("ACADEMIC");
        ev.setPublic(true);
        websiteCmsService.createEvent(ev);

        CreateGalleryRequest gal = new CreateGalleryRequest();
        gal.setTitle("Campus Learning Environment (Placeholder)");
        gal.setDescription("Admin can upload or link verified school photographs here via Website CMS.");
        gal.setCategory("CAMPUS");
        gal.setImageUrl("/favicon.svg");
        gal.setDisplayOrder(1);
        gal.setPublished(true);
        websiteCmsService.addGalleryItem(gal);
    }

    private TeacherDto createTeacher(String code, String name, String email, String phone, String username, String pass, String qual, String spec) {
        CreateTeacherRequest req = new CreateTeacherRequest();
        req.setEmployeeCode(code);
        req.setFullName(name);
        req.setEmail(email);
        req.setPhone(phone);
        req.setUsername(username);
        req.setPassword(pass);
        req.setQualification(qual);
        req.setSpecialization(spec);
        req.setJoiningDate(LocalDate.of(2024, 6, 1));
        return teacherService.createTeacher(req);
    }

    private void createStaff(String code, String name, String roleTitle, String dept, String phone, String salary) {
        CreateStaffRequest req = new CreateStaffRequest();
        req.setEmployeeCode(code);
        req.setFullName(name);
        req.setRoleTitle(roleTitle);
        req.setDepartment(dept);
        req.setPhone(phone);
        req.setJoiningDate(LocalDate.of(2024, 6, 1));
        req.setSalary(new BigDecimal(salary));
        teacherService.createStaff(req);
    }

    private SectionDto createSection(Long classId, Long yearId, String name, int cap, String room, Long classTeacherId) {
        CreateSectionRequest req = new CreateSectionRequest();
        req.setClassId(classId);
        req.setAcademicYearId(yearId);
        req.setName(name);
        req.setCapacity(cap);
        req.setRoomNumber(room);
        req.setClassTeacherId(classTeacherId);
        return academicService.createSection(req);
    }

    private void assignClassSubj(Long classId, Long subjectId, int max, int pass) {
        AssignClassSubjectRequest req = new AssignClassSubjectRequest();
        req.setClassId(classId);
        req.setSubjectId(subjectId);
        req.setMaxMarks(max);
        req.setPassingMarks(pass);
        academicService.assignSubjectToClass(req);
    }

    private void assignTeacher(Long teacherId, Long sectionId, Long subjectId, boolean isCt) {
        AssignTeacherRequest req = new AssignTeacherRequest();
        req.setTeacherId(teacherId);
        req.setSectionId(sectionId);
        req.setSubjectId(subjectId);
        req.setClassTeacher(isCt);
        teacherService.assignTeacher(req);
    }

    private StudentDetailDto admitStudent(
            String fullName, LocalDate dob, String gender, String blood,
            Long classId, Long sectionId, String stuUser, String stuPass,
            Long existingParentId, String parentName, String parentEmail, String parentPhone,
            String parentUser, String parentPass, String rel) {
        AdmitStudentRequest req = new AdmitStudentRequest();
        req.setFullName(fullName);
        req.setDateOfBirth(dob);
        req.setGender(gender);
        req.setBloodGroup(blood);
        req.setClassId(classId);
        req.setSectionId(sectionId);
        req.setStudentUsername(stuUser);
        req.setStudentPassword(stuPass);
        req.setExistingParentId(existingParentId);
        req.setParentFullName(parentName);
        req.setParentEmail(parentEmail);
        req.setParentPhone(parentPhone);
        req.setParentUsername(parentUser);
        req.setParentPassword(parentPass);
        req.setRelationship(rel);
        req.setAddress("Kalluru, Khammam, Telangana");
        return studentService.admitStudent(req);
    }

    private FeeStructureDto createFee(Long classId, String name, String type, String amount) {
        CreateFeeStructureRequest req = new CreateFeeStructureRequest();
        req.setClassId(classId);
        req.setName(name);
        req.setFeeType(type);
        req.setAmount(new BigDecimal(amount));
        req.setDueDate(LocalDate.now().plusMonths(1));
        return feeService.createFeeStructure(req);
    }

    private List<StudentFeeDto> assignFee(Long structureId) {
        AssignFeeRequest req = new AssignFeeRequest();
        req.setFeeStructureId(structureId);
        return feeService.assignFee(req);
    }

    private ExamSubjectDto configureExamSubject(Long examId, Long classId, Long subjectId, boolean activity, String max, String pass) {
        ConfigureExamSubjectRequest req = new ConfigureExamSubjectRequest();
        req.setExaminationId(examId);
        req.setClassId(classId);
        req.setSubjectId(subjectId);
        req.setActivityBased(activity);
        req.setMaxMarks(new BigDecimal(max));
        req.setPassingMarks(new BigDecimal(pass));
        req.setExamDate(LocalDate.now().minusDays(7));
        return examinationService.configureExamSubject(req);
    }

    private void enterSingleMark(Long examSubjId, Long studentId, BigDecimal marks, String rating, String remarks) {
        EnterMarksRequest req = new EnterMarksRequest();
        req.setExaminationSubjectId(examSubjId);
        StudentMarkInput input = new StudentMarkInput();
        input.setStudentId(studentId);
        input.setMarksObtained(marks);
        input.setActivityRating(rating);
        input.setDescriptiveRemarks(remarks);
        req.setMarks(List.of(input));
        examinationService.enterMarks(req);
    }

    private void saveTimetableSlot(Long sectionId, String day, int period, LocalTime start, LocalTime end,
                                   boolean isBreak, String breakLabel, Long subjectId, Long teacherId, String room) {
        SaveTimetableSlotRequest req = new SaveTimetableSlotRequest();
        req.setSectionId(sectionId);
        req.setDayOfWeek(day);
        req.setPeriodNumber(period);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setBreak(isBreak);
        req.setBreakLabel(breakLabel);
        req.setSubjectId(subjectId);
        req.setTeacherId(teacherId);
        req.setRoomNumber(room);
        timetableService.saveSlot(req);
    }

    private Book createBook(String isbn, String title, String author, String category, int copies, String shelf) {
        CreateBookRequest req = new CreateBookRequest();
        req.setIsbn(isbn);
        req.setTitle(title);
        req.setAuthor(author);
        req.setCategory(category);
        req.setTotalCopies(copies);
        req.setShelfLocation(shelf);
        return libraryService.addBook(req);
    }
}
