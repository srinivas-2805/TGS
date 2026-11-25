package com.tgs.school.timetable.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.Section;
import com.tgs.school.academics.entity.Subject;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.academics.repository.SubjectRepository;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.reports.service.PdfGeneratorService;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.repository.TeacherRepository;
import com.tgs.school.timetable.entity.TimetableEntry;
import com.tgs.school.timetable.repository.TimetableRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AccessControlService accessControlService;
    private final PdfGeneratorService pdfGeneratorService;

    @Data
    public static class SaveTimetableSlotRequest {
        private Long academicYearId;
        @NotNull private Long sectionId;
        @NotBlank private String dayOfWeek;
        @NotNull private Integer periodNumber;
        @NotNull private LocalTime startTime;
        @NotNull private LocalTime endTime;
        private boolean isBreak;
        private String breakLabel;
        private Long subjectId;
        private Long teacherId;
        private String roomNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimetableSlotDto {
        private Long id;
        private Long sectionId;
        private String className;
        private String sectionName;
        private String dayOfWeek;
        private Integer periodNumber;
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean isBreak;
        private String breakLabel;
        private Long subjectId;
        private String subjectName;
        private Long teacherId;
        private String teacherName;
        private String roomNumber;
    }

    @Transactional
    public TimetableSlotDto saveSlot(SaveTimetableSlotRequest req) {
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        AcademicYear ay = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId()).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : academicYearRepository.findByIsCurrentTrue().orElseThrow(() -> new ResourceNotFoundException("Current academic year not found"));
        Section section = sectionRepository.findById(req.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        String day = req.getDayOfWeek().toUpperCase();
        Teacher teacher = null;
        Subject subject = null;

        if (!req.isBreak()) {
            if (req.getSubjectId() == null || req.getTeacherId() == null) {
                throw new BadRequestException("Subject and Teacher are required for non-break periods");
            }
            subject = subjectRepository.findById(req.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
            teacher = teacherRepository.findById(req.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

            if (timetableRepository.existsTeacherConflict(ay.getId(), day, teacher.getId(), req.getStartTime(), req.getEndTime(), null)) {
                throw new ConflictException("Teacher " + teacher.getFullName() + " is already scheduled in another class during this slot.");
            }
            if (req.getRoomNumber() != null && !req.getRoomNumber().isBlank()
                    && timetableRepository.existsRoomConflict(ay.getId(), day, req.getRoomNumber().trim(), req.getStartTime(), req.getEndTime(), null)) {
                throw new ConflictException("Room " + req.getRoomNumber() + " is already booked during this time slot.");
            }
        }

        TimetableEntry saved = timetableRepository.save(TimetableEntry.builder()
                .academicYear(ay)
                .schoolClass(section.getSchoolClass())
                .section(section)
                .dayOfWeek(day)
                .periodNumber(req.getPeriodNumber())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .isBreak(req.isBreak())
                .breakLabel(req.getBreakLabel())
                .subject(subject)
                .teacher(teacher)
                .roomNumber(req.getRoomNumber())
                .build());

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getSectionTimetable(Long sectionId, Long yearId) {
        Long resolvedYear = yearId != null ? yearId
                : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("No active academic year"));
        return timetableRepository.findByYearAndSection(resolvedYear, sectionId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getMyTeacherTimetable() {
        Teacher teacher = accessControlService.requireCurrentTeacher();
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active academic year"));
        return timetableRepository.findByYearAndTeacher(ay.getId(), teacher.getId()).stream().map(this::toDto).toList();
    }

    @Transactional
    public void deleteSlot(Long id) {
        timetableRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public byte[] exportSectionTimetablePdf(Long sectionId) {
        Section sec = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        List<TimetableSlotDto> slots = getSectionTimetable(sectionId, null);
        List<List<String>> rows = slots.stream()
                .map(s -> List.of(
                        s.getDayOfWeek(),
                        "P" + s.getPeriodNumber(),
                        s.getStartTime() + "-" + s.getEndTime(),
                        s.isBreak() ? (s.getBreakLabel() != null ? s.getBreakLabel() : "BREAK") : s.getSubjectName(),
                        s.isBreak() ? "-" : s.getTeacherName(),
                        s.getRoomNumber() != null ? s.getRoomNumber() : "-"
                ))
                .toList();
        return pdfGeneratorService.generateSimpleTablePdf(
                "WEEKLY TIMETABLE",
                sec.getSchoolClass().getName() + " - Section " + sec.getName(),
                List.of("Day", "Period", "Time", "Subject", "Teacher", "Room"),
                rows);
    }

    private TimetableSlotDto toDto(TimetableEntry t) {
        return TimetableSlotDto.builder()
                .id(t.getId())
                .sectionId(t.getSection().getId())
                .className(t.getSchoolClass().getName())
                .sectionName(t.getSection().getName())
                .dayOfWeek(t.getDayOfWeek())
                .periodNumber(t.getPeriodNumber())
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .isBreak(t.isBreak())
                .breakLabel(t.getBreakLabel())
                .subjectId(t.getSubject() != null ? t.getSubject().getId() : null)
                .subjectName(t.getSubject() != null ? t.getSubject().getName() : null)
                .teacherId(t.getTeacher() != null ? t.getTeacher().getId() : null)
                .teacherName(t.getTeacher() != null ? t.getTeacher().getFullName() : null)
                .roomNumber(t.getRoomNumber())
                .build();
    }
}
