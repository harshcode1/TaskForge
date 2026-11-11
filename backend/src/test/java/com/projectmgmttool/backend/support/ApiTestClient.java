package com.projectmgmttool.backend.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Small helper for integration tests: registers users through the real
 * /api/auth/register endpoint and sends requests with their JWT, so tests
 * exercise the same security chain a real client does.
 */
public class ApiTestClient {

    public static final String PASSWORD = "Passw0rd1";

    private final TestRestTemplate rest;

    public ApiTestClient(TestRestTemplate rest) {
        this.rest = rest;
    }

    /** An email that is unique per call, so tests never collide with each other. */
    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    /** Registers a new user through the API; the result has "token" and "userId". */
    public Map<String, Object> register(String email) {
        Map<String, String> body = Map.of("name", "Test User", "email", email, "password", PASSWORD);
        return rest.postForEntity("/api/auth/register", body, Map.class).getBody();
    }

    /** Registers a new user and returns their JWT. */
    public String registerAndGetToken(String email) {
        return (String) register(email).get("token");
    }

    public <T> ResponseEntity<T> exchange(HttpMethod method, String path, String token, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), responseType);
    }

    public <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        return exchange(HttpMethod.GET, path, token, null, responseType);
    }

    public <T> ResponseEntity<T> post(String path, String token, Object body, Class<T> responseType) {
        return exchange(HttpMethod.POST, path, token, body, responseType);
    }

    public <T> ResponseEntity<T> delete(String path, String token, Class<T> responseType) {
        return exchange(HttpMethod.DELETE, path, token, null, responseType);
    }

    /** Invites a user to a project; role is "MANAGER", "MEMBER", etc. */
    public ResponseEntity<Map> invite(String token, String projectId, String userEmail, String role) {
        return post("/api/project-members/invite", token,
                Map.of("projectId", projectId, "userEmail", userEmail, "role", role), Map.class);
    }

    /** Creates a project as the given user and returns its id. */
    public String createProject(String token, String name) {
        ResponseEntity<Map> response = post("/api/projects", token,
                Map.of("name", name, "description", "created by a test"), Map.class);
        return (String) response.getBody().get("id");
    }
}
