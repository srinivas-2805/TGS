package com.tgs.school.transport.repository;

import com.tgs.school.transport.entity.TransportRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransportRouteRepository extends JpaRepository<TransportRoute, Long> {
    @Query("SELECT r FROM TransportRoute r LEFT JOIN FETCH r.bus LEFT JOIN FETCH r.driver ORDER BY r.routeCode")
    List<TransportRoute> findAllWithDetails();
}
