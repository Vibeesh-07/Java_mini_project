package com.refind.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refind.dto.auth.LoginRequest;
import com.refind.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register validates payload and creates account")
    void testRegisterValidationAndSuccess() throws Exception {
        // 1. Missing fields fails with 400
        RegisterRequest invalid = new RegisterRequest("", "invalid-email", "123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());

        // 2. Valid registration succeeds with 201 Created
        RegisterRequest valid = new RegisterRequest("Integration User", "integration@test.campus", "SecurePassword123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("integration@test.campus"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()); // never leak hash
    }

    @Test
    @DisplayName("GET /api/auth/csrf returns valid CSRF token")
    void testGetCsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.headerName").exists());
    }

    @Test
    @DisplayName("GET /api/auth/me returns unauthenticated for anonymous user")
    void testGetMeAnonymous() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login succeeds for bootstrapped admin and fails for bad credentials")
    void testLoginFlow() throws Exception {
        // Bad credentials fails with 401
        LoginRequest badReq = new LoginRequest("admin@test.campus", "WrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isUnauthorized());

        // Bootstrapped admin succeeds with 200
        LoginRequest goodReq = new LoginRequest("admin@test.campus", "AdminPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("admin@test.campus"));
    }
}
