package com.tgs.school.examinations.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.entity.Subject;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SchoolClassRepository;
import com.tgs.school.academics.repository.SubjectRepository;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.examinations.entity.*;
import com.tgs.school.examinations.repository.*;
import com.tgs.school.reports.service.PdfGeneratorService;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.teachers.entity.Teacher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationSubjectRepository examSubjectRepository;
    private final GradingRuleRepository gradingRuleRepository;
    private final MarkRepository markRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final PdfGeneratorService pdfGeneratorService;
    private final AuditService auditService;

    @Data
    public static class CreateExamRequest {
        private Long academicYearId;
        @NotBlank private String name;
        @NotBlank private String examType; // UNIT_TEST, FORMATIVE, SUMMATIVE, HALF_YEARLY, ANNUAL
        @NotNull private LocalDate startDate;
        @NotNull private LocalDate endDate;
        private String description;
    }

    @Data
    public static class ConfigureExamSubjectRequest {
        @NotNull private Long examinationId;
        @NotNull private Long classId;
        @NotNull private Long subjectId;
        private LocalDate examDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal maxMarks = BigDecimal.valueOf(100);
        private BigDecimal passingMarks = BigDecimal.valueOf(35);
        private boolean isActivityBased;
        private String roomNumber;
    }

    @Data
    public static class EnterMarksRequest {
        @NotNull private Long examinationSubjectId;
        @NotEmpty @Valid private List<StudentMarkInput> marks;
    }

    @Data
    public static class StudentMarkInput {
        @NotNull private Long studentId;
        private BigDecimal marksObtained;
        private boolean isAbsent;
        private String activityRating;
        private String descriptiveRemarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExamDto {
        private Long id;
        private Long academicYearId;
        private String academicYearName;
        private String name;
        private String examType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExamSubjectDto {
        private Long id;
        private Long examinationId;
        private Long classId;
        private String className;
        private boolean classActivityGraded;
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private LocalDate examDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal maxMarks;
        private BigDecimal passingMarks;
        private boolean isActivityBased;
        private String roomNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MarkDto {
        private Long id;
        private Long examinationSubjectId;
        private Long examinationId;
        private String examinationName;
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private String subjectName;
        private BigDecimal maxMarks;
        private BigDecimal passingMarks;
        private BigDecimal marksObtained;
        private boolean isAbsent;
        private boolean isActivityBased;
        private String activityRating;
        private String descriptiveRemarks;
        private String grade;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReportCardDto {
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private String className;
        private String sectionName;
        private String rollNumber;
        private Long examinationId;
        private String examinationName;
        private String academicYearName;
        private boolean isActivityGradedClass;
        private List<MarkDto> subjects;
        private BigDecimal totalMarksObtained;
        private BigDecimal totalMaxMarks;
        private BigDecimal percentage;
        private String overallGrade;
        private String resultStatus;
    }

    @Transactional
    public ExamDto createExamination(CreateExamRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("Examination end date cannot be before start date");
        }
        AcademicYear ay = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId()).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : academicYearRepository.findByIsCurrentTrue().orElseThrow(() -> new ResourceNotFoundException("Current academic year not found"));

        Examination saved = examinationRepository.save(Examination.builder()
                .academicYear(ay)
                .name(req.getName().trim())
                .examType(req.getExamType().toUpperCase())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status("SCHEDULED")
                .description(req.getDescription())
                .build());
        auditService.log("CREATE_EXAM", "EXAMINATION", saved.getId().toString(), saved.getName());
        return toExamDto(saved);
    }

    @Transactional
    public ExamDto updateExamStatus(Long examId, String status) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        exam.setStatus(status.toUpperCase());
        return toExamDto(examinationRepository.save(exam));
    }

    @Transactional(readOnly = true)
    public List<ExamDto> listExaminations(Long yearId) {
        return examinationRepository.findByYear(yearId).stream().map(this::toExamDto).toList();
    }

    @Transactional
    public ExamSubjectDto configureExamSubject(ConfigureExamSubjectRequest req) {
        Examination exam = examinationRepository.findById(req.getExaminationId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        SchoolClass sc = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Subject sub = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        ExaminationSubject es = examSubjectRepository
                .findByExaminationIdAndSchoolClassIdAndSubjectId(exam.getId(), sc.getId(), sub.getId())
                .orElse(ExaminationSubject.builder().examination(exam).schoolClass(sc).subject(sub).build());
        es.setExamDate(req.getExamDate());
        es.setStartTime(req.getStartTime());
        es.setEndTime(req.getEndTime());
        es.setMaxMarks(req.getMaxMarks() != null ? req.getMaxMarks() : BigDecimal.valueOf(100));
        es.setPassingMarks(req.getPassingMarks() != null ? req.getPassingMarks() : BigDecimal.valueOf(35));
        es.setActivityBased(req.isActivityBased() || sc.isActivityGraded() || sub.isActivityBased());
        es.setRoomNumber(req.getRoomNumber());

        return toSubjectDto(examSubjectRepository.save(es));
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectDto> listExamSubjects(Long examId, Long classId) {
        return examSubjectRepository.findByExamAndClass(examId, classId).stream().map(this::toSubjectDto).toList();
    }

    @Transactional
    public List<MarkDto> enterMarks(EnterMarksRequest req) {
        ExaminationSubject es = examSubjectRepository.findById(req.getExaminationSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));
        AcademicYear ay = es.getExamination().getAcademicYear();
        User editor = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);

        Teacher teacher = null;
        if (!accessControlService.isSchoolWideStaff()) {
            teacher = accessControlService.requireCurrentTeacher();
        }

        List<GradingRule> rules = gradingRuleRepository.findApplicableRules(ay.getId());
        List<MarkDto> results = new ArrayList<>();

        for (StudentMarkInput input : req.getMarks()) {
            Student student = studentRepository.findById(input.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + input.getStudentId()));
            StudentEnrollment en = enrollmentRepository.findByStudentAndYear(student.getId(), ay.getId())
                    .orElseThrow(() -> new BadRequestException("Student " + student.getFullName() + " not enrolled in " + ay.getName()));
            if (!en.getSchoolClass().getId().equals(es.getSchoolClass().getId())) {
                throw new BadRequestException("Student " + student.getFullName() + " is not in class " + es.getSchoolClass().getName());
            }
            if (teacher != null) {
                accessControlService.assertTeacherAssignedToSubject(
                        teacher.getId(), en.getSection().getId(), es.getSubject().getId(), ay.getId());
            }

            Mark mark = markRepository.findByExaminationSubjectIdAndStudentId(es.getId(), student.getId())
                    .orElse(Mark.builder().examinationSubject(es).student(student).enrollment(en).build());

            mark.setAbsent(input.isAbsent());
            mark.setDescriptiveRemarks(input.getDescriptiveRemarks());
            mark.setEnteredBy(editor);

            if (es.isActivityBased() || es.getSchoolClass().isActivityGraded()) {
                mark.setActivityRating(input.getActivityRating() != null ? input.getActivityRating() : "GOOD");
                mark.setMarksObtained(null);
                mark.setGrade(mapActivityGrade(mark.getActivityRating()));
            } else {
                if (input.isAbsent()) {
                    mark.setMarksObtained(BigDecimal.ZERO);
                    mark.setGrade("AB");
                } else {
                    if (input.getMarksObtained() == null) {
                        throw new BadRequestException("Marks obtained is required for " + student.getFullName());
                    }
                    if (input.getMarksObtained().compareTo(BigDecimal.ZERO) < 0
                            || input.getMarksObtained().compareTo(es.getMaxMarks()) > 0) {
                        throw new BadRequestException("Marks for " + student.getFullName() + " must be between 0 and " + es.getMaxMarks());
                    }
                    mark.setMarksObtained(input.getMarksObtained());
                    BigDecimal pct = input.getMarksObtained()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(es.getMaxMarks(), 2, RoundingMode.HALF_UP);
                    mark.setGrade(calculateGrade(pct, rules));
                }
            }
            results.add(toMarkDto(markRepository.save(mark)));
        }

        auditService.log("ENTER_MARKS", "EXAMINATION_SUBJECT", es.getId().toString(),
                "Entered " + results.size() + " marks for " + es.getSchoolClass().getName() + " - " + es.getSubject().getName());
        return results;
    }

    @Transactional(readOnly = true)
    public List<MarkDto> getMarksForExamSubject(Long examSubjectId) {
        return markRepository.findByExamSubject(examSubjectId).stream().map(this::toMarkDto).toList();
    }

    @Transactional(readOnly = true)
    public ReportCardDto getStudentReportCard(Long studentId, Long examId) {
        accessControlService.assertCanAccessStudent(studentId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        StudentEnrollment en = enrollmentRepository.findByStudentAndYear(studentId, exam.getAcademicYear().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student enrollment not found for examination year"));

        List<Mark> marks = markRepository.findByStudentAndExam(studentId, examId);
        List<MarkDto> subjectMarks = marks.stream().map(this::toMarkDto).toList();
        boolean activityClass = en.getSchoolClass().isActivityGraded();

        BigDecimal totalObtained = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;
        boolean anyFailed = false;

        for (Mark m : marks) {
            if (!m.getExaminationSubject().isActivityBased()) {
                totalMax = totalMax.add(m.getExaminationSubject().getMaxMarks());
                if (!m.isAbsent() && m.getMarksObtained() != null) {
                    totalObtained = totalObtained.add(m.getMarksObtained());
                    if (m.getMarksObtained().compareTo(m.getExaminationSubject().getPassingMarks()) < 0) {
                        anyFailed = true;
                    }
                } else {
                    anyFailed = true;
                }
            }
        }

        BigDecimal pct = totalMax.compareTo(BigDecimal.ZERO) > 0
                ? totalObtained.multiply(BigDecimal.valueOf(100)).divide(totalMax, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<GradingRule> rules = gradingRuleRepository.findApplicableRules(exam.getAcademicYear().getId());
        String overallGrade = activityClass ? "ACTIVITY_GRADED" : calculateGrade(pct, rules);
        String resultStatus = activityClass ? "PROMOTED_ACTIVITY" : (marks.isEmpty() ? "PENDING" : (anyFailed ? "NEEDS_IMPROVEMENT" : "PASS"));

        return ReportCardDto.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .admissionNumber(student.getAdmissionNumber())
                .className(en.getSchoolClass().getName())
                .sectionName(en.getSection().getName())
                .rollNumber(en.getRollNumber())
                .examinationId(exam.getId())
                .examinationName(exam.getName())
                .academicYearName(exam.getAcademicYear().getName())
                .isActivityGradedClass(activityClass)
                .subjects(subjectMarks)
                .totalMarksObtained(totalObtained)
                .totalMaxMarks(totalMax)
                .percentage(pct)
                .overallGrade(overallGrade)
                .resultStatus(resultStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportReportCardPdf(Long studentId, Long examId) {
        ReportCardDto rc = getStudentReportCard(studentId, examId);
        List<PdfGeneratorService.ReportCardRow> rows = rc.getSubjects().stream()
                .map(m -> new PdfGeneratorService.ReportCardRow(
                        m.getSubjectName(),
                        m.isActivityBased(),
                        m.getMaxMarks() != null ? m.getMaxMarks().toPlainString() : "-",
                        m.isAbsent() ? "AB" : (m.getMarksObtained() != null ? m.getMarksObtained().toPlainString() : "-"),
                        m.getActivityRating(),
                        m.getGrade(),
                        m.getDescriptiveRemarks()
                ))
                .toList();

        return pdfGeneratorService.generateReportCardPdf(new PdfGeneratorService.ReportCardData(
                "TELANGANA GRAMMAR SCHOOL",
                "Kalluru, Khammam, Telangana, India",
                rc.getExaminationName(),
                rc.getAcademicYearName(),
                rc.getStudentName(),
                rc.getAdmissionNumber(),
                rc.getClassName(),
                rc.getSectionName(),
                rc.getRollNumber(),
                rc.isActivityGradedClass(),
                rows,
                rc.getTotalMarksObtained() + " / " + rc.getTotalMaxMarks(),
                rc.getPercentage() + "%",
                rc.getOverallGrade(),
                "Result Status: " + rc.getResultStatus()
        ));
    }

    @Transactional(readOnly = true)
    public List<MarkDto> getClassPerformance(Long examId, Long classId) {
        return markRepository.findByExamAndClass(examId, classId).stream().map(this::toMarkDto).toList();
    }

    @Transactional(readOnly = true)
    public List<GradingRule> listGradingRules() {
        return gradingRuleRepository.findApplicableRules(null);
    }

    @Data
    public static class SaveGradingRuleRequest {
        @NotBlank private String gradeName;
        @NotNull private BigDecimal minPercentage;
        @NotNull private BigDecimal maxPercentage;
        private BigDecimal gradePoint;
        private String description;
        private boolean passing = true;
    }

    @Transactional
    public GradingRule createGradingRule(SaveGradingRuleRequest req) {
        if (req.getMaxPercentage().compareTo(req.getMinPercentage()) < 0) {
            throw new BadRequestException("Max percentage cannot be less than min percentage");
        }
        return gradingRuleRepository.save(GradingRule.builder()
                .gradeName(req.getGradeName().trim().toUpperCase())
                .minPercentage(req.getMinPercentage())
                .maxPercentage(req.getMaxPercentage())
                .gradePoint(req.getGradePoint())
                .description(req.getDescription())
                .isPassing(req.isPassing())
                .build());
    }

    @Transactional
    public void deleteGradingRule(Long id) {
        if (!gradingRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Grading rule not found");
        }
        gradingRuleRepository.deleteById(id);
    }

    private String calculateGrade(BigDecimal pct, List<GradingRule> rules) {
        for (GradingRule rule : rules) {
            if (pct.compareTo(rule.getMinPercentage()) >= 0 && pct.compareTo(rule.getMaxPercentage()) <= 0) {
                return rule.getGradeName();
            }
        }
        return "D";
    }

    private String mapActivityGrade(String rating) {
        return switch (rating.toUpperCase()) {
            case "EXCELLENT" -> "A+";
            case "GOOD" -> "A";
            case "SATISFACTORY" -> "B";
            default -> "C";
        };
    }

    private ExamDto toExamDto(Examination e) {
        return ExamDto.builder()
                .id(e.getId())
                .academicYearId(e.getAcademicYear().getId())
                .academicYearName(e.getAcademicYear().getName())
                .name(e.getName())
                .examType(e.getExamType())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .status(e.getStatus())
                .description(e.getDescription())
                .build();
    }

    private ExamSubjectDto toSubjectDto(ExaminationSubject es) {
        return ExamSubjectDto.builder()
                .id(es.getId())
                .examinationId(es.getExamination().getId())
                .classId(es.getSchoolClass().getId())
                .className(es.getSchoolClass().getName())
                .classActivityGraded(es.getSchoolClass().isActivityGraded())
                .subjectId(es.getSubject().getId())
                .subjectName(es.getSubject().getName())
                .subjectCode(es.getSubject().getCode())
                .examDate(es.getExamDate())
                .startTime(es.getStartTime())
                .endTime(es.getEndTime())
                .maxMarks(es.getMaxMarks())
                .passingMarks(es.getPassingMarks())
                .isActivityBased(es.isActivityBased())
                .roomNumber(es.getRoomNumber())
                .build();
    }

    private MarkDto toMarkDto(Mark m) {
        ExaminationSubject es = m.getExaminationSubject();
        return MarkDto.builder()
                .id(m.getId())
                .examinationSubjectId(es.getId())
                .examinationId(es.getExamination().getId())
                .examinationName(es.getExamination().getName())
                .studentId(m.getStudent().getId())
                .studentName(m.getStudent().getFullName())
                .admissionNumber(m.getStudent().getAdmissionNumber())
                .subjectName(es.getSubject().getName())
                .maxMarks(es.getMaxMarks())
                .passingMarks(es.getPassingMarks())
                .marksObtained(m.getMarksObtained())
                .isAbsent(m.isAbsent())
                .isActivityBased(es.isActivityBased())
                .activityRating(m.getActivityRating())
                .descriptiveRemarks(m.getDescriptiveRemarks())
                .grade(m.getGrade())
                .build();
    }
}
