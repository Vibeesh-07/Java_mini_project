package com.refind.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/reports is publicly accessible and returns paged response")
    void testPublicSearchReports() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("GET /api/statistics/public returns calculated metrics from database")
    void testPublicStatistics() throws Exception {
        mockMvc.perform(get("/api/statistics/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLostOpen").isNumber())
                .andExpect(jsonPath("$.totalFoundOpen").isNumber())
                .andExpect(jsonPath("$.totalResolved").isNumber())
                .andExpect(jsonPath("$.totalPendingClaims").isNumber());
    }

    @Test
    @DisplayName("POST /api/reports without authentication returns 401 Unauthorized when CSRF token is provided")
    void testCreateReportUnauthorized() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lost Key\",\"description\":\"Desk\",\"categoryId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/reports without CSRF token returns 403 Forbidden")
    void testCreateReportWithoutCsrfFails() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lost Key\",\"description\":\"Desk\",\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard without authentication returns 401 Unauthorized")
    void testAdminEndpointUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@test.campus", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/dashboard with ROLE_ADMIN returns 200 OK with metrics")
    void testAdminEndpointWithRoleAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalReports").isNumber());
    }

    @Test
    @WithMockUser(username = "user@test.campus", roles = {"USER"})
    @DisplayName("GET /api/admin/dashboard with normal ROLE_USER returns 403 Forbidden")
    void testAdminEndpointForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reports/9999/image returns 404 Not Found")
    void testGetReportImageNotFound() throws Exception {
        mockMvc.perform(get("/api/reports/9999/image"))
                .andExpect(status().isNotFound());
    }
}
