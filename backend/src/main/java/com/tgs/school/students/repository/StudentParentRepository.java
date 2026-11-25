package com.tgs.school.students.repository;

import com.tgs.school.students.entity.StudentParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {
    @Query("SELECT sp FROM StudentParent sp JOIN FETCH sp.parent p JOIN FETCH p.user WHERE sp.student.id = :studentId")
    List<StudentParent> findByStudentIdWithParent(@Param("studentId") Long studentId);

    @Query("SELECT sp FROM StudentParent sp JOIN FETCH sp.student s WHERE sp.parent.id = :parentId AND s.deletedAt IS NULL")
    List<StudentParent> findByParentIdWithStudent(@Param("parentId") Long parentId);

    boolean existsByStudentIdAndParentUserId(Long studentId, Long userId);
}
