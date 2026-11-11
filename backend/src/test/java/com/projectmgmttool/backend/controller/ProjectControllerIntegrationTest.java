package com.projectmgmttool.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import com.projectmgmttool.backend.support.ApiTestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetProjects_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/projects", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testCreateProject_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/projects", "{}", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testCreateProject_withValidToken_ownerSeesItInTheirList() {
        ApiTestClient api = new ApiTestClient(restTemplate);
        String token = api.registerAndGetToken(ApiTestClient.uniqueEmail("owner"));

        String projectId = api.createProject(token, "Owner's project");

        ResponseEntity<List> list = api.get("/api/projects", token, List.class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertEquals(1, list.getBody().size());
        assertEquals(projectId, ((Map<?, ?>) list.getBody().get(0)).get("id"));
    }
}
