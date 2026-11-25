package com.tgs.school.fees.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SchoolClassRepository;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.fees.entity.FeeStructure;
import com.tgs.school.fees.entity.Payment;
import com.tgs.school.fees.entity.StudentFee;
import com.tgs.school.fees.repository.FeeStructureRepository;
import com.tgs.school.fees.repository.PaymentRepository;
import com.tgs.school.fees.repository.StudentFeeRepository;
import com.tgs.school.notifications.service.NotificationService;
import com.tgs.school.reports.service.PdfGeneratorService;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.entity.StudentParent;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentParentRepository;
import com.tgs.school.students.repository.StudentRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final PaymentRepository paymentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentParentRepository studentParentRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final PdfGeneratorService pdfGeneratorService;
    private final AuditService auditService;

    @Data
    public static class CreateFeeStructureRequest {
        private Long academicYearId;
        @NotNull private Long classId;
        @NotBlank private String name;
        @NotBlank private String feeType; // TUITION, ADMISSION, EXAMINATION, TRANSPORT, OTHER
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        private LocalDate dueDate;
        private String frequency = "ANNUAL";
        private String description;
    }

    @Data
    public static class AssignFeeRequest {
        @NotNull private Long feeStructureId;
        private List<Long> studentIds; // if empty/null, assign to all active students in the fee structure's class
        private BigDecimal concessionAmount = BigDecimal.ZERO;
        private BigDecimal scholarshipAmount = BigDecimal.ZERO;
        private String concessionReason;
    }

    @Data
    public static class UpdateConcessionRequest {
        @NotNull @DecimalMin("0.00") private BigDecimal concessionAmount;
        @NotNull @DecimalMin("0.00") private BigDecimal scholarshipAmount;
        private String concessionReason;
    }

    @Data
    public static class RecordPaymentRequest {
        @NotNull private Long studentFeeId;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        @NotBlank private String paymentMode; // CASH, UPI, BANK_TRANSFER, ONLINE_GATEWAY
        private String transactionReference;
        private String gatewayOrderId;
        private String gatewayPaymentId;
        private String gatewaySignature;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FeeStructureDto {
        private Long id;
        private Long academicYearId;
        private String academicYearName;
        private Long classId;
        private String className;
        private String name;
        private String feeType;
        private BigDecimal amount;
        private LocalDate dueDate;
        private String frequency;
        private String description;
        private boolean isActive;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentFeeDto {
        private Long id;
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private String className;
        private Long feeStructureId;
        private String feeName;
        private String feeType;
        private BigDecimal totalAmount;
        private BigDecimal concessionAmount;
        private BigDecimal scholarshipAmount;
        private BigDecimal netAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceAmount;
        private String status;
        private LocalDate dueDate;
        private String concessionReason;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentDto {
        private Long id;
        private Long studentFeeId;
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private String feeName;
        private String receiptNumber;
        private BigDecimal amount;
        private String paymentMode;
        private String paymentStatus;
        private String transactionReference;
        private boolean gatewayVerified;
        private Instant paidAt;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FeeReportSummaryDto {
        private BigDecimal totalCollected;
        private BigDecimal totalPending;
        private long paidCount;
        private long pendingCount;
        private long partialCount;
        private List<PaymentDto> recentPayments;
    }

    @Transactional
    public FeeStructureDto createFeeStructure(CreateFeeStructureRequest req) {
        AcademicYear ay = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId()).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : academicYearRepository.findByIsCurrentTrue().orElseThrow(() -> new ResourceNotFoundException("Current academic year not found"));
        SchoolClass sc = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        FeeStructure saved = feeStructureRepository.save(FeeStructure.builder()
                .academicYear(ay)
                .schoolClass(sc)
                .name(req.getName().trim())
                .feeType(req.getFeeType().toUpperCase())
                .amount(req.getAmount())
                .dueDate(req.getDueDate())
                .frequency(req.getFrequency() != null ? req.getFrequency().toUpperCase() : "ANNUAL")
                .description(req.getDescription())
                .build());
        auditService.log("CREATE_FEE_STRUCTURE", "FEE_STRUCTURE", saved.getId().toString(),
                sc.getName() + " - " + saved.getName() + " Rs." + saved.getAmount());
        return toStructureDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureDto> listFeeStructures(Long yearId, Long classId) {
        return feeStructureRepository.findFiltered(yearId, classId).stream().map(this::toStructureDto).toList();
    }

    @Transactional
    public List<StudentFeeDto> assignFee(AssignFeeRequest req) {
        FeeStructure fs = feeStructureRepository.findById(req.getFeeStructureId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));
        AcademicYear ay = fs.getAcademicYear();

        List<Student> targets = new ArrayList<>();
        if (req.getStudentIds() != null && !req.getStudentIds().isEmpty()) {
            for (Long sid : req.getStudentIds()) {
                targets.add(studentRepository.findById(sid).orElseThrow(() -> new ResourceNotFoundException("Student not found: " + sid)));
            }
        } else {
            enrollmentRepository.findActiveByYearAndClass(ay.getId(), fs.getSchoolClass().getId())
                    .forEach(en -> targets.add(en.getStudent()));
        }

        List<StudentFeeDto> assigned = new ArrayList<>();
        BigDecimal concession = req.getConcessionAmount() != null ? req.getConcessionAmount() : BigDecimal.ZERO;
        BigDecimal scholarship = req.getScholarshipAmount() != null ? req.getScholarshipAmount() : BigDecimal.ZERO;

        for (Student s : targets) {
            if (studentFeeRepository.findByStudentIdAndFeeStructureId(s.getId(), fs.getId()).isPresent()) {
                continue;
            }
            StudentEnrollment en = enrollmentRepository.findByStudentAndYear(s.getId(), ay.getId()).orElse(null);
            StudentFee sf = StudentFee.builder()
                    .student(s)
                    .enrollment(en)
                    .feeStructure(fs)
                    .academicYear(ay)
                    .totalAmount(fs.getAmount())
                    .concessionAmount(concession)
                    .scholarshipAmount(scholarship)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(fs.getDueDate())
                    .concessionReason(req.getConcessionReason())
                    .build();
            sf.recalculate();
            assigned.add(toStudentFeeDto(studentFeeRepository.save(sf)));
        }
        auditService.log("ASSIGN_FEE", "FEE_STRUCTURE", fs.getId().toString(), "Assigned to " + assigned.size() + " students");
        return assigned;
    }

    @Transactional
    public StudentFeeDto updateConcession(Long studentFeeId, UpdateConcessionRequest req) {
        StudentFee sf = studentFeeRepository.findById(studentFeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Student fee record not found"));
        sf.setConcessionAmount(req.getConcessionAmount());
        sf.setScholarshipAmount(req.getScholarshipAmount());
        sf.setConcessionReason(req.getConcessionReason());
        sf.recalculate();
        return toStudentFeeDto(studentFeeRepository.save(sf));
    }

    @Transactional
    public PaymentDto recordPayment(RecordPaymentRequest req) {
        StudentFee sf = studentFeeRepository.findById(req.getStudentFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Student fee record not found"));
        if (req.getAmount().compareTo(sf.getBalanceAmount()) > 0) {
            throw new BadRequestException("Payment amount exceeds remaining balance of Rs. " + sf.getBalanceAmount());
        }
        String mode = req.getPaymentMode().toUpperCase();
        boolean gatewayVerified = false;

        if ("ONLINE_GATEWAY".equals(mode)) {
            gatewayVerified = paymentGatewayService.verifyOnlinePayment(
                    req.getGatewayOrderId(), req.getGatewayPaymentId(), req.getGatewaySignature());
            if (!gatewayVerified) {
                throw new BadRequestException("Payment gateway signature verification failed. Payment cannot be marked successful.");
            }
        }

        User collector = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        String receiptNo = "TGS-RCP-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = paymentRepository.save(Payment.builder()
                .studentFee(sf)
                .student(sf.getStudent())
                .receiptNumber(receiptNo)
                .amount(req.getAmount())
                .paymentMode(mode)
                .paymentStatus("VERIFIED")
                .transactionReference(req.getTransactionReference())
                .gatewayOrderId(req.getGatewayOrderId())
                .gatewayPaymentId(req.getGatewayPaymentId())
                .gatewayVerified(gatewayVerified)
                .paidAt(Instant.now())
                .collectedBy(collector)
                .remarks(req.getRemarks())
                .build());

        sf.setPaidAmount(sf.getPaidAmount().add(req.getAmount()));
        sf.recalculate();
        studentFeeRepository.save(sf);

        auditService.log("RECORD_PAYMENT", "PAYMENT", payment.getId().toString(),
                "Receipt=" + receiptNo + ", Amount=Rs." + req.getAmount() + ", Mode=" + mode);
        return toPaymentDto(payment);
    }

    @Transactional(readOnly = true)
    public List<StudentFeeDto> getStudentFees(Long studentId) {
        accessControlService.assertCanAccessStudentAcademicsOrFees(studentId);
        return studentFeeRepository.findByStudentIdWithDetails(studentId).stream()
                .map(this::toStudentFeeDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getStudentPayments(Long studentId) {
        accessControlService.assertCanAccessStudentAcademicsOrFees(studentId);
        return paymentRepository.findByStudentIdWithDetails(studentId).stream()
                .map(this::toPaymentDto).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentFeeDto> listAllStudentFees(Long yearId, String status) {
        return studentFeeRepository.findFiltered(yearId, status).stream()
                .map(this::toStudentFeeDto).toList();
    }

    @Transactional(readOnly = true)
    public FeeReportSummaryDto getFeeReport(Long yearId) {
        Long resolvedYear = yearId != null ? yearId
                : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        List<StudentFee> all = studentFeeRepository.findFiltered(resolvedYear, null);
        return FeeReportSummaryDto.builder()
                .totalCollected(studentFeeRepository.sumTotalCollected(resolvedYear))
                .totalPending(studentFeeRepository.sumTotalPending(resolvedYear))
                .paidCount(all.stream().filter(f -> "PAID".equals(f.getStatus())).count())
                .pendingCount(all.stream().filter(f -> "PENDING".equals(f.getStatus()) || "OVERDUE".equals(f.getStatus())).count())
                .partialCount(all.stream().filter(f -> "PARTIAL".equals(f.getStatus())).count())
                .recentPayments(paymentRepository.findRecentPayments().stream().limit(25).map(this::toPaymentDto).toList())
                .build();
    }

    @Transactional
    public int sendPendingFeeReminders(Long yearId) {
        Long resolvedYear = yearId != null ? yearId
                : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        List<StudentFee> pending = studentFeeRepository.findFiltered(resolvedYear, null).stream()
                .filter(sf -> sf.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        int sent = 0;
        for (StudentFee sf : pending) {
            for (StudentParent sp : studentParentRepository.findByStudentIdWithParent(sf.getStudent().getId())) {
                notificationService.notifyUser(
                        sp.getParent().getUser(),
                        "Fee Payment Reminder: " + sf.getFeeStructure().getName(),
                        "Pending fee balance of Rs. " + sf.getBalanceAmount() + " for " + sf.getStudent().getFullName()
                                + (sf.getDueDate() != null ? " (Due: " + sf.getDueDate() + ")" : "") + ".",
                        "FEE_REMINDER",
                        "STUDENT_FEE",
                        sf.getId(),
                        true
                );
                sent++;
            }
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment receipt not found"));
        accessControlService.assertCanAccessStudentAcademicsOrFees(p.getStudent().getId());

        String dateStr = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(p.getPaidAt());

        return pdfGeneratorService.generateFeeReceiptPdf(new PdfGeneratorService.FeeReceiptData(
                p.getReceiptNumber(),
                dateStr,
                p.getStudent().getFullName(),
                p.getStudent().getAdmissionNumber(),
                p.getStudentFee().getFeeStructure().getSchoolClass().getName(),
                p.getStudentFee().getFeeStructure().getName(),
                p.getPaymentMode(),
                p.getTransactionReference() != null ? p.getTransactionReference() : "-",
                p.getAmount().toPlainString(),
                p.getStudentFee().getBalanceAmount().toPlainString()
        ));
    }

    private FeeStructureDto toStructureDto(FeeStructure fs) {
        return FeeStructureDto.builder()
                .id(fs.getId())
                .academicYearId(fs.getAcademicYear().getId())
                .academicYearName(fs.getAcademicYear().getName())
                .classId(fs.getSchoolClass().getId())
                .className(fs.getSchoolClass().getName())
                .name(fs.getName())
                .feeType(fs.getFeeType())
                .amount(fs.getAmount())
                .dueDate(fs.getDueDate())
                .frequency(fs.getFrequency())
                .description(fs.getDescription())
                .isActive(fs.isActive())
                .build();
    }

    private StudentFeeDto toStudentFeeDto(StudentFee sf) {
        return StudentFeeDto.builder()
                .id(sf.getId())
                .studentId(sf.getStudent().getId())
                .studentName(sf.getStudent().getFullName())
                .admissionNumber(sf.getStudent().getAdmissionNumber())
                .className(sf.getFeeStructure().getSchoolClass().getName())
                .feeStructureId(sf.getFeeStructure().getId())
                .feeName(sf.getFeeStructure().getName())
                .feeType(sf.getFeeStructure().getFeeType())
                .totalAmount(sf.getTotalAmount())
                .concessionAmount(sf.getConcessionAmount())
                .scholarshipAmount(sf.getScholarshipAmount())
                .netAmount(sf.getNetAmount())
                .paidAmount(sf.getPaidAmount())
                .balanceAmount(sf.getBalanceAmount())
                .status(sf.getStatus())
                .dueDate(sf.getDueDate())
                .concessionReason(sf.getConcessionReason())
                .build();
    }

    private PaymentDto toPaymentDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId())
                .studentFeeId(p.getStudentFee().getId())
                .studentId(p.getStudent().getId())
                .studentName(p.getStudent().getFullName())
                .admissionNumber(p.getStudent().getAdmissionNumber())
                .feeName(p.getStudentFee().getFeeStructure().getName())
                .receiptNumber(p.getReceiptNumber())
                .amount(p.getAmount())
                .paymentMode(p.getPaymentMode())
                .paymentStatus(p.getPaymentStatus())
                .transactionReference(p.getTransactionReference())
                .gatewayVerified(p.isGatewayVerified())
                .paidAt(p.getPaidAt())
                .remarks(p.getRemarks())
                .build();
    }
}
