package com.tgs.school.fees.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.fees.service.FeeService;
import com.tgs.school.fees.service.FeeService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;
    private final com.tgs.school.fees.service.PaymentGatewayService paymentGatewayService;

    @GetMapping("/gateway-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> gatewayStatus() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "provider", paymentGatewayService.getProviderName(),
                "onlineGatewayConfigured", paymentGatewayService.isOnlineGatewayConfigured()
        )));
    }

    @GetMapping("/structures")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT','PARENT','STUDENT')")
    public ResponseEntity<ApiResponse<List<FeeStructureDto>>> listStructures(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(feeService.listFeeStructures(academicYearId, classId)));
    }

    @PostMapping("/structures")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FeeStructureDto>> createStructure(@Valid @RequestBody CreateFeeStructureRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fee structure created", feeService.createFeeStructure(req)));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<StudentFeeDto>>> assignFee(@Valid @RequestBody AssignFeeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fee assigned to students", feeService.assignFee(req)));
    }

    @PatchMapping("/student-fees/{id}/concession")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<StudentFeeDto>> updateConcession(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConcessionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Concession updated", feeService.updateConcession(id, req)));
    }

    @GetMapping("/student-fees")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<StudentFeeDto>>> listAllStudentFees(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(feeService.listAllStudentFees(academicYearId, status)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<StudentFeeDto>>> getStudentFees(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(feeService.getStudentFees(studentId)));
    }

    @GetMapping("/student/{studentId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getStudentPayments(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(feeService.getStudentPayments(studentId)));
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(@Valid @RequestBody RecordPaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment recorded", feeService.recordPayment(req)));
    }

    @GetMapping("/payments/{paymentId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId) {
        byte[] pdf = feeService.generateReceiptPdf(paymentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=TGS-Fee-Receipt-" + paymentId + ".pdf")
                .body(pdf);
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FeeReportSummaryDto>> getReport(@RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.ok(feeService.getFeeReport(academicYearId)));
    }

    @PostMapping("/reminders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sendReminders(@RequestParam(required = false) Long academicYearId) {
        int sent = feeService.sendPendingFeeReminders(academicYearId);
        return ResponseEntity.ok(ApiResponse.ok("Fee reminders dispatched", Map.of("notificationsSent", sent)));
    }
}
