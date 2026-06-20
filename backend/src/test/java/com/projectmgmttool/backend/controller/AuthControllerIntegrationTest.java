package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.dto.AuthRequest;
import com.projectmgmttool.backend.dto.AuthResponse;
import com.projectmgmttool.backend.dto.RegisterRequest;
import com.projectmgmttool.backend.entity.enums.Role;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String TEST_EMAIL = "integration_test_auth@example.com";
    private static final String TEST_PASSWORD = "Test123!";

    @Test
    @Order(1)
    void testRegisterUser() {
        RegisterRequest request = new RegisterRequest("Integration User", TEST_EMAIL, TEST_PASSWORD, Role.MEMBER);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", request, AuthResponse.class);

        assertTrue(response.getStatusCode() == HttpStatus.OK
                || response.getStatusCode() == HttpStatus.CONFLICT,
                "Should return 200 or 409 if already registered");
    }

    @Test
    @Order(2)
    void testLoginUser_withValidCredentials_returnsToken() {
        // Ensure user exists
        RegisterRequest reg = new RegisterRequest("Integration User", TEST_EMAIL, TEST_PASSWORD, Role.MEMBER);
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", request, AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertEquals(TEST_EMAIL, response.getBody().getEmail());
    }
}
