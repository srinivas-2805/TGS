package com.tgs.school.academics.service;

import com.tgs.school.academics.entity.*;
import com.tgs.school.academics.repository.*;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.repository.TeacherRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicService {

    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final AuditService auditService;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AcademicYearDto {
        private Long id;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean isCurrent;
        private String status;
    }

    @Data
    public static class CreateAcademicYearRequest {
        @NotBlank private String name;
        @NotNull private LocalDate startDate;
        @NotNull private LocalDate endDate;
        private boolean isCurrent;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SchoolClassDto {
        private Long id;
        private String name;
        private Integer levelOrder;
        private String category;
        private boolean isActivityGraded;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SectionDto {
        private Long id;
        private Long classId;
        private String className;
        private Integer levelOrder;
        private Long academicYearId;
        private String academicYearName;
        private String name;
        private Integer capacity;
        private String roomNumber;
        private Long classTeacherId;
        private String classTeacherName;
    }

    @Data
    public static class CreateSectionRequest {
        @NotNull private Long classId;
        private Long academicYearId;
        @NotBlank private String name;
        private Integer capacity = 40;
        private String roomNumber;
        private Long classTeacherId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectDto {
        private Long id;
        private String name;
        private String code;
        private String description;
        private boolean isActivityBased;
    }

    @Data
    public static class CreateSubjectRequest {
        @NotBlank private String name;
        @NotBlank private String code;
        private String description;
        private boolean isActivityBased;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassSubjectDto {
        private Long id;
        private Long classId;
        private String className;
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private Integer maxMarks;
        private Integer passingMarks;
        private boolean isActivityBased;
    }

    @Data
    public static class AssignClassSubjectRequest {
        @NotNull private Long classId;
        @NotNull private Long subjectId;
        private Integer maxMarks = 100;
        private Integer passingMarks = 35;
    }

    @Transactional(readOnly = true)
    public List<AcademicYearDto> listAcademicYears() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AcademicYear getCurrentAcademicYear() {
        return academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year configured"));
    }

    @Transactional
    public AcademicYearDto createAcademicYear(CreateAcademicYearRequest req) {
        if (academicYearRepository.findByName(req.getName().trim()).isPresent()) {
            throw new ConflictException("Academic year already exists: " + req.getName());
        }
        if (req.isCurrent()) {
            academicYearRepository.findAll().forEach(ay -> {
                if (ay.isCurrent()) {
                    ay.setCurrent(false);
                    academicYearRepository.save(ay);
                }
            });
        }
        AcademicYear saved = academicYearRepository.save(AcademicYear.builder()
                .name(req.getName().trim())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isCurrent(req.isCurrent())
                .status(req.isCurrent() ? "ACTIVE" : "UPCOMING")
                .build());
        auditService.log("CREATE_ACADEMIC_YEAR", "ACADEMIC_YEAR", saved.getId().toString(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public AcademicYearDto setCurrentAcademicYear(Long id) {
        AcademicYear target = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        academicYearRepository.findAll().forEach(ay -> {
            ay.setCurrent(ay.getId().equals(id));
            if (ay.getId().equals(id)) ay.setStatus("ACTIVE");
            academicYearRepository.save(ay);
        });
        auditService.log("SET_CURRENT_ACADEMIC_YEAR", "ACADEMIC_YEAR", id.toString(), target.getName());
        return toDto(target);
    }

    @Transactional(readOnly = true)
    public List<SchoolClassDto> listClasses() {
        return classRepository.findAllByOrderByLevelOrderAsc().stream()
                .map(c -> SchoolClassDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .levelOrder(c.getLevelOrder())
                        .category(c.getCategory())
                        .isActivityGraded(c.isActivityGraded())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SectionDto> listSections(Long yearId) {
        Long resolvedYearId = yearId != null ? yearId : getCurrentAcademicYear().getId();
        return sectionRepository.findByAcademicYearIdWithDetails(resolvedYearId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public SectionDto createSection(CreateSectionRequest req) {
        SchoolClass sc = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        AcademicYear ay = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId()).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : getCurrentAcademicYear();
        String secName = req.getName().trim().toUpperCase();
        if (sectionRepository.findBySchoolClassIdAndAcademicYearIdAndName(sc.getId(), ay.getId(), secName).isPresent()) {
            throw new ConflictException("Section " + secName + " already exists for " + sc.getName());
        }
        Teacher classTeacher = req.getClassTeacherId() != null
                ? teacherRepository.findById(req.getClassTeacherId()).orElseThrow(() -> new ResourceNotFoundException("Teacher not found"))
                : null;
        Section saved = sectionRepository.save(Section.builder()
                .schoolClass(sc)
                .academicYear(ay)
                .name(secName)
                .capacity(req.getCapacity() != null ? req.getCapacity() : 40)
                .roomNumber(req.getRoomNumber())
                .classTeacher(classTeacher)
                .build());
        auditService.log("CREATE_SECTION", "SECTION", saved.getId().toString(), sc.getName() + "-" + secName);
        return toDto(saved);
    }

    @Transactional
    public SectionDto updateSection(Long id, CreateSectionRequest req) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        if (req.getCapacity() != null) section.setCapacity(req.getCapacity());
        if (req.getRoomNumber() != null) section.setRoomNumber(req.getRoomNumber());
        if (req.getClassTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(req.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
            section.setClassTeacher(teacher);
        }
        return toDto(sectionRepository.save(section));
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> listSubjects() {
        return subjectRepository.findAll().stream()
                .map(s -> SubjectDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .code(s.getCode())
                        .description(s.getDescription())
                        .isActivityBased(s.isActivityBased())
                        .build())
                .toList();
    }

    @Transactional
    public SubjectDto createSubject(CreateSubjectRequest req) {
        String code = req.getCode().trim().toUpperCase();
        if (subjectRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Subject code already exists: " + code);
        }
        Subject saved = subjectRepository.save(Subject.builder()
                .name(req.getName().trim())
                .code(code)
                .description(req.getDescription())
                .isActivityBased(req.isActivityBased())
                .build());
        return SubjectDto.builder()
                .id(saved.getId()).name(saved.getName()).code(saved.getCode())
                .description(saved.getDescription()).isActivityBased(saved.isActivityBased()).build();
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectDto> listClassSubjects(Long classId) {
        return classSubjectRepository.findByClassIdWithDetails(classId).stream()
                .map(cs -> ClassSubjectDto.builder()
                        .id(cs.getId())
                        .classId(cs.getSchoolClass().getId())
                        .className(cs.getSchoolClass().getName())
                        .subjectId(cs.getSubject().getId())
                        .subjectName(cs.getSubject().getName())
                        .subjectCode(cs.getSubject().getCode())
                        .maxMarks(cs.getMaxMarks())
                        .passingMarks(cs.getPassingMarks())
                        .isActivityBased(cs.getSubject().isActivityBased())
                        .build())
                .toList();
    }

    @Transactional
    public ClassSubjectDto assignSubjectToClass(AssignClassSubjectRequest req) {
        SchoolClass sc = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Subject sub = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        ClassSubject cs = classSubjectRepository.findBySchoolClassIdAndSubjectId(sc.getId(), sub.getId())
                .orElse(ClassSubject.builder().schoolClass(sc).subject(sub).build());
        cs.setMaxMarks(req.getMaxMarks() != null ? req.getMaxMarks() : 100);
        cs.setPassingMarks(req.getPassingMarks() != null ? req.getPassingMarks() : 35);
        ClassSubject saved = classSubjectRepository.save(cs);
        return ClassSubjectDto.builder()
                .id(saved.getId()).classId(sc.getId()).className(sc.getName())
                .subjectId(sub.getId()).subjectName(sub.getName()).subjectCode(sub.getCode())
                .maxMarks(saved.getMaxMarks()).passingMarks(saved.getPassingMarks())
                .isActivityBased(sub.isActivityBased()).build();
    }

    private AcademicYearDto toDto(AcademicYear ay) {
        return AcademicYearDto.builder()
                .id(ay.getId()).name(ay.getName()).startDate(ay.getStartDate())
                .endDate(ay.getEndDate()).isCurrent(ay.isCurrent()).status(ay.getStatus()).build();
    }

    private SectionDto toDto(Section s) {
        return SectionDto.builder()
                .id(s.getId())
                .classId(s.getSchoolClass().getId())
                .className(s.getSchoolClass().getName())
                .levelOrder(s.getSchoolClass().getLevelOrder())
                .academicYearId(s.getAcademicYear().getId())
                .academicYearName(s.getAcademicYear().getName())
                .name(s.getName())
                .capacity(s.getCapacity())
                .roomNumber(s.getRoomNumber())
                .classTeacherId(s.getClassTeacher() != null ? s.getClassTeacher().getId() : null)
                .classTeacherName(s.getClassTeacher() != null ? s.getClassTeacher().getFullName() : null)
                .build();
    }
}
