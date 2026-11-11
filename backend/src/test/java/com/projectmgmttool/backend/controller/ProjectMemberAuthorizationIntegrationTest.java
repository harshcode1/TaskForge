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

/**
 * Who may add, remove and list project members. Uses real JWTs against the
 * real security chain. Non-members get 404 (the project "doesn't exist" for
 * them); members without the right get 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectMemberAuthorizationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ApiTestClient api;

    private String ownerToken;
    private String memberToken;
    private String memberEmail;
    private Object memberId;
    private String strangerToken;
    private String strangerEmail;
    private String projectId;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(restTemplate);
        ownerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("owner"));

        memberEmail = ApiTestClient.uniqueEmail("member");
        Map<String, Object> member = api.register(memberEmail);
        memberToken = (String) member.get("token");
        memberId = member.get("userId");

        strangerEmail = ApiTestClient.uniqueEmail("stranger");
        strangerToken = api.registerAndGetToken(strangerEmail);

        projectId = api.createProject(ownerToken, "Members project");
        assertEquals(HttpStatus.OK, api.invite(ownerToken, projectId, memberEmail, "MEMBER").getStatusCode());
    }

    // ---- invite ----

    @Test
    void invite_asOwner_addsMemberWhoThenAppearsInList() {
        ResponseEntity<List> members = api.get("/api/project-members/" + projectId, ownerToken, List.class);

        assertEquals(HttpStatus.OK, members.getStatusCode());
        assertEquals(1, members.getBody().size());
    }

    @Test
    void invite_asPlainMember_returns403() {
        ResponseEntity<Map> response = api.invite(memberToken, projectId, strangerEmail, "MEMBER");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void invite_asNonMember_returns404() {
        ResponseEntity<Map> response = api.invite(strangerToken, projectId, strangerEmail, "MEMBER");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void invite_withAdminProjectRole_returns400() {
        ResponseEntity<Map> response = api.invite(ownerToken, projectId, strangerEmail, "ADMIN");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void invite_sameUserTwice_returns409() {
        ResponseEntity<Map> response = api.invite(ownerToken, projectId, memberEmail, "MEMBER");
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void invite_theOwnerThemselves_returns409() {
        String ownerEmail = (String) api.get("/api/users/me", ownerToken, Map.class).getBody().get("email");
        ResponseEntity<Map> response = api.invite(ownerToken, projectId, ownerEmail, "MEMBER");
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // ---- remove ----

    @Test
    void remove_asOwner_returns204_andFormerMemberLosesAccess() {
        ResponseEntity<Void> response = api.delete(
                "/api/project-members/" + projectId + "/" + memberId, ownerToken, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        ResponseEntity<Map> afterwards = api.get("/api/project-members/" + projectId, memberToken, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, afterwards.getStatusCode());
    }

    @Test
    void remove_asPlainMember_returns403() {
        ResponseEntity<Map> response = api.delete(
                "/api/project-members/" + projectId + "/" + memberId, memberToken, Map.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void remove_asNonMember_returns404() {
        ResponseEntity<Map> response = api.delete(
                "/api/project-members/" + projectId + "/" + memberId, strangerToken, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void remove_userWhoIsNotAMember_returns404() {
        Object strangerId = api.get("/api/users/me", strangerToken, Map.class).getBody().get("id");
        ResponseEntity<Map> response = api.delete(
                "/api/project-members/" + projectId + "/" + strangerId, ownerToken, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void remove_unauthenticated_returns401() {
        ResponseEntity<Map> response = api.delete(
                "/api/project-members/" + projectId + "/" + memberId, null, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ---- list ----

    @Test
    void listMembers_asMember_returns200() {
        ResponseEntity<List> response = api.get("/api/project-members/" + projectId, memberToken, List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void listMembers_asNonMember_returns404() {
        ResponseEntity<Map> response = api.get("/api/project-members/" + projectId, strangerToken, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
