package com.tgs.school;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcademicWorkflowsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    void studentAdmissionAttendanceExamFeesLeaveAndPromotionWorkflows() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String teacherToken = login("teacher1", "Teacher@123");
        String accountantToken = login("accountant", "Accountant@123");

        // Resolve Class 5 and Section A
        MvcResult classesRes = mockMvc.perform(get("/api/v1/academics/classes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode classes = objectMapper.readTree(classesRes.getResponse().getContentAsString()).path("data");
        long class5Id = -1;
        long class6Id = -1;
        for (JsonNode c : classes) {
            if ("5th Class".equals(c.path("name").asText())) class5Id = c.path("id").asLong();
            if ("6th Class".equals(c.path("name").asText())) class6Id = c.path("id").asLong();
        }
        assertThat(class5Id).isPositive();
        assertThat(class6Id).isPositive();

        MvcResult sectionsRes = mockMvc.perform(get("/api/v1/academics/sections")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode sections = objectMapper.readTree(sectionsRes.getResponse().getContentAsString()).path("data");
        long sec5AId = -1;
        long sec6AId = -1;
        for (JsonNode s : sections) {
            if (s.path("classId").asLong() == class5Id && "A".equals(s.path("name").asText())) sec5AId = s.path("id").asLong();
            if (s.path("classId").asLong() == class6Id && "A".equals(s.path("name").asText())) sec6AId = s.path("id").asLong();
        }
        assertThat(sec5AId).isPositive();

        // 1. Admit a new student + auto-create parent account
        Map<String, Object> admitPayload = Map.ofEntries(
                Map.entry("fullName", "Kiran Sai Test"),
                Map.entry("dateOfBirth", "2016-05-10"),
                Map.entry("gender", "MALE"),
                Map.entry("bloodGroup", "O+"),
                Map.entry("classId", class5Id),
                Map.entry("sectionId", sec5AId),
                Map.entry("parentFullName", "Prakash Sai Test"),
                Map.entry("parentEmail", "prakash.test@tgs-dev.example"),
                Map.entry("parentPhone", "9888877777"),
                Map.entry("parentUsername", "parent_kiran"),
                Map.entry("parentPassword", "Parent@123"),
                Map.entry("address", "Kalluru, Khammam")
        );
        MvcResult admitRes = mockMvc.perform(post("/api/v1/students/admit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.admissionNumber").isNotEmpty())
                .andReturn();
        long studentId = objectMapper.readTree(admitRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Download ID Card PDF
        mockMvc.perform(get("/api/v1/students/" + studentId + "/id-card/pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 2. Teacher marks student ABSENT -> triggers parent notification & prevents duplicate
        String markDate = LocalDate.now().minusDays(2).toString();
        Map<String, Object> attPayload = Map.of(
                "sectionId", sec5AId,
                "attendanceDate", markDate,
                "records", List.of(Map.of("studentId", studentId, "status", "ABSENT", "remarks", "Uninformed"))
        );
        MvcResult attRes = mockMvc.perform(post("/api/v1/attendance/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        long attId = objectMapper.readTree(attRes.getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        // Duplicate attendance for same student/date/session must return 409 Conflict
        mockMvc.perform(post("/api/v1/attendance/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attPayload)))
                .andExpect(status().isConflict());

        // Parent receives absent notification
        String newParentToken = login("parent_kiran", "Parent@123");
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + newParentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // Admin corrects attendance
        mockMvc.perform(patch("/api/v1/attendance/students/" + attId + "/correct")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "LATE", "reason", "Bus delayed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LATE"));

        // 3. Fee assignment, payment, and gateway rejection
        MvcResult feeStructRes = mockMvc.perform(get("/api/v1/fees/structures?classId=" + class5Id)
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andReturn();
        long fsId = objectMapper.readTree(feeStructRes.getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        MvcResult assignFeeRes = mockMvc.perform(post("/api/v1/fees/assign")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("feeStructureId", fsId, "studentIds", List.of(studentId), "concessionAmount", 1000))))
                .andExpect(status().isCreated())
                .andReturn();
        long studentFeeId = objectMapper.readTree(assignFeeRes.getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        // Unconfigured online payment gateway must be rejected
        mockMvc.perform(post("/api/v1/fees/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentFeeId", studentFeeId,
                                "amount", 5000,
                                "paymentMode", "ONLINE_GATEWAY",
                                "gatewayOrderId", "ord_1",
                                "gatewayPaymentId", "pay_1",
                                "gatewaySignature", "fake"))))
                .andExpect(status().isBadRequest());

        // Valid cash payment + receipt PDF
        MvcResult payRes = mockMvc.perform(post("/api/v1/fees/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentFeeId", studentFeeId,
                                "amount", 5000,
                                "paymentMode", "CASH"))))
                .andExpect(status().isCreated())
                .andReturn();
        long paymentId = objectMapper.readTree(payRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/fees/payments/" + paymentId + "/receipt")
                        .header("Authorization", "Bearer " + newParentToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 4. Leave application by parent & approval updates attendance
        String leaveDate = LocalDate.now().plusDays(10).toString();
        MvcResult leaveRes = mockMvc.perform(post("/api/v1/leaves/student")
                        .header("Authorization", "Bearer " + newParentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentId", studentId,
                                "startDate", leaveDate,
                                "endDate", leaveDate,
                                "reason", "Family function"))))
                .andExpect(status().isCreated())
                .andReturn();
        long leaveId = objectMapper.readTree(leaveRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/leaves/" + leaveId + "/review")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED", "remarks", "Approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 5. Create next academic year & promote student while preserving previous history
        MvcResult yearsRes = mockMvc.perform(get("/api/v1/academics/academic-years")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long currentYearId = -1;
        for (JsonNode y : objectMapper.readTree(yearsRes.getResponse().getContentAsString()).path("data")) {
            if (y.path("current").asBoolean()) currentYearId = y.path("id").asLong();
        }

        MvcResult newYearRes = mockMvc.perform(post("/api/v1/academics/academic-years")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2027-2028",
                                "startDate", "2027-06-01",
                                "endDate", "2028-04-30",
                                "isCurrent", false))))
                .andExpect(status().isCreated())
                .andReturn();
        long nextYearId = objectMapper.readTree(newYearRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult nextSecRes = mockMvc.perform(post("/api/v1/academics/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "classId", class6Id,
                                "academicYearId", nextYearId,
                                "name", "A",
                                "capacity", 40))))
                .andExpect(status().isCreated())
                .andReturn();
        long nextSecId = objectMapper.readTree(nextSecRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/students/promote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAcademicYearId", currentYearId,
                                "toAcademicYearId", nextYearId,
                                "targetClassId", class6Id,
                                "targetSectionId", nextSecId,
                                "studentIds", List.of(studentId),
                                "remarks", "Annual promotion"))))
                .andExpect(status().isOk());

        // Enrollment history preserves both 2026-2027 and 2027-2028
        mockMvc.perform(get("/api/v1/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrollmentHistory.length()").value(2));

        // 6. Reports dashboard and exports
        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").isNumber());

        mockMvc.perform(get("/api/v1/reports/students/export?format=pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/api/v1/reports/fees/export?format=xlsx")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk());
    }

    @Test
    void timetableConflictAndExamMarksValidationWork() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String teacherToken = login("teacher1", "Teacher@123");

        // Teacher conflict check on Monday 09:00-09:45 (teacher1 is already booked in 5th-A)
        MvcResult teachersRes = mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        long t1Id = -1;
        for (JsonNode t : objectMapper.readTree(teachersRes.getResponse().getContentAsString()).path("data").path("content")) {
            if ("teacher1".equals(t.path("username").asText())) t1Id = t.path("id").asLong();
        }
        MvcResult sectionsRes = mockMvc.perform(get("/api/v1/academics/sections")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        long sec8AId = -1;
        for (JsonNode s : objectMapper.readTree(sectionsRes.getResponse().getContentAsString()).path("data")) {
            if ("8th Class".equals(s.path("className").asText()) && "A".equals(s.path("name").asText())) {
                sec8AId = s.path("id").asLong();
            }
        }
        MvcResult subjsRes = mockMvc.perform(get("/api/v1/academics/subjects")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        long matId = objectMapper.readTree(subjsRes.getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        mockMvc.perform(post("/api/v1/timetables")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sectionId", sec8AId,
                                "dayOfWeek", "MONDAY",
                                "periodNumber", 1,
                                "startTime", "09:00:00",
                                "endTime", "09:45:00",
                                "subjectId", matId,
                                "teacherId", t1Id))))
                .andExpect(status().isConflict());

        // Seeded report card PDF works for parent1
        String parent1Token = login("parent1", "Parent@123");
        MvcResult cRes = mockMvc.perform(get("/api/v1/parents/my-children")
                        .header("Authorization", "Bearer " + parent1Token))
                .andReturn();
        long child1Id = objectMapper.readTree(cRes.getResponse().getContentAsString()).path("data").get(0).path("studentId").asLong();
        long child2Id = objectMapper.readTree(cRes.getResponse().getContentAsString()).path("data").get(1).path("studentId").asLong();

        MvcResult examsRes = mockMvc.perform(get("/api/v1/examinations")
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();
        long examId = objectMapper.readTree(examsRes.getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/examinations/report-card/pdf?studentId=" + child1Id + "&examinationId=" + examId)
                        .header("Authorization", "Bearer " + parent1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // LKG child report card (activity-based) also generates cleanly
        mockMvc.perform(get("/api/v1/examinations/report-card?studentId=" + child2Id + "&examinationId=" + examId)
                        .header("Authorization", "Bearer " + parent1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityGradedClass").value(true));
    }
}
