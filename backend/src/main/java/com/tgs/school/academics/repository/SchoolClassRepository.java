package com.tgs.school.academics.repository;

import com.tgs.school.academics.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findAllByOrderByLevelOrderAsc();
    Optional<SchoolClass> findByName(String name);
    Optional<SchoolClass> findByLevelOrder(Integer levelOrder);
}
