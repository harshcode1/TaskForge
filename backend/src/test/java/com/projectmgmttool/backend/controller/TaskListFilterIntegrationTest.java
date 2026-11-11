package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GET /api/tasks/project/{id}?status=... returns only the tasks with that status. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskListFilterIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ApiTestClient api;
    private String ownerToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(restTemplate);
        ownerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("owner"));
        projectId = api.createProject(ownerToken, "Filter project");
        createTask("Todo one", "TODO");
        createTask("Todo two", "TODO");
        createTask("Done one", "DONE");
    }

    private void createTask(String title, String status) {
        api.post("/api/tasks", ownerToken, Map.of("title", title, "status", status,
                "priority", "LOW", "projectId", projectId), Map.class);
    }

    @Test
    void noFilter_returnsEveryTask() {
        ResponseEntity<List> response = api.get("/api/tasks/project/" + projectId, ownerToken, List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
    }

    @Test
    void statusFilter_returnsOnlyTasksWithThatStatus() {
        ResponseEntity<List> response = api.get("/api/tasks/project/" + projectId + "?status=DONE", ownerToken, List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().stream().allMatch(t -> "DONE".equals(((Map<?, ?>) t).get("status"))));
    }

    @Test
    void statusFilter_withAnUnknownStatus_returns400() {
        ResponseEntity<Map> response = api.get("/api/tasks/project/" + projectId + "?status=BOGUS", ownerToken, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
