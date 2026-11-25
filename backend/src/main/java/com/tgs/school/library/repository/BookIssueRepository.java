package com.tgs.school.library.repository;

import com.tgs.school.library.entity.BookIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {
    @Query("SELECT bi FROM BookIssue bi JOIN FETCH bi.book LEFT JOIN FETCH bi.student LEFT JOIN FETCH bi.teacher " +
           "WHERE (:status IS NULL OR bi.status = :status) AND (:studentId IS NULL OR bi.student.id = :studentId) " +
           "ORDER BY bi.issueDate DESC, bi.id DESC")
    List<BookIssue> findFiltered(@Param("status") String status, @Param("studentId") Long studentId);

    long countByStatus(String status);
}
