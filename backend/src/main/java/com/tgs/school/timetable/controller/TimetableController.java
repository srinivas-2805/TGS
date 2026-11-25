package com.tgs.school.timetable.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.timetable.service.TimetableService;
import com.tgs.school.timetable.service.TimetableService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> sectionTimetable(
            @PathVariable Long sectionId,
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.ok(timetableService.getSectionTimetable(sectionId, academicYearId)));
    }

    @GetMapping("/teacher/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> myTeacherTimetable() {
        return ResponseEntity.ok(ApiResponse.ok(timetableService.getMyTeacherTimetable()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<TimetableSlotDto>> createSlot(@Valid @RequestBody SaveTimetableSlotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Timetable slot saved", timetableService.saveSlot(req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSlot(@PathVariable Long id) {
        timetableService.deleteSlot(id);
        return ResponseEntity.ok(ApiResponse.okMessage("Timetable slot deleted"));
    }

    @GetMapping("/section/{sectionId}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long sectionId) {
        byte[] pdf = timetableService.exportSectionTimetablePdf(sectionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=TGS-Timetable-" + sectionId + ".pdf")
                .body(pdf);
    }
}
