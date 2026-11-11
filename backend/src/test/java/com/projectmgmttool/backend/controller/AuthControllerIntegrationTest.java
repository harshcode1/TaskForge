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
import com.projectmgmttool.backend.repository.UserRepository;
import com.projectmgmttool.backend.support.ApiTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "integration_test_auth@example.com";
    private static final String TEST_PASSWORD = "Test123!";

    @Test
    @Order(1)
    void testRegisterUser() {
        RegisterRequest request = new RegisterRequest("Integration User", TEST_EMAIL, TEST_PASSWORD);
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
        RegisterRequest reg = new RegisterRequest("Integration User", TEST_EMAIL, TEST_PASSWORD);
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", request, AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertEquals(TEST_EMAIL, response.getBody().getEmail());
    }

    @Test
    void testRegister_withRoleAdminInRequestBody_userIsSavedAsMember() {
        String email = ApiTestClient.uniqueEmail("sneaky");
        // Raw JSON on purpose: this is exactly what an attacker would send.
        String body = "{\"name\":\"Sneaky User\",\"email\":\"" + email
                + "\",\"password\":\"" + ApiTestClient.PASSWORD + "\",\"role\":\"ADMIN\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", new HttpEntity<>(body, headers), AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Role.MEMBER, response.getBody().getRole());
        assertEquals(Role.MEMBER, userRepository.findByEmail(email).orElseThrow().getRole());
    }
}
