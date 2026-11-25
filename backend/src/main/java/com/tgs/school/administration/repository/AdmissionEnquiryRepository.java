package com.tgs.school.administration.repository;

import com.tgs.school.administration.entity.AdmissionEnquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionEnquiryRepository extends JpaRepository<AdmissionEnquiry, Long> {
    Page<AdmissionEnquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
