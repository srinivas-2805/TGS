package com.tgs.school.transport.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.transport.entity.Bus;
import com.tgs.school.transport.entity.Driver;
import com.tgs.school.transport.service.TransportService;
import com.tgs.school.transport.service.TransportService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @GetMapping("/buses")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<Bus>>> listBuses() {
        return ResponseEntity.ok(ApiResponse.ok(transportService.listBuses()));
    }

    @PostMapping("/buses")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<Bus>> createBus(@Valid @RequestBody CreateBusRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Bus added", transportService.createBus(req)));
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<List<Driver>>> listDrivers() {
        return ResponseEntity.ok(ApiResponse.ok(transportService.listDrivers()));
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<Driver>> createDriver(@Valid @RequestBody CreateDriverRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Driver added", transportService.createDriver(req)));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<RouteDto>>> listRoutes() {
        return ResponseEntity.ok(ApiResponse.ok(transportService.listRoutes()));
    }

    @PostMapping("/routes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<RouteDto>> createRoute(@Valid @RequestBody CreateRouteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Route created", transportService.createRoute(req)));
    }

    @GetMapping("/allocations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<StudentTransportDto>>> listAllocations(@RequestParam(required = false) Long routeId) {
        return ResponseEntity.ok(ApiResponse.ok(transportService.listAllocations(routeId)));
    }

    @PostMapping("/allocations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<StudentTransportDto>> assign(@Valid @RequestBody AssignTransportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Student assigned to route", transportService.assignStudent(req)));
    }
}
