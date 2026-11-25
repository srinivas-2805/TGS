package com.tgs.school.examinations.repository;

import com.tgs.school.examinations.entity.GradingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GradingRuleRepository extends JpaRepository<GradingRule, Long> {
    @Query("SELECT g FROM GradingRule g WHERE g.academicYear.id = :yearId OR g.academicYear IS NULL ORDER BY g.minPercentage DESC")
    List<GradingRule> findApplicableRules(@Param("yearId") Long yearId);
}
