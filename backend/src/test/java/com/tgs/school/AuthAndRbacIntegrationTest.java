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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    void publicWebsiteAndHealthAreAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));

        mockMvc.perform(get("/api/v1/public/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.classes.length()").value(12));
    }

    @Test
    void loginRefreshAndLogoutFlowWorks() throws Exception {
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "Admin@123"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("data");
        String accessToken = data.path("accessToken").asText();
        String refreshToken = data.path("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // Access /me
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        // Refresh rotates token
        MvcResult refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        String newRefresh = objectMapper.readTree(refreshRes.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        // Old refresh token is revoked
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isBadRequest());

        // Logout revokes tokens
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", newRefresh))))
                .andExpect(status().isOk());
    }

    @Test
    void rbacIsolatesParentsAccountantsAndTeachers() throws Exception {
        String parent1Token = loginAndGetToken("parent1", "Parent@123");
        String parent2Token = loginAndGetToken("parent2", "Parent@123");
        String accountantToken = loginAndGetToken("accountant", "Accountant@123");
        String teacher1Token = loginAndGetToken("teacher1", "Teacher@123");

        // Parent1 has 2 children (multi-child support)
        MvcResult children1 = mockMvc.perform(get("/api/v1/parents/my-children")
                        .header("Authorization", "Bearer " + parent1Token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode c1Arr = objectMapper.readTree(children1.getResponse().getContentAsString()).path("data");
        assertThat(c1Arr.size()).isEqualTo(2);
        long parent1ChildId = c1Arr.get(0).path("studentId").asLong();

        // Parent2 cannot view Parent1's child profile, fees, or attendance
        mockMvc.perform(get("/api/v1/students/" + parent1ChildId)
                        .header("Authorization", "Bearer " + parent2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/fees/student/" + parent1ChildId)
                        .header("Authorization", "Bearer " + parent2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/attendance/student/" + parent1ChildId)
                        .header("Authorization", "Bearer " + parent2Token))
                .andExpect(status().isForbidden());

        // Accountant can view fees but cannot admit students or modify CMS
        mockMvc.perform(get("/api/v1/fees/report")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cms/content")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isForbidden());

        // Teacher cannot access CMS or approve teacher leave
        mockMvc.perform(get("/api/v1/cms/content")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isForbidden());
    }
}
