package com.tgs.school.library.service;

import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.library.entity.Book;
import com.tgs.school.library.entity.BookIssue;
import com.tgs.school.library.repository.BookIssueRepository;
import com.tgs.school.library.repository.BookRepository;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.repository.TeacherRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository bookRepository;
    private final BookIssueRepository bookIssueRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    @Value("${app.library.late-fee-per-day:5}")
    private BigDecimal lateFeePerDay;

    @Data
    public static class CreateBookRequest {
        private String isbn;
        @NotBlank private String title;
        @NotBlank private String author;
        private String publisher;
        @NotBlank private String category;
        @NotNull @Min(1) private Integer totalCopies;
        private String shelfLocation;
    }

    @Data
    public static class IssueBookRequest {
        @NotNull private Long bookId;
        private Long studentId;
        private Long teacherId;
        @NotNull private LocalDate dueDate;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookIssueDto {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private String bookIsbn;
        private Long studentId;
        private String studentName;
        private Long teacherId;
        private String teacherName;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
        private BigDecimal lateFeeAmount;
        private boolean lateFeePaid;
        private String remarks;
    }

    @Transactional
    public Book addBook(CreateBookRequest req) {
        Book saved = bookRepository.save(Book.builder()
                .isbn(req.getIsbn())
                .title(req.getTitle().trim())
                .author(req.getAuthor().trim())
                .publisher(req.getPublisher())
                .category(req.getCategory().trim())
                .totalCopies(req.getTotalCopies())
                .availableCopies(req.getTotalCopies())
                .shelfLocation(req.getShelfLocation())
                .build());
        auditService.log("ADD_BOOK", "BOOK", saved.getId().toString(), saved.getTitle());
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<Book> searchBooks(String q, String category, Pageable pageable) {
        return PageResponse.from(bookRepository.search(q, category, pageable));
    }

    @Transactional
    public BookIssueDto issueBook(IssueBookRequest req) {
        if (req.getStudentId() == null && req.getTeacherId() == null) {
            throw new BadRequestException("Either studentId or teacherId is required");
        }
        Book book = bookRepository.findById(req.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (book.getAvailableCopies() <= 0) {
            throw new BadRequestException("No available copies for: " + book.getTitle());
        }
        Student student = req.getStudentId() != null
                ? studentRepository.findById(req.getStudentId()).orElseThrow(() -> new ResourceNotFoundException("Student not found"))
                : null;
        Teacher teacher = req.getTeacherId() != null
                ? teacherRepository.findById(req.getTeacherId()).orElseThrow(() -> new ResourceNotFoundException("Teacher not found"))
                : null;
        User issuer = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        BookIssue issue = bookIssueRepository.save(BookIssue.builder()
                .book(book)
                .student(student)
                .teacher(teacher)
                .issuedBy(issuer)
                .issueDate(LocalDate.now())
                .dueDate(req.getDueDate())
                .status("ISSUED")
                .remarks(req.getRemarks())
                .build());
        auditService.log("ISSUE_BOOK", "BOOK_ISSUE", issue.getId().toString(), book.getTitle());
        return toDto(issue);
    }

    @Transactional
    public BookIssueDto returnBook(Long issueId, boolean lateFeePaid) {
        BookIssue issue = bookIssueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Book issue record not found"));
        if ("RETURNED".equals(issue.getStatus())) {
            throw new BadRequestException("Book already returned");
        }
        LocalDate today = LocalDate.now();
        long overdueDays = ChronoUnit.DAYS.between(issue.getDueDate(), today);
        BigDecimal lateFee = overdueDays > 0
                ? lateFeePerDay.multiply(BigDecimal.valueOf(overdueDays))
                : BigDecimal.ZERO;

        issue.setReturnDate(today);
        issue.setStatus("RETURNED");
        issue.setLateFeeAmount(lateFee);
        issue.setLateFeePaid(lateFee.compareTo(BigDecimal.ZERO) == 0 || lateFeePaid);

        Book book = issue.getBook();
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        bookRepository.save(book);

        return toDto(bookIssueRepository.save(issue));
    }

    @Transactional(readOnly = true)
    public List<BookIssueDto> listIssues(String status, Long studentId) {
        if (studentId != null && !accessControlService.isSchoolWideStaff()) {
            accessControlService.assertCanAccessStudent(studentId);
        }
        return bookIssueRepository.findFiltered(status, studentId).stream().map(this::toDto).toList();
    }

    private BookIssueDto toDto(BookIssue bi) {
        return BookIssueDto.builder()
                .id(bi.getId())
                .bookId(bi.getBook().getId())
                .bookTitle(bi.getBook().getTitle())
                .bookIsbn(bi.getBook().getIsbn())
                .studentId(bi.getStudent() != null ? bi.getStudent().getId() : null)
                .studentName(bi.getStudent() != null ? bi.getStudent().getFullName() : null)
                .teacherId(bi.getTeacher() != null ? bi.getTeacher().getId() : null)
                .teacherName(bi.getTeacher() != null ? bi.getTeacher().getFullName() : null)
                .issueDate(bi.getIssueDate())
                .dueDate(bi.getDueDate())
                .returnDate(bi.getReturnDate())
                .status(bi.getStatus())
                .lateFeeAmount(bi.getLateFeeAmount())
                .lateFeePaid(bi.isLateFeePaid())
                .remarks(bi.getRemarks())
                .build();
    }
}
