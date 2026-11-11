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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The read-only, project-scoped endpoints: dashboard, activity feed and the AI
 * project summary. Members may read them; non-members get 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectReadEndpointsAuthorizationIntegrationTest {

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
        String managerEmail = ApiTestClient.uniqueEmail("manager");
        api.registerAndGetToken(managerEmail);
        strangerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("stranger"));

        projectId = api.createProject(ownerToken, "Read-only endpoints project");
        api.invite(ownerToken, projectId, memberEmail, "MEMBER");
        api.invite(ownerToken, projectId, managerEmail, "MANAGER");
    }

    // ---- GET /api/dashboard/{projectId} ----

    @Test
    void dashboard_asOwner_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/dashboard/" + projectId, ownerToken, Map.class).getStatusCode());
    }

    @Test
    void dashboard_asMember_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/dashboard/" + projectId, memberToken, Map.class).getStatusCode());
    }

    @Test
    void dashboard_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, api.get("/api/dashboard/" + projectId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void dashboard_unknownProject_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = api.get("/api/dashboard/" + UUID.randomUUID(), strangerToken, Map.class);
        ResponseEntity<Map> notMine = api.get("/api/dashboard/" + projectId, strangerToken, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    @Test
    void dashboard_totalMembers_countsTheOwnerPlusEveryMember() {
        // owner + 1 MEMBER + 1 MANAGER (set up in @BeforeEach) = 3
        Map<?, ?> body = api.get("/api/dashboard/" + projectId, ownerToken, Map.class).getBody();
        assertEquals(3, ((Number) body.get("totalMembers")).intValue());
    }

    // ---- GET /api/activity/{projectId} ----

    @Test
    void activity_asMember_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/activity/" + projectId, memberToken, List.class).getStatusCode());
    }

    @Test
    void activity_asOwner_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/activity/" + projectId, ownerToken, List.class).getStatusCode());
    }

    @Test
    void activity_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, api.get("/api/activity/" + projectId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void activity_unknownProject_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = api.get("/api/activity/" + UUID.randomUUID(), strangerToken, Map.class);
        ResponseEntity<Map> notMine = api.get("/api/activity/" + projectId, strangerToken, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    // ---- GET /api/ai/project-summary/{projectId} ----
    // No OpenAI key in tests, so a caller who passes the access check gets 503
    // ("AI not configured"). That is how these tests tell "allowed" from "denied".

    @Test
    void aiSummary_asMember_passesAccessCheck() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                api.get("/api/ai/project-summary/" + projectId, memberToken, Map.class).getStatusCode());
    }

    @Test
    void aiSummary_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.get("/api/ai/project-summary/" + projectId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void aiSummary_unknownProject_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = api.get("/api/ai/project-summary/" + UUID.randomUUID(), strangerToken, Map.class);
        ResponseEntity<Map> notMine = api.get("/api/ai/project-summary/" + projectId, strangerToken, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }
}
