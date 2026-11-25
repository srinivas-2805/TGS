package com.tgs.school.transport.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.transport.entity.*;
import com.tgs.school.transport.repository.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportService {

    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final TransportRouteRepository routeRepository;
    private final StudentTransportRepository studentTransportRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;

    @Data
    public static class CreateBusRequest {
        @NotBlank private String busNumber;
        @NotBlank private String registrationNumber;
        @NotNull private Integer capacity;
        private String model;
        private LocalDate insuranceExpiry;
    }

    @Data
    public static class CreateDriverRequest {
        @NotBlank private String fullName;
        @NotBlank private String phone;
        @NotBlank private String licenseNumber;
        private LocalDate licenseExpiry;
        private String address;
    }

    @Data
    public static class CreateRouteRequest {
        @NotBlank private String routeName;
        @NotBlank private String routeCode;
        private Long busId;
        private Long driverId;
        @NotBlank private String pickupPointsJson;
        @NotNull private BigDecimal monthlyFee;
        private LocalTime morningStartTime;
        private LocalTime afternoonDepartureTime;
    }

    @Data
    public static class AssignTransportRequest {
        @NotNull private Long studentId;
        @NotNull private Long routeId;
        @NotBlank private String pickupPoint;
        @NotBlank private String dropPoint;
        private BigDecimal monthlyFee;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RouteDto {
        private Long id;
        private String routeName;
        private String routeCode;
        private Long busId;
        private String busNumber;
        private String registrationNumber;
        private Long driverId;
        private String driverName;
        private String driverPhone;
        private String pickupPointsJson;
        private BigDecimal monthlyFee;
        private LocalTime morningStartTime;
        private LocalTime afternoonDepartureTime;
        private boolean isActive;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentTransportDto {
        private Long id;
        private Long studentId;
        private String admissionNumber;
        private String studentName;
        private Long routeId;
        private String routeName;
        private String routeCode;
        private String busNumber;
        private String pickupPoint;
        private String dropPoint;
        private BigDecimal monthlyFee;
        private String status;
    }

    @Transactional
    public Bus createBus(CreateBusRequest req) {
        if (busRepository.existsByBusNumber(req.getBusNumber().trim())) {
            throw new ConflictException("Bus number already exists");
        }
        return busRepository.save(Bus.builder()
                .busNumber(req.getBusNumber().trim())
                .registrationNumber(req.getRegistrationNumber().trim().toUpperCase())
                .capacity(req.getCapacity())
                .model(req.getModel())
                .insuranceExpiry(req.getInsuranceExpiry())
                .build());
    }

    @Transactional(readOnly = true)
    public List<Bus> listBuses() {
        return busRepository.findAll();
    }

    @Transactional
    public Driver createDriver(CreateDriverRequest req) {
        if (driverRepository.existsByLicenseNumber(req.getLicenseNumber().trim())) {
            throw new ConflictException("Driver license number already exists");
        }
        return driverRepository.save(Driver.builder()
                .fullName(req.getFullName().trim())
                .phone(req.getPhone().trim())
                .licenseNumber(req.getLicenseNumber().trim().toUpperCase())
                .licenseExpiry(req.getLicenseExpiry())
                .address(req.getAddress())
                .build());
    }

    @Transactional(readOnly = true)
    public List<Driver> listDrivers() {
        return driverRepository.findAll();
    }

    @Transactional
    public RouteDto createRoute(CreateRouteRequest req) {
        Bus bus = req.getBusId() != null ? busRepository.findById(req.getBusId()).orElseThrow(() -> new ResourceNotFoundException("Bus not found")) : null;
        Driver driver = req.getDriverId() != null ? driverRepository.findById(req.getDriverId()).orElseThrow(() -> new ResourceNotFoundException("Driver not found")) : null;
        TransportRoute saved = routeRepository.save(TransportRoute.builder()
                .routeName(req.getRouteName().trim())
                .routeCode(req.getRouteCode().trim().toUpperCase())
                .bus(bus)
                .driver(driver)
                .pickupPointsJson(req.getPickupPointsJson())
                .monthlyFee(req.getMonthlyFee())
                .morningStartTime(req.getMorningStartTime())
                .afternoonDepartureTime(req.getAfternoonDepartureTime())
                .build());
        return toRouteDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RouteDto> listRoutes() {
        return routeRepository.findAllWithDetails().stream().map(this::toRouteDto).toList();
    }

    @Transactional
    public StudentTransportDto assignStudent(AssignTransportRequest req) {
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year"));
        Student student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        TransportRoute route = routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        StudentTransport st = studentTransportRepository.findByStudentIdAndAcademicYearId(student.getId(), ay.getId())
                .orElse(StudentTransport.builder().student(student).academicYear(ay).build());
        st.setRoute(route);
        st.setPickupPoint(req.getPickupPoint().trim());
        st.setDropPoint(req.getDropPoint().trim());
        st.setMonthlyFee(req.getMonthlyFee() != null ? req.getMonthlyFee() : route.getMonthlyFee());
        st.setStatus("ACTIVE");
        return toStudentTransportDto(studentTransportRepository.save(st));
    }

    @Transactional(readOnly = true)
    public List<StudentTransportDto> listAllocations(Long routeId) {
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue().orElse(null);
        return studentTransportRepository.findFiltered(routeId, ay != null ? ay.getId() : null)
                .stream().map(this::toStudentTransportDto).toList();
    }

    private RouteDto toRouteDto(TransportRoute r) {
        return RouteDto.builder()
                .id(r.getId())
                .routeName(r.getRouteName())
                .routeCode(r.getRouteCode())
                .busId(r.getBus() != null ? r.getBus().getId() : null)
                .busNumber(r.getBus() != null ? r.getBus().getBusNumber() : null)
                .registrationNumber(r.getBus() != null ? r.getBus().getRegistrationNumber() : null)
                .driverId(r.getDriver() != null ? r.getDriver().getId() : null)
                .driverName(r.getDriver() != null ? r.getDriver().getFullName() : null)
                .driverPhone(r.getDriver() != null ? r.getDriver().getPhone() : null)
                .pickupPointsJson(r.getPickupPointsJson())
                .monthlyFee(r.getMonthlyFee())
                .morningStartTime(r.getMorningStartTime())
                .afternoonDepartureTime(r.getAfternoonDepartureTime())
                .isActive(r.isActive())
                .build();
    }

    private StudentTransportDto toStudentTransportDto(StudentTransport st) {
        return StudentTransportDto.builder()
                .id(st.getId())
                .studentId(st.getStudent().getId())
                .admissionNumber(st.getStudent().getAdmissionNumber())
                .studentName(st.getStudent().getFullName())
                .routeId(st.getRoute().getId())
                .routeName(st.getRoute().getRouteName())
                .routeCode(st.getRoute().getRouteCode())
                .busNumber(st.getRoute().getBus() != null ? st.getRoute().getBus().getBusNumber() : null)
                .pickupPoint(st.getPickupPoint())
                .dropPoint(st.getDropPoint())
                .monthlyFee(st.getMonthlyFee())
                .status(st.getStatus())
                .build();
    }
}
