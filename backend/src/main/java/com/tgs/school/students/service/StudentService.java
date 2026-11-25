package com.tgs.school.students.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.entity.Section;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SchoolClassRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.common.storage.StorageService;
import com.tgs.school.parents.entity.ParentProfile;
import com.tgs.school.parents.repository.ParentRepository;
import com.tgs.school.parents.service.ParentService;
import com.tgs.school.students.entity.*;
import com.tgs.school.students.repository.*;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.repository.TeacherRepository;
import com.tgs.school.users.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentDocumentRepository documentRepository;
    private final StudentRemarkRepository remarkRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ParentRepository parentRepository;
    private final ParentService parentService;
    private final StorageService storageService;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    @Data
    public static class AdmitStudentRequest {
        @NotBlank private String fullName;
        @NotNull private LocalDate dateOfBirth;
        @NotBlank private String gender;
        private String bloodGroup;
        private LocalDate admissionDate;
        private String address;
        private String previousSchool;
        private String remarks;

        // Enrollment
        private Long academicYearId;
        @NotNull private Long classId;
        @NotNull private Long sectionId;
        private String rollNumber;

        // Student login credentials (optional - auto generated if omitted)
        private String studentUsername;
        private String studentEmail;
        private String studentPassword;

        // Parent details
        private Long existingParentId;
        private String parentFullName;
        @Email private String parentEmail;
        private String parentPhone;
        private String parentOccupation;
        private String parentAddress;
        private String relationship = "FATHER";
        private String parentUsername;
        private String parentPassword;
    }

    @Data
    public static class UpdateStudentRequest {
        @NotBlank private String fullName;
        @NotNull private LocalDate dateOfBirth;
        @NotBlank private String gender;
        private String bloodGroup;
        private String address;
        private String previousSchool;
        private String remarks;
        private String rollNumber;
        private Long sectionId;
    }

    @Data
    public static class PromoteStudentsRequest {
        @NotNull private Long fromAcademicYearId;
        @NotNull private Long toAcademicYearId;
        @NotNull private Long targetClassId;
        @NotNull private Long targetSectionId;
        @NotNull private List<Long> studentIds;
        private String remarks;
    }

    @Data
    public static class AddRemarkRequest {
        @NotBlank private String remarkType;
        @NotBlank private String remarkText;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentSummaryDto {
        private Long id;
        private String admissionNumber;
        private String fullName;
        private LocalDate dateOfBirth;
        private String gender;
        private String bloodGroup;
        private String status;
        private LocalDate admissionDate;
        private Long academicYearId;
        private String academicYearName;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private String rollNumber;
        private String primaryParentName;
        private String primaryParentPhone;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentDetailDto {
        private Long id;
        private Long userId;
        private String username;
        private String admissionNumber;
        private String fullName;
        private LocalDate dateOfBirth;
        private String gender;
        private String bloodGroup;
        private String photoUrl;
        private LocalDate admissionDate;
        private String address;
        private String previousSchool;
        private String status;
        private String remarks;
        private EnrollmentDto currentEnrollment;
        private List<EnrollmentDto> enrollmentHistory;
        private List<ParentLinkDto> parents;
        private List<DocumentDto> documents;
        private List<RemarkDto> teacherRemarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnrollmentDto {
        private Long id;
        private Long academicYearId;
        private String academicYearName;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private String rollNumber;
        private String enrollmentStatus;
        private LocalDate enrolledAt;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParentLinkDto {
        private Long parentId;
        private String fullName;
        private String email;
        private String phone;
        private String occupation;
        private String address;
        private String relationship;
        private boolean isPrimary;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentDto {
        private Long id;
        private String documentType;
        private String title;
        private String fileName;
        private String contentType;
        private long fileSize;
        private Instant createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RemarkDto {
        private Long id;
        private String teacherName;
        private String academicYearName;
        private String remarkType;
        private String remarkText;
        private Instant createdAt;
    }

    @Transactional
    public StudentDetailDto admitStudent(AdmitStudentRequest req) {
        AcademicYear year = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId())
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : academicYearRepository.findByIsCurrentTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("No current academic year"));
        SchoolClass schoolClass = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Section section = sectionRepository.findById(req.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (!section.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw new BadRequestException("Section does not belong to selected class");
        }
        long currentStrength = enrollmentRepository.countBySectionIdAndAcademicYearIdAndEnrollmentStatus(
                section.getId(), year.getId(), "ACTIVE");
        if (currentStrength >= section.getCapacity()) {
            throw new ConflictException("Selected section has reached maximum capacity of " + section.getCapacity());
        }

        long nextSeq = studentRepository.findMaxId().orElse(0L) + 1001;
        String admissionNo = "TGS-" + LocalDate.now().getYear() + "-" + String.format("%04d", nextSeq);
        while (studentRepository.existsByAdmissionNumber(admissionNo)) {
            nextSeq++;
            admissionNo = "TGS-" + LocalDate.now().getYear() + "-" + String.format("%04d", nextSeq);
        }

        String stuUsername = (req.getStudentUsername() != null && !req.getStudentUsername().isBlank())
                ? req.getStudentUsername().trim().toLowerCase()
                : admissionNo.toLowerCase();
        String stuEmail = (req.getStudentEmail() != null && !req.getStudentEmail().isBlank())
                ? req.getStudentEmail().trim().toLowerCase()
                : stuUsername + "@student.tgs.edu.in";
        String stuPassword = (req.getStudentPassword() != null && !req.getStudentPassword().isBlank())
                ? req.getStudentPassword()
                : "Student@123";

        User studentUser = userService.createUserAccount(
                stuUsername, stuEmail, req.getParentPhone(), req.getFullName(), stuPassword, Set.of(RoleName.STUDENT));

        Student student = studentRepository.save(Student.builder()
                .user(studentUser)
                .admissionNumber(admissionNo)
                .fullName(req.getFullName().trim())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender().trim())
                .bloodGroup(req.getBloodGroup())
                .admissionDate(req.getAdmissionDate() != null ? req.getAdmissionDate() : LocalDate.now())
                .address(req.getAddress())
                .previousSchool(req.getPreviousSchool())
                .remarks(req.getRemarks())
                .status("ACTIVE")
                .build());

        // Link or create parent account
        ParentProfile parent = null;
        if (req.getExistingParentId() != null) {
            parent = parentRepository.findById(req.getExistingParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Existing parent not found"));
        } else if (req.getParentFullName() != null && !req.getParentFullName().isBlank()
                && req.getParentEmail() != null && !req.getParentEmail().isBlank()
                && req.getParentPhone() != null && !req.getParentPhone().isBlank()) {
            ParentService.CreateParentRequest pReq = new ParentService.CreateParentRequest();
            pReq.setFullName(req.getParentFullName());
            pReq.setEmail(req.getParentEmail());
            pReq.setPhone(req.getParentPhone());
            pReq.setOccupation(req.getParentOccupation());
            pReq.setAddress(req.getParentAddress() != null ? req.getParentAddress() : req.getAddress());
            pReq.setUsername(req.getParentUsername());
            pReq.setPassword(req.getParentPassword());
            parent = parentService.findOrCreateParent(pReq);
        }

        if (parent != null) {
            studentParentRepository.save(StudentParent.builder()
                    .student(student)
                    .parent(parent)
                    .relationship(req.getRelationship() != null ? req.getRelationship() : "FATHER")
                    .isPrimary(true)
                    .build());
        }

        String rollNo = (req.getRollNumber() != null && !req.getRollNumber().isBlank())
                ? req.getRollNumber().trim()
                : String.format("%02d", currentStrength + 1);

        enrollmentRepository.save(StudentEnrollment.builder()
                .student(student)
                .academicYear(year)
                .schoolClass(schoolClass)
                .section(section)
                .rollNumber(rollNo)
                .enrollmentStatus("ACTIVE")
                .enrolledAt(LocalDate.now())
                .build());

        auditService.log("ADMIT_STUDENT", "STUDENT", student.getId().toString(),
                "Admitted " + student.getFullName() + " (" + admissionNo + ") to " + schoolClass.getName() + "-" + section.getName());
        return getStudentDetail(student.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentSummaryDto> listStudents(Long yearId, Long classId, Long sectionId, String status, String q, Pageable pageable) {
        Long resolvedYear = yearId != null ? yearId
                : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        Page<StudentEnrollment> page = enrollmentRepository.searchEnrollments(resolvedYear, classId, sectionId, status, q, pageable);
        return PageResponse.from(page.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public StudentDetailDto getStudentDetail(Long studentId) {
        accessControlService.assertCanAccessStudent(studentId);
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return buildDetailDto(s);
    }

    @Transactional(readOnly = true)
    public StudentDetailDto getMyProfile() {
        Student s = accessControlService.requireCurrentStudent();
        return buildDetailDto(s);
    }

    @Transactional
    public StudentDetailDto updateStudent(Long studentId, UpdateStudentRequest req) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        s.setFullName(req.getFullName().trim());
        s.setDateOfBirth(req.getDateOfBirth());
        s.setGender(req.getGender().trim());
        s.setBloodGroup(req.getBloodGroup());
        s.setAddress(req.getAddress());
        s.setPreviousSchool(req.getPreviousSchool());
        s.setRemarks(req.getRemarks());
        s.getUser().setFullName(req.getFullName().trim());
        userRepository.save(s.getUser());
        studentRepository.save(s);

        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);
        if (currentYear != null) {
            enrollmentRepository.findByStudentAndYear(studentId, currentYear.getId()).ifPresent(en -> {
                if (req.getRollNumber() != null) en.setRollNumber(req.getRollNumber());
                if (req.getSectionId() != null) {
                    Section sec = sectionRepository.findById(req.getSectionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
                    en.setSection(sec);
                    en.setSchoolClass(sec.getSchoolClass());
                }
                enrollmentRepository.save(en);
            });
        }
        auditService.log("UPDATE_STUDENT", "STUDENT", studentId.toString(), "Updated profile for " + s.getAdmissionNumber());
        return buildDetailDto(s);
    }

    @Transactional
    public void updateStudentStatus(Long studentId, String newStatus, String reason) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        s.setStatus(newStatus.toUpperCase());
        if (reason != null && !reason.isBlank()) {
            s.setRemarks((s.getRemarks() != null ? s.getRemarks() + "\n" : "") + "[" + newStatus + "] " + reason);
        }
        if ("ARCHIVED".equalsIgnoreCase(newStatus) || "WITHDRAWN".equalsIgnoreCase(newStatus) || "TRANSFERRED".equalsIgnoreCase(newStatus)) {
            s.getUser().setEnabled(false);
            userRepository.save(s.getUser());
            if ("ARCHIVED".equalsIgnoreCase(newStatus)) {
                s.setDeletedAt(Instant.now());
            }
            AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);
            if (currentYear != null) {
                enrollmentRepository.findByStudentAndYear(studentId, currentYear.getId()).ifPresent(en -> {
                    en.setEnrollmentStatus(newStatus.toUpperCase());
                    enrollmentRepository.save(en);
                });
            }
        }
        studentRepository.save(s);
        auditService.log("UPDATE_STUDENT_STATUS", "STUDENT", studentId.toString(), "Status -> " + newStatus);
    }

    @Transactional
    public List<StudentSummaryDto> promoteStudents(PromoteStudentsRequest req) {
        if (req.getFromAcademicYearId().equals(req.getToAcademicYearId())) {
            throw new BadRequestException("Source and destination academic years must be different");
        }
        AcademicYear toYear = academicYearRepository.findById(req.getToAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Target academic year not found"));
        SchoolClass targetClass = classRepository.findById(req.getTargetClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));
        Section targetSection = sectionRepository.findById(req.getTargetSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Target section not found"));

        List<StudentSummaryDto> promoted = new ArrayList<>();
        int rollCounter = (int) enrollmentRepository.countBySectionIdAndAcademicYearIdAndEnrollmentStatus(
                targetSection.getId(), toYear.getId(), "ACTIVE") + 1;

        for (Long studentId : req.getStudentIds()) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
            if (enrollmentRepository.existsByStudentIdAndAcademicYearId(studentId, toYear.getId())) {
                throw new ConflictException("Student " + student.getAdmissionNumber() + " already enrolled in " + toYear.getName());
            }
            StudentEnrollment prev = enrollmentRepository.findByStudentAndYear(studentId, req.getFromAcademicYearId())
                    .orElseThrow(() -> new BadRequestException("Student " + student.getAdmissionNumber() + " has no enrollment in source academic year"));

            prev.setEnrollmentStatus("PROMOTED");
            enrollmentRepository.save(prev);

            StudentEnrollment next = enrollmentRepository.save(StudentEnrollment.builder()
                    .student(student)
                    .academicYear(toYear)
                    .schoolClass(targetClass)
                    .section(targetSection)
                    .rollNumber(String.format("%02d", rollCounter++))
                    .enrollmentStatus("ACTIVE")
                    .promotedFromEnrollment(prev)
                    .remarks(req.getRemarks())
                    .enrolledAt(LocalDate.now())
                    .build());

            promoted.add(toSummary(next));
        }
        auditService.log("PROMOTE_STUDENTS", "STUDENT_ENROLLMENT", toYear.getId().toString(),
                "Promoted " + promoted.size() + " students to " + targetClass.getName() + "-" + targetSection.getName());
        return promoted;
    }

    @Transactional
    public DocumentDto uploadDocument(Long studentId, String documentType, String title, MultipartFile file) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        StorageService.StoredFile stored = storageService.store(file, "students/" + studentId);
        User uploader = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);

        StudentDocument doc = documentRepository.save(StudentDocument.builder()
                .student(student)
                .documentType(documentType)
                .title(title != null && !title.isBlank() ? title : stored.originalFileName())
                .fileName(stored.originalFileName())
                .storagePath(stored.storagePath())
                .contentType(stored.contentType())
                .fileSize(stored.size())
                .uploadedBy(uploader)
                .build());

        if ("PHOTO".equalsIgnoreCase(documentType)) {
            student.setPhotoUrl("/api/v1/students/" + studentId + "/documents/" + doc.getId() + "/download");
            studentRepository.save(student);
        }
        return DocumentDto.builder()
                .id(doc.getId()).documentType(doc.getDocumentType()).title(doc.getTitle())
                .fileName(doc.getFileName()).contentType(doc.getContentType())
                .fileSize(doc.getFileSize()).createdAt(doc.getCreatedAt()).build();
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(Long studentId, Long documentId) {
        accessControlService.assertCanAccessStudent(studentId);
        StudentDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!doc.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("Document does not belong to student");
        }
        Resource res = storageService.loadAsResource(doc.getStoragePath());
        return new DocumentDownload(res, doc.getFileName(), doc.getContentType());
    }

    public record DocumentDownload(Resource resource, String fileName, String contentType) {}

    @Transactional
    public RemarkDto addRemark(Long studentId, AddRemarkRequest req) {
        accessControlService.assertCanAccessStudent(studentId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year"));
        Teacher teacher = teacherRepository.findByUserId(SecurityUtils.getCurrentUserId()).orElse(null);

        StudentRemark saved = remarkRepository.save(StudentRemark.builder()
                .student(student)
                .teacher(teacher)
                .academicYear(ay)
                .remarkType(req.getRemarkType().toUpperCase())
                .remarkText(req.getRemarkText().trim())
                .build());

        return RemarkDto.builder()
                .id(saved.getId())
                .teacherName(teacher != null ? teacher.getFullName() : SecurityUtils.getCurrentUser().getFullName())
                .academicYearName(ay.getName())
                .remarkType(saved.getRemarkType())
                .remarkText(saved.getRemarkText())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private StudentSummaryDto toSummary(StudentEnrollment e) {
        Student s = e.getStudent();
        List<StudentParent> parents = studentParentRepository.findByStudentIdWithParent(s.getId());
        ParentProfile primary = parents.isEmpty() ? null : parents.get(0).getParent();
        return StudentSummaryDto.builder()
                .id(s.getId())
                .admissionNumber(s.getAdmissionNumber())
                .fullName(s.getFullName())
                .dateOfBirth(s.getDateOfBirth())
                .gender(s.getGender())
                .bloodGroup(s.getBloodGroup())
                .status(s.getStatus())
                .admissionDate(s.getAdmissionDate())
                .academicYearId(e.getAcademicYear().getId())
                .academicYearName(e.getAcademicYear().getName())
                .classId(e.getSchoolClass().getId())
                .className(e.getSchoolClass().getName())
                .sectionId(e.getSection().getId())
                .sectionName(e.getSection().getName())
                .rollNumber(e.getRollNumber())
                .primaryParentName(primary != null ? primary.getFullName() : null)
                .primaryParentPhone(primary != null ? primary.getPhone() : null)
                .build();
    }

    private StudentDetailDto buildDetailDto(Student s) {
        List<StudentEnrollment> history = enrollmentRepository.findHistoryByStudentId(s.getId());
        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);
        EnrollmentDto currentEn = history.stream()
                .filter(h -> currentYear != null && h.getAcademicYear().getId().equals(currentYear.getId()))
                .findFirst()
                .map(this::toEnrollmentDto)
                .orElse(history.isEmpty() ? null : toEnrollmentDto(history.get(0)));

        List<ParentLinkDto> parents = studentParentRepository.findByStudentIdWithParent(s.getId()).stream()
                .map(sp -> ParentLinkDto.builder()
                        .parentId(sp.getParent().getId())
                        .fullName(sp.getParent().getFullName())
                        .email(sp.getParent().getEmail())
                        .phone(sp.getParent().getPhone())
                        .occupation(sp.getParent().getOccupation())
                        .address(sp.getParent().getAddress())
                        .relationship(sp.getRelationship())
                        .isPrimary(sp.isPrimary())
                        .build())
                .toList();

        List<DocumentDto> docs = documentRepository.findByStudentIdOrderByCreatedAtDesc(s.getId()).stream()
                .map(d -> DocumentDto.builder()
                        .id(d.getId()).documentType(d.getDocumentType()).title(d.getTitle())
                        .fileName(d.getFileName()).contentType(d.getContentType())
                        .fileSize(d.getFileSize()).createdAt(d.getCreatedAt()).build())
                .toList();

        List<RemarkDto> remarks = remarkRepository.findByStudentIdWithDetails(s.getId()).stream()
                .map(r -> RemarkDto.builder()
                        .id(r.getId())
                        .teacherName(r.getTeacher() != null ? r.getTeacher().getFullName() : "Administration")
                        .academicYearName(r.getAcademicYear().getName())
                        .remarkType(r.getRemarkType())
                        .remarkText(r.getRemarkText())
                        .createdAt(r.getCreatedAt()).build())
                .toList();

        return StudentDetailDto.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .username(s.getUser().getUsername())
                .admissionNumber(s.getAdmissionNumber())
                .fullName(s.getFullName())
                .dateOfBirth(s.getDateOfBirth())
                .gender(s.getGender())
                .bloodGroup(s.getBloodGroup())
                .photoUrl(s.getPhotoUrl())
                .admissionDate(s.getAdmissionDate())
                .address(s.getAddress())
                .previousSchool(s.getPreviousSchool())
                .status(s.getStatus())
                .remarks(s.getRemarks())
                .currentEnrollment(currentEn)
                .enrollmentHistory(history.stream().map(this::toEnrollmentDto).toList())
                .parents(parents)
                .documents(docs)
                .teacherRemarks(remarks)
                .build();
    }

    private EnrollmentDto toEnrollmentDto(StudentEnrollment e) {
        return EnrollmentDto.builder()
                .id(e.getId())
                .academicYearId(e.getAcademicYear().getId())
                .academicYearName(e.getAcademicYear().getName())
                .classId(e.getSchoolClass().getId())
                .className(e.getSchoolClass().getName())
                .sectionId(e.getSection().getId())
                .sectionName(e.getSection().getName())
                .rollNumber(e.getRollNumber())
                .enrollmentStatus(e.getEnrollmentStatus())
                .enrolledAt(e.getEnrolledAt())
                .remarks(e.getRemarks())
                .build();
    }
}
