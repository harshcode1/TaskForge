package com.projectmgmttool.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetMyTasks_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tasks", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetTasksForProject_unauthenticated_returns401() {
        UUID projectId = UUID.randomUUID();
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tasks/project/" + projectId, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testCreateTask_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/tasks", "{}", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
