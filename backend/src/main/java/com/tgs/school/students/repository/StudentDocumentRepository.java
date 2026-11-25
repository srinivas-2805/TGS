package com.tgs.school.students.repository;

import com.tgs.school.students.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {
    List<StudentDocument> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
