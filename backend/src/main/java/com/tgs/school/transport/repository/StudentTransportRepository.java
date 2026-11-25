package com.tgs.school.transport.repository;

import com.tgs.school.transport.entity.StudentTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentTransportRepository extends JpaRepository<StudentTransport, Long> {
    @Query("SELECT st FROM StudentTransport st JOIN FETCH st.student JOIN FETCH st.route r LEFT JOIN FETCH r.bus " +
           "WHERE (:routeId IS NULL OR r.id = :routeId) AND (:yearId IS NULL OR st.academicYear.id = :yearId) ORDER BY st.student.fullName")
    List<StudentTransport> findFiltered(@Param("routeId") Long routeId, @Param("yearId") Long yearId);

    Optional<StudentTransport> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
}
