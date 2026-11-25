package com.tgs.school.homework.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.Section;
import com.tgs.school.academics.entity.Subject;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.academics.repository.SubjectRepository;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.common.storage.StorageService;
import com.tgs.school.homework.entity.Homework;
import com.tgs.school.homework.entity.HomeworkSubmission;
import com.tgs.school.homework.entity.StudyMaterial;
import com.tgs.school.homework.repository.HomeworkRepository;
import com.tgs.school.homework.repository.HomeworkSubmissionRepository;
import com.tgs.school.homework.repository.StudyMaterialRepository;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.teachers.entity.Teacher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StorageService storageService;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    @Data
    public static class CreateHomeworkRequest {
        @NotNull private Long sectionId;
        @NotNull private Long subjectId;
        @NotBlank private String title;
        @NotBlank private String description;
        @NotNull private LocalDate dueDate;
        private boolean allowSubmission = true;
    }

    @Data
    public static class ReviewSubmissionRequest {
        @NotBlank private String status; // REVIEWED or RESUBMIT
        private String teacherFeedback;
        private String gradeOrScore;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HomeworkDto {
        private Long id;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private Long subjectId;
        private String subjectName;
        private String teacherName;
        private String title;
        private String description;
        private String attachmentName;
        private LocalDate dueDate;
        private boolean allowSubmission;
        private Instant createdAt;
        private SubmissionDto mySubmission;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubmissionDto {
        private Long id;
        private Long homeworkId;
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private String submissionText;
        private String attachmentName;
        private String status;
        private String teacherFeedback;
        private String gradeOrScore;
        private Instant submittedAt;
        private Instant reviewedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudyMaterialDto {
        private Long id;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private Long subjectId;
        private String subjectName;
        private String teacherName;
        private String title;
        private String description;
        private String materialType;
        private String fileName;
        private Instant createdAt;
    }

    @Transactional
    public HomeworkDto createHomework(CreateHomeworkRequest req, MultipartFile attachment) {
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active academic year"));
        Section section = sectionRepository.findById(req.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Teacher teacher = accessControlService.requireCurrentTeacher();
        accessControlService.assertTeacherAssignedToSubject(teacher.getId(), section.getId(), subject.getId(), ay.getId());

        String attName = null;
        String attPath = null;
        if (attachment != null && !attachment.isEmpty()) {
            StorageService.StoredFile stored = storageService.store(attachment, "homework");
            attName = stored.originalFileName();
            attPath = stored.storagePath();
        }

        Homework saved = homeworkRepository.save(Homework.builder()
                .academicYear(ay)
                .schoolClass(section.getSchoolClass())
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .title(req.getTitle().trim())
                .description(req.getDescription().trim())
                .attachmentName(attName)
                .attachmentPath(attPath)
                .dueDate(req.getDueDate())
                .allowSubmission(req.isAllowSubmission())
                .build());

        auditService.log("CREATE_HOMEWORK", "HOMEWORK", saved.getId().toString(), saved.getTitle());
        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public List<HomeworkDto> listHomework(Long classId, Long sectionId, Long teacherId) {
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue().orElse(null);
        return homeworkRepository.findFiltered(ay != null ? ay.getId() : null, classId, sectionId, teacherId)
                .stream().map(h -> toDto(h, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<HomeworkDto> listForStudent(Long studentId) {
        accessControlService.assertCanAccessStudent(studentId);
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active academic year"));
        StudentEnrollment en = enrollmentRepository.findByStudentAndYear(studentId, ay.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student enrollment not found"));
        return homeworkRepository.findFiltered(ay.getId(), en.getSchoolClass().getId(), en.getSection().getId(), null)
                .stream()
                .map(h -> {
                    SubmissionDto sub = submissionRepository.findByHomeworkIdAndStudentId(h.getId(), studentId)
                            .map(this::toSubmissionDto).orElse(null);
                    return toDto(h, sub);
                }).toList();
    }

    @Transactional
    public SubmissionDto submitHomework(Long homeworkId, String submissionText, MultipartFile attachment) {
        Student student = accessControlService.requireCurrentStudent();
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found"));

        HomeworkSubmission sub = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, student.getId())
                .orElse(HomeworkSubmission.builder().homework(hw).student(student).build());
        sub.setSubmissionText(submissionText);
        sub.setSubmittedAt(Instant.now());
        sub.setStatus(LocalDate.now().isAfter(hw.getDueDate()) ? "LATE" : "SUBMITTED");

        if (attachment != null && !attachment.isEmpty()) {
            StorageService.StoredFile stored = storageService.store(attachment, "submissions/" + homeworkId);
            sub.setAttachmentName(stored.originalFileName());
            sub.setAttachmentPath(stored.storagePath());
        }
        return toSubmissionDto(submissionRepository.save(sub));
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> listSubmissions(Long homeworkId) {
        return submissionRepository.findByHomeworkIdWithStudent(homeworkId).stream()
                .map(this::toSubmissionDto).toList();
    }

    @Transactional
    public SubmissionDto reviewSubmission(Long submissionId, ReviewSubmissionRequest req) {
        Teacher teacher = accessControlService.requireCurrentTeacher();
        HomeworkSubmission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        sub.setStatus(req.getStatus().toUpperCase());
        sub.setTeacherFeedback(req.getTeacherFeedback());
        sub.setGradeOrScore(req.getGradeOrScore());
        sub.setReviewedAt(Instant.now());
        sub.setReviewedByTeacher(teacher);
        return toSubmissionDto(submissionRepository.save(sub));
    }

    @Transactional
    public StudyMaterialDto uploadStudyMaterial(Long sectionId, Long subjectId, String title, String description, String materialType, MultipartFile file) {
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active academic year"));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Teacher teacher = accessControlService.requireCurrentTeacher();
        accessControlService.assertTeacherAssignedToSubject(teacher.getId(), section.getId(), subject.getId(), ay.getId());

        StorageService.StoredFile stored = storageService.store(file, "materials");
        StudyMaterial saved = studyMaterialRepository.save(StudyMaterial.builder()
                .academicYear(ay)
                .schoolClass(section.getSchoolClass())
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .title(title.trim())
                .description(description)
                .materialType(materialType != null ? materialType.toUpperCase() : "NOTES")
                .fileName(stored.originalFileName())
                .filePath(stored.storagePath())
                .contentType(stored.contentType())
                .build());
        return toMaterialDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialDto> listMaterials(Long classId, Long sectionId) {
        return studyMaterialRepository.findFiltered(classId, sectionId).stream().map(this::toMaterialDto).toList();
    }

    @Transactional(readOnly = true)
    public FileDownload downloadMaterial(Long materialId) {
        StudyMaterial sm = studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Study material not found"));
        Resource res = storageService.loadAsResource(sm.getFilePath());
        return new FileDownload(res, sm.getFileName(), sm.getContentType());
    }

    @Transactional(readOnly = true)
    public FileDownload downloadHomeworkAttachment(Long homeworkId) {
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found"));
        Resource res = storageService.loadAsResource(hw.getAttachmentPath());
        return new FileDownload(res, hw.getAttachmentName(), "application/octet-stream");
    }

    public record FileDownload(Resource resource, String fileName, String contentType) {}

    private HomeworkDto toDto(Homework h, SubmissionDto mySub) {
        return HomeworkDto.builder()
                .id(h.getId())
                .classId(h.getSchoolClass().getId())
                .className(h.getSchoolClass().getName())
                .sectionId(h.getSection().getId())
                .sectionName(h.getSection().getName())
                .subjectId(h.getSubject().getId())
                .subjectName(h.getSubject().getName())
                .teacherName(h.getTeacher().getFullName())
                .title(h.getTitle())
                .description(h.getDescription())
                .attachmentName(h.getAttachmentName())
                .dueDate(h.getDueDate())
                .allowSubmission(h.isAllowSubmission())
                .createdAt(h.getCreatedAt())
                .mySubmission(mySub)
                .build();
    }

    private SubmissionDto toSubmissionDto(HomeworkSubmission hs) {
        return SubmissionDto.builder()
                .id(hs.getId())
                .homeworkId(hs.getHomework().getId())
                .studentId(hs.getStudent().getId())
                .studentName(hs.getStudent().getFullName())
                .admissionNumber(hs.getStudent().getAdmissionNumber())
                .submissionText(hs.getSubmissionText())
                .attachmentName(hs.getAttachmentName())
                .status(hs.getStatus())
                .teacherFeedback(hs.getTeacherFeedback())
                .gradeOrScore(hs.getGradeOrScore())
                .submittedAt(hs.getSubmittedAt())
                .reviewedAt(hs.getReviewedAt())
                .build();
    }

    private StudyMaterialDto toMaterialDto(StudyMaterial sm) {
        return StudyMaterialDto.builder()
                .id(sm.getId())
                .classId(sm.getSchoolClass().getId())
                .className(sm.getSchoolClass().getName())
                .sectionId(sm.getSection() != null ? sm.getSection().getId() : null)
                .sectionName(sm.getSection() != null ? sm.getSection().getName() : null)
                .subjectId(sm.getSubject().getId())
                .subjectName(sm.getSubject().getName())
                .teacherName(sm.getTeacher().getFullName())
                .title(sm.getTitle())
                .description(sm.getDescription())
                .materialType(sm.getMaterialType())
                .fileName(sm.getFileName())
                .createdAt(sm.getCreatedAt())
                .build();
    }
}
