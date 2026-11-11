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
 * Comments live inside a task, which lives inside a project, so access to a
 * comment is decided by membership of that project. Non-members get 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CommentAuthorizationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ApiTestClient api;

    private String ownerToken;
    private String memberToken;
    private Object memberId;
    private String otherMemberToken;
    private String strangerToken;
    private String projectId;
    private String taskId;
    private String memberCommentId;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(restTemplate);
        ownerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("owner"));

        String memberEmail = ApiTestClient.uniqueEmail("member");
        Map<String, Object> member = api.register(memberEmail);
        memberToken = (String) member.get("token");
        memberId = member.get("userId");

        String otherEmail = ApiTestClient.uniqueEmail("other");
        otherMemberToken = api.registerAndGetToken(otherEmail);
        strangerToken = api.registerAndGetToken(ApiTestClient.uniqueEmail("stranger"));

        projectId = api.createProject(ownerToken, "Comments project");
        api.invite(ownerToken, projectId, memberEmail, "MEMBER");
        api.invite(ownerToken, projectId, otherEmail, "MEMBER");

        Map<String, Object> task = Map.of("title", "Discuss design", "status", "TODO",
                "priority", "LOW", "projectId", projectId);
        taskId = (String) api.post("/api/tasks", ownerToken, task, Map.class).getBody().get("id");

        memberCommentId = (String) addComment(memberToken, taskId).getBody().get("id");
    }

    private ResponseEntity<Map> addComment(String token, String forTaskId) {
        return api.post("/api/comments/add", token, Map.of("taskId", forTaskId, "text", "Looks good"), Map.class);
    }

    // ---- add ----

    @Test
    void addComment_asOwner_returns200() {
        assertEquals(HttpStatus.OK, addComment(ownerToken, taskId).getStatusCode());
    }

    @Test
    void addComment_asMember_returns200() {
        assertEquals(HttpStatus.OK, addComment(memberToken, taskId).getStatusCode());
    }

    @Test
    void addComment_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND, addComment(strangerToken, taskId).getStatusCode());
    }

    @Test
    void addComment_unknownTask_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = addComment(strangerToken, UUID.randomUUID().toString());
        ResponseEntity<Map> notMine = addComment(strangerToken, taskId);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getStatusCode(), notMine.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    // ---- list ----

    @Test
    void listComments_asMember_returnsThem() {
        ResponseEntity<List> response = api.get("/api/comments/task/" + taskId, memberToken, List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listComments_asOwner_returns200() {
        assertEquals(HttpStatus.OK, api.get("/api/comments/task/" + taskId, ownerToken, List.class).getStatusCode());
    }

    @Test
    void listComments_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.get("/api/comments/task/" + taskId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void listComments_unknownTask_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = api.get("/api/comments/task/" + UUID.randomUUID(), strangerToken, Map.class);
        ResponseEntity<Map> notMine = api.get("/api/comments/task/" + taskId, strangerToken, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }

    // ---- delete ----

    @Test
    void deleteComment_asAuthor_returns200() {
        assertEquals(HttpStatus.OK,
                api.delete("/api/comments/" + memberCommentId, memberToken, Void.class).getStatusCode());
    }

    @Test
    void deleteComment_asOtherMember_returns403() {
        assertEquals(HttpStatus.FORBIDDEN,
                api.delete("/api/comments/" + memberCommentId, otherMemberToken, Map.class).getStatusCode());
    }

    @Test
    void deleteComment_asOwnerWhoIsNotTheAuthor_returns403() {
        assertEquals(HttpStatus.FORBIDDEN,
                api.delete("/api/comments/" + memberCommentId, ownerToken, Map.class).getStatusCode());
    }

    @Test
    void deleteComment_asNonMember_returns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                api.delete("/api/comments/" + memberCommentId, strangerToken, Map.class).getStatusCode());
    }

    @Test
    void deleteComment_authorWhoWasRemovedFromProject_returns404() {
        api.delete("/api/project-members/" + projectId + "/" + memberId, ownerToken, Void.class);

        assertEquals(HttpStatus.NOT_FOUND,
                api.delete("/api/comments/" + memberCommentId, memberToken, Map.class).getStatusCode());
    }

    @Test
    void deleteComment_unknownComment_looksIdenticalToNonMemberResponse() {
        ResponseEntity<Map> unknown = api.delete("/api/comments/" + UUID.randomUUID(), strangerToken, Map.class);
        ResponseEntity<Map> notMine = api.delete("/api/comments/" + memberCommentId, strangerToken, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals(unknown.getBody().get("message"), notMine.getBody().get("message"));
    }
}
