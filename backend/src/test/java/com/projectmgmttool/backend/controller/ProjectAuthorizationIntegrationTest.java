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

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Read / update / delete on /api/projects/{id}. Non-members get 404, members
 * without the right get 403, the owner is allowed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectAuthorizationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ApiTestClient api;
    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(restTemplate);
        ownerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("owner"));
        String memberEmail = ApiTestClient.uniqueEmail("member");
        memberToken = api.registerAndGetToken(memberEmail);
        strangerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("stranger"));

        projectId = api.createProject(ownerToken, "Secret project");
        api.invite(ownerToken, projectId, memberEmail, "MEMBER");
    }

    private ResponseEntity<Map> update(String token, String id) {
        return api.exchange(HttpMethod.PUT, "/api/projects/" + id, token,
                Map.of("name", "Renamed", "description", "changed"), Map.class);
    }

    // ---- read ----

    @Test
    void getProject_asOwner_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/projects/" + projectId, ownerToken, Map.class).getStatusCode());
    }

    @Test
    void getProject_asMember_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/projects/" + projectId, memberToken, Map.class).getStatusCode());
    }

    @Test
    void getProject_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, api.get("/api/projects/" + projectId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void getProject_unknownId_returns404_sameAsNonMember() {
        ResponseEntity<Map> unknown = api.get("/api/projects/" + UUID.randomUUID(), strangerToken, Map.class);
        ResponseEntity<Map> notMine = api.get("/api/projects/" + projectId, strangerToken, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getStatusCode(), notMine.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    @Test
    void getProject_unauthenticated_returns401() {
        assertEquals(HttpStatus.UNAUTHORIZED, api.get("/api/projects/" + projectId, null, Map.class).getStatusCode());
    }

    // ---- update ----

    @Test
    void updateProject_asOwner_returns200() {
        assertEquals(HttpStatus.OK, update(ownerToken, projectId).getStatusCode());
    }

    @Test
    void updateProject_asMember_returns403() {
        assertEquals(HttpStatus.FORBIDDEN, update(memberToken, projectId).getStatusCode());
    }

    @Test
    void updateProject_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, update(strangerToken, projectId).getStatusCode());
    }

    // ---- delete ----

    @Test
    void deleteProject_asMember_returns403() {
        assertEquals(HttpStatus.FORBIDDEN,
                api.delete("/api/projects/" + projectId, memberToken, Map.class).getStatusCode());
    }

    @Test
    void deleteProject_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.delete("/api/projects/" + projectId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void deleteProject_asOwner_returns204_andProjectIsGone() {
        assertEquals(HttpStatus.NO_CONTENT,
                api.delete("/api/projects/" + projectId, ownerToken, Void.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, api.get("/api/projects/" + projectId, ownerToken, Map.class).getStatusCode());
    }
}
