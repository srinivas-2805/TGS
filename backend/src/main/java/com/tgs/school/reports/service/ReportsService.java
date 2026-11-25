package com.tgs.school.reports.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SchoolClassRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.attendance.entity.Attendance;
import com.tgs.school.attendance.entity.TeacherAttendance;
import com.tgs.school.attendance.repository.AttendanceRepository;
import com.tgs.school.attendance.repository.TeacherAttendanceRepository;
import com.tgs.school.communication.repository.AnnouncementRepository;
import com.tgs.school.examinations.entity.Examination;
import com.tgs.school.examinations.entity.Mark;
import com.tgs.school.examinations.repository.ExaminationRepository;
import com.tgs.school.examinations.repository.MarkRepository;
import com.tgs.school.fees.entity.Payment;
import com.tgs.school.fees.entity.StudentFee;
import com.tgs.school.fees.repository.PaymentRepository;
import com.tgs.school.fees.repository.StudentFeeRepository;
import com.tgs.school.library.entity.Book;
import com.tgs.school.library.repository.BookIssueRepository;
import com.tgs.school.library.repository.BookRepository;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.teachers.repository.StaffRepository;
import com.tgs.school.teachers.repository.TeacherRepository;
import com.tgs.school.transport.entity.StudentTransport;
import com.tgs.school.transport.repository.StudentTransportRepository;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final PaymentRepository paymentRepository;
    private final ExaminationRepository examinationRepository;
    private final MarkRepository markRepository;
    private final AnnouncementRepository announcementRepository;
    private final BookRepository bookRepository;
    private final BookIssueRepository bookIssueRepository;
    private final StudentTransportRepository studentTransportRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final ExcelExportService excelExportService;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardSummaryDto {
        private long totalStudents;
        private long totalTeachers;
        private long totalStaff;
        private long totalClasses;
        private long totalSections;
        private long todayPresent;
        private long todayAbsent;
        private long todayLate;
        private long todayHalfDay;
        private long todayLeave;
        private long todayMarked;
        private BigDecimal totalFeeCollected;
        private BigDecimal pendingFees;
        private long upcomingExaminations;
        private long totalLibraryBooks;
        private long issuedBooksCount;
        private List<RecentAdmissionItem> recentAdmissions;
        private List<RecentAnnouncementItem> recentAnnouncements;
        private List<UpcomingExamItem> upcomingExamsList;
        private List<ChartPoint> classWiseDistribution;
        private List<AttendanceTrendPoint> monthlyAttendance;
        private List<FeeTrendPoint> monthlyFeeCollection;
        private List<ExamPerformancePoint> examPerformance;
        private List<ChartPoint> admissionStatistics;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentAdmissionItem {
        private Long id;
        private String admissionNumber;
        private String fullName;
        private LocalDate admissionDate;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentAnnouncementItem {
        private Long id;
        private String title;
        private String category;
        private String targetAudience;
        private Instant publishedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpcomingExamItem {
        private Long id;
        private String name;
        private String examType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChartPoint {
        private String label;
        private long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceTrendPoint {
        private String month;
        private long present;
        private long absent;
        private double percentage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FeeTrendPoint {
        private String month;
        private BigDecimal collected;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExamPerformancePoint {
        private String subject;
        private double averagePercentage;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(Long academicYearId) {
        AcademicYear year = academicYearId != null
                ? academicYearRepository.findById(academicYearId).orElse(null)
                : academicYearRepository.findByIsCurrentTrue().orElse(null);
        Long yearId = year != null ? year.getId() : null;

        LocalDate today = LocalDate.now();
        long present = attendanceRepository.countByAttendanceDateAndStatus(today, "PRESENT");
        long absent = attendanceRepository.countByAttendanceDateAndStatus(today, "ABSENT");
        long late = attendanceRepository.countByAttendanceDateAndStatus(today, "LATE");
        long halfDay = attendanceRepository.countByAttendanceDateAndStatus(today, "HALF_DAY");
        long leave = attendanceRepository.countByAttendanceDateAndStatus(today, "APPROVED_LEAVE");
        long marked = attendanceRepository.countByAttendanceDate(today);

        BigDecimal collected = studentFeeRepository.sumTotalCollected(yearId);
        BigDecimal pending = studentFeeRepository.sumTotalPending(yearId);

        List<RecentAdmissionItem> recentAdmissions = studentRepository.findTop5ByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(s -> RecentAdmissionItem.builder()
                        .id(s.getId())
                        .admissionNumber(s.getAdmissionNumber())
                        .fullName(s.getFullName())
                        .admissionDate(s.getAdmissionDate())
                        .status(s.getStatus())
                        .build())
                .toList();

        List<RecentAnnouncementItem> recentAnnouncements = announcementRepository.findAllWithDetails()
                .stream()
                .limit(5)
                .map(a -> RecentAnnouncementItem.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .category(a.getCategory())
                        .targetAudience(a.getTargetAudience())
                        .publishedAt(a.getPublishedAt())
                        .build())
                .toList();

        List<Examination> allExams = examinationRepository.findByYear(yearId);
        List<UpcomingExamItem> upcomingExams = allExams.stream()
                .filter(e -> !"COMPLETED".equalsIgnoreCase(e.getStatus()))
                .limit(5)
                .map(e -> UpcomingExamItem.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .examType(e.getExamType())
                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .status(e.getStatus())
                        .build())
                .toList();

        List<ChartPoint> classDistribution = yearId != null
                ? enrollmentRepository.countStudentsByClassForYear(yearId).stream()
                        .map(row -> new ChartPoint((String) row[0], ((Number) row[1]).longValue()))
                        .toList()
                : List.of();

        // Monthly attendance trend (last 6 months)
        List<AttendanceTrendPoint> monthlyAttendance = new ArrayList<>();
        if (yearId != null) {
            LocalDate sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1);
            List<Attendance> attRecords = attendanceRepository.findReportRecords(yearId, null, null, sixMonthsAgo, today);
            Map<YearMonth, long[]> attByMonth = new LinkedHashMap<>();
            for (int i = 5; i >= 0; i--) {
                attByMonth.put(YearMonth.from(today.minusMonths(i)), new long[]{0, 0});
            }
            for (Attendance a : attRecords) {
                YearMonth ym = YearMonth.from(a.getAttendanceDate());
                long[] counts = attByMonth.get(ym);
                if (counts != null) {
                    if ("ABSENT".equals(a.getStatus())) {
                        counts[1]++;
                    } else {
                        counts[0]++;
                    }
                }
            }
            attByMonth.forEach((ym, counts) -> {
                long tot = counts[0] + counts[1];
                double pct = tot > 0 ? Math.round((counts[0] * 1000.0) / tot) / 10.0 : 0.0;
                monthlyAttendance.add(new AttendanceTrendPoint(
                        ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        counts[0], counts[1], pct));
            });
        }

        // Monthly fee collection (last 6 months)
        Map<YearMonth, BigDecimal> feeByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            feeByMonth.put(YearMonth.from(today.minusMonths(i)), BigDecimal.ZERO);
        }
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        for (Payment p : paymentRepository.findRecentPayments()) {
            YearMonth ym = YearMonth.from(p.getPaidAt().atZone(zone));
            if (feeByMonth.containsKey(ym)) {
                feeByMonth.put(ym, feeByMonth.get(ym).add(p.getAmount()));
            }
        }
        List<FeeTrendPoint> monthlyFeeCollection = feeByMonth.entrySet().stream()
                .map(e -> new FeeTrendPoint(e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), e.getValue()))
                .toList();

        // Exam performance summary (latest exam with marks)
        List<ExamPerformancePoint> examPerf = new ArrayList<>();
        if (!allExams.isEmpty()) {
            Examination latestExam = allExams.get(0);
            Map<String, List<Double>> subjectScores = new LinkedHashMap<>();
            for (com.tgs.school.academics.entity.SchoolClass esClass : classRepository.findAllByOrderByLevelOrderAsc()) {
                for (Mark m : markRepository.findByExamAndClass(latestExam.getId(), esClass.getId())) {
                    if (!m.getExaminationSubject().isActivityBased()
                            && !m.isAbsent()
                            && m.getMarksObtained() != null
                            && m.getExaminationSubject().getMaxMarks().compareTo(BigDecimal.ZERO) > 0) {
                        double pct = m.getMarksObtained()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(m.getExaminationSubject().getMaxMarks(), 2, RoundingMode.HALF_UP)
                                .doubleValue();
                        subjectScores.computeIfAbsent(m.getExaminationSubject().getSubject().getName(), k -> new ArrayList<>()).add(pct);
                    }
                }
            }
            subjectScores.forEach((subj, vals) -> {
                double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                examPerf.add(new ExamPerformancePoint(subj, Math.round(avg * 10.0) / 10.0));
            });
        }

        // Admission statistics by month (last 6 months)
        Map<YearMonth, Long> admByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            admByMonth.put(YearMonth.from(today.minusMonths(i)), 0L);
        }
        for (Student s : studentRepository.findAll()) {
            if (s.getDeletedAt() == null && s.getAdmissionDate() != null) {
                YearMonth ym = YearMonth.from(s.getAdmissionDate());
                if (admByMonth.containsKey(ym)) {
                    admByMonth.put(ym, admByMonth.get(ym) + 1);
                }
            }
        }
        List<ChartPoint> admissionStats = admByMonth.entrySet().stream()
                .map(e -> new ChartPoint(e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), e.getValue()))
                .toList();

        return DashboardSummaryDto.builder()
                .totalStudents(studentRepository.countByDeletedAtIsNullAndStatus("ACTIVE"))
                .totalTeachers(teacherRepository.countByDeletedAtIsNull())
                .totalStaff(staffRepository.countByDeletedAtIsNull())
                .totalClasses(classRepository.count())
                .totalSections(yearId != null ? sectionRepository.findByAcademicYearIdWithDetails(yearId).size() : sectionRepository.count())
                .todayPresent(present)
                .todayAbsent(absent)
                .todayLate(late)
                .todayHalfDay(halfDay)
                .todayLeave(leave)
                .todayMarked(marked)
                .totalFeeCollected(collected != null ? collected : BigDecimal.ZERO)
                .pendingFees(pending != null ? pending : BigDecimal.ZERO)
                .upcomingExaminations(examinationRepository.countByStatusIn(List.of("SCHEDULED", "ONGOING")))
                .totalLibraryBooks(bookRepository.count())
                .issuedBooksCount(bookIssueRepository.countByStatus("ISSUED"))
                .recentAdmissions(recentAdmissions)
                .recentAnnouncements(recentAnnouncements)
                .upcomingExamsList(upcomingExams)
                .classWiseDistribution(classDistribution)
                .monthlyAttendance(monthlyAttendance)
                .monthlyFeeCollection(monthlyFeeCollection)
                .examPerformance(examPerf)
                .admissionStatistics(admissionStats)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportStudentsReport(String format, Long yearId, Long classId, Long sectionId) {
        Long resolvedYear = yearId != null ? yearId : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        List<StudentEnrollment> list = enrollmentRepository.searchEnrollments(
                resolvedYear, classId, sectionId, "ACTIVE", null, PageRequest.of(0, 1000)).getContent();
        List<String> headers = List.of("Admission No", "Student Name", "Class", "Section", "Roll No", "Gender", "DOB", "Status");
        List<List<String>> rows = list.stream().map(e -> List.of(
                e.getStudent().getAdmissionNumber(),
                e.getStudent().getFullName(),
                e.getSchoolClass().getName(),
                e.getSection().getName(),
                e.getRollNumber() != null ? e.getRollNumber() : "-",
                e.getStudent().getGender(),
                e.getStudent().getDateOfBirth().toString(),
                e.getStudent().getStatus()
        )).toList();
        return export("Student Strength Report", "Active Students Directory", "Students", headers, rows, format);
    }

    @Transactional(readOnly = true)
    public byte[] exportAttendanceReport(String format, Long yearId, Long classId, Long sectionId, LocalDate from, LocalDate to) {
        Long resolvedYear = yearId != null ? yearId : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        List<Attendance> records = resolvedYear != null
                ? attendanceRepository.findReportRecords(resolvedYear, classId, sectionId, start, end)
                : List.of();
        List<String> headers = List.of("Date", "Admission No", "Student", "Class", "Section", "Session", "Status");
        List<List<String>> rows = records.stream().map(a -> List.of(
                a.getAttendanceDate().toString(),
                a.getStudent().getAdmissionNumber(),
                a.getStudent().getFullName(),
                a.getSchoolClass().getName(),
                a.getSection().getName(),
                a.getSession(),
                a.getStatus()
        )).toList();
        return export("Student Attendance Report", "Period: " + start + " to " + end, "Attendance", headers, rows, format);
    }

    @Transactional(readOnly = true)
    public byte[] exportTeacherAttendanceReport(String format, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        List<TeacherAttendance> list = teacherAttendanceRepository.findBetween(start, end);
        List<String> headers = List.of("Date", "Emp Code", "Teacher Name", "Status", "Remarks");
        List<List<String>> rows = list.stream().map(ta -> List.of(
                ta.getAttendanceDate().toString(),
                ta.getTeacher().getEmployeeCode(),
                ta.getTeacher().getFullName(),
                ta.getStatus(),
                ta.getRemarks() != null ? ta.getRemarks() : "-"
        )).toList();
        return export("Teacher Attendance Report", "Period: " + start + " to " + end, "TeacherAttendance", headers, rows, format);
    }

    @Transactional(readOnly = true)
    public byte[] exportFeeReport(String format, Long yearId, String status) {
        Long resolvedYear = yearId != null ? yearId : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        List<StudentFee> list = studentFeeRepository.findFiltered(resolvedYear, status);
        List<String> headers = List.of("Admission No", "Student", "Class", "Fee Name", "Net (Rs)", "Paid (Rs)", "Balance (Rs)", "Status");
        List<List<String>> rows = list.stream().map(sf -> List.of(
                sf.getStudent().getAdmissionNumber(),
                sf.getStudent().getFullName(),
                sf.getFeeStructure().getSchoolClass().getName(),
                sf.getFeeStructure().getName(),
                sf.getNetAmount().toPlainString(),
                sf.getPaidAmount().toPlainString(),
                sf.getBalanceAmount().toPlainString(),
                sf.getStatus()
        )).toList();
        return export("Fee Collection & Dues Report", "Telangana Grammar School Finance", "Fees", headers, rows, format);
    }

    @Transactional(readOnly = true)
    public byte[] exportExamReport(String format, Long examId, Long classId) {
        List<Mark> list = (examId != null && classId != null)
                ? markRepository.findByExamAndClass(examId, classId)
                : List.of();
        List<String> headers = List.of("Admission No", "Student", "Subject", "Max", "Obtained", "Activity/Grade", "Remarks");
        List<List<String>> rows = list.stream().map(m -> List.of(
                m.getStudent().getAdmissionNumber(),
                m.getStudent().getFullName(),
                m.getExaminationSubject().getSubject().getName(),
                m.getExaminationSubject().getMaxMarks().toPlainString(),
                m.isAbsent() ? "AB" : (m.getMarksObtained() != null ? m.getMarksObtained().toPlainString() : "-"),
                m.getGrade() != null ? m.getGrade() : (m.getActivityRating() != null ? m.getActivityRating() : "-"),
                m.getDescriptiveRemarks() != null ? m.getDescriptiveRemarks() : "-"
        )).toList();
        return export("Examination Performance Report", "Exam ID: " + examId + " | Class ID: " + classId, "ExamMarks", headers, rows, format);
    }

    @Transactional(readOnly = true)
    public byte[] exportLibraryReport(String format) {
        List<Book> books = bookRepository.findAll();
        List<String> headers = List.of("ISBN", "Title", "Author", "Category", "Total", "Available", "Shelf");
        List<List<String>> rows = books.stream().map(b -> List.of(
                b.getIsbn() != null ? b.getIsbn() : "-",
                b.getTitle(),
                b.getAuthor(),
                b.getCategory(),
                String.valueOf(b.getTotalCopies()),
                String.valueOf(b.getAvailableCopies()),
                b.getShelfLocation() != null ? b.getShelfLocation() : "-"
        )).toList();
        return export("Library Inventory Report", "Catalog & Availability", "Library", headers, rows, format);
    }

    @Transactional(readOnly = true)
    public byte[] exportTransportReport(String format, Long routeId) {
        Long currentYearId = academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        List<StudentTransport> list = studentTransportRepository.findFiltered(routeId, currentYearId);
        List<String> headers = List.of("Admission No", "Student", "Route", "Bus", "Pickup", "Drop", "Monthly Fee");
        List<List<String>> rows = list.stream().map(st -> List.of(
                st.getStudent().getAdmissionNumber(),
                st.getStudent().getFullName(),
                st.getRoute().getRouteName(),
                st.getRoute().getBus() != null ? st.getRoute().getBus().getBusNumber() : "-",
                st.getPickupPoint(),
                st.getDropPoint(),
                st.getMonthlyFee().toPlainString()
        )).toList();
        return export("Transport Allocation Report", "Student Route Assignments", "Transport", headers, rows, format);
    }

    private byte[] export(String title, String subtitle, String sheetName, List<String> headers, List<List<String>> rows, String format) {
        if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            return excelExportService.exportToExcel(sheetName, headers, rows);
        }
        return pdfGeneratorService.generateSimpleTablePdf(title, subtitle, headers, rows);
    }
}
