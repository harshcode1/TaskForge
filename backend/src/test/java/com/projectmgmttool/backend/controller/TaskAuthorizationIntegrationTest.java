package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Create / list / update / delete tasks. Owner and manager and member may
 * work with tasks, only owner or manager may delete, a non-member gets 404
 * (indistinguishable from a task or project that doesn't exist).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskAuthorizationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ApiTestClient api;

    private String ownerEmail;
    private String ownerToken;
    private String managerToken;
    private String memberEmail;
    private String memberToken;
    private String strangerEmail;
    private String strangerToken;
    private String projectId;
    private String taskId;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(restTemplate);
        ownerEmail = ApiTestClient.uniqueEmail("owner");
        ownerToken = api.registerAndGetToken(ownerEmail);
        String managerEmail = ApiTestClient.uniqueEmail("manager");
        managerToken = api.registerAndGetToken(managerEmail);
        memberEmail = ApiTestClient.uniqueEmail("member");
        memberToken = api.registerAndGetToken(memberEmail);
        strangerEmail = ApiTestClient.uniqueEmail("stranger");
        strangerToken = api.registerAndGetToken(strangerEmail);

        projectId = api.createProject(ownerToken, "Tasks project");
        api.invite(ownerToken, projectId, managerEmail, "MANAGER");
        api.invite(ownerToken, projectId, memberEmail, "MEMBER");

        taskId = (String) createTask(ownerToken, projectId, null).getBody().get("id");
    }

    private ResponseEntity<Map> createTask(String token, String forProjectId, String assigneeEmail) {
        return api.post("/api/tasks", token, taskBody(forProjectId, assigneeEmail), Map.class);
    }

    private Map<String, Object> taskBody(String forProjectId, String assigneeEmail) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Write the report");
        body.put("description", "details");
        body.put("status", "TODO");
        body.put("priority", "MEDIUM");
        if (forProjectId != null) {
            body.put("projectId", forProjectId);
        }
        if (assigneeEmail != null) {
            body.put("assigneeEmail", assigneeEmail);
        }
        return body;
    }

    private ResponseEntity<Map> updateTask(String token, String id, String assigneeEmail) {
        return api.exchange(HttpMethod.PUT, "/api/tasks/" + id, token, taskBody(projectId, assigneeEmail), Map.class);
    }

    // ---- create ----

    @Test
    void createTask_asOwner_returns200() {
        assertEquals(HttpStatus.OK, createTask(ownerToken, projectId, null).getStatusCode());
    }

    @Test
    void createTask_asManager_returns200() {
        assertEquals(HttpStatus.OK, createTask(managerToken, projectId, null).getStatusCode());
    }

    @Test
    void createTask_asMember_returns200() {
        assertEquals(HttpStatus.OK, createTask(memberToken, projectId, null).getStatusCode());
    }

    @Test
    void createTask_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, createTask(strangerToken, projectId, null).getStatusCode());
    }

    @Test
    void createTask_inUnknownProject_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = createTask(strangerToken, UUID.randomUUID().toString(), null);
        ResponseEntity<Map> notMine = createTask(strangerToken, projectId, null);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getStatusCode(), notMine.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    @Test
    void createTask_withoutProjectId_returns400() {
        assertEquals(HttpStatus.BAD_REQUEST, createTask(ownerToken, null, null).getStatusCode());
    }

    // ---- assignee must belong to the project ----

    @Test
    void createTask_assigneeIsMember_returns200() {
        assertEquals(HttpStatus.OK, createTask(ownerToken, projectId, memberEmail).getStatusCode());
    }

    @Test
    void createTask_assigneeIsOwner_returns200() {
        assertEquals(HttpStatus.OK, createTask(memberToken, projectId, ownerEmail).getStatusCode());
    }

    @Test
    void createTask_assigneeIsNotInProject_returns400() {
        assertEquals(HttpStatus.BAD_REQUEST, createTask(ownerToken, projectId, strangerEmail).getStatusCode());
    }

    @Test
    void createTask_assigneeDoesNotExist_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                createTask(ownerToken, projectId, "nobody-" + UUID.randomUUID() + "@example.com").getStatusCode());
    }

    // ---- list ----

    @Test
    void listTasks_asOwner_returnsTheProjectTasks() {
        ResponseEntity<List> response = api.get("/api/tasks/project/" + projectId, ownerToken, List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listTasks_asMember_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/tasks/project/" + projectId, memberToken, List.class).getStatusCode());
    }

    @Test
    void listTasks_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.get("/api/tasks/project/" + projectId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void listTasks_unknownProject_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.get("/api/tasks/project/" + UUID.randomUUID(), ownerToken, Map.class).getStatusCode());
    }

    // ---- update ----

    @Test
    void updateTask_asMember_returns200() {
        assertEquals(HttpStatus.OK, updateTask(memberToken, taskId, null).getStatusCode());
    }

    @Test
    void updateTask_asOwner_returns200() {
        assertEquals(HttpStatus.OK, updateTask(ownerToken, taskId, null).getStatusCode());
    }

    @Test
    void updateTask_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, updateTask(strangerToken, taskId, null).getStatusCode());
    }

    @Test
    void updateTask_unknownTask_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = updateTask(strangerToken, UUID.randomUUID().toString(), null);
        ResponseEntity<Map> notMine = updateTask(strangerToken, taskId, null);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getStatusCode(), notMine.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    @Test
    void updateTask_assigneeIsNotInProject_returns400() {
        assertEquals(HttpStatus.BAD_REQUEST, updateTask(ownerToken, taskId, strangerEmail).getStatusCode());
    }

    // ---- delete ----

    @Test
    void deleteTask_asMember_returns403() {
        assertEquals(HttpStatus.FORBIDDEN,
                api.delete("/api/tasks/" + taskId, memberToken, Map.class).getStatusCode());
    }

    @Test
    void deleteTask_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.delete("/api/tasks/" + taskId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void deleteTask_asManager_returns204() {
        assertEquals(HttpStatus.NO_CONTENT,
                api.delete("/api/tasks/" + taskId, managerToken, Void.class).getStatusCode());
    }

    @Test
    void deleteTask_asOwner_returns204() {
        assertEquals(HttpStatus.NO_CONTENT,
                api.delete("/api/tasks/" + taskId, ownerToken, Void.class).getStatusCode());
    }
}
