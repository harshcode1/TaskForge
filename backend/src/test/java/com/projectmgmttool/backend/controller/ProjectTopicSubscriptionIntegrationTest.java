package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.support.ApiTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end check over a real WebSocket: only the owner and members of a
 * project may subscribe to its live-update topic. The interceptor unit tests
 * (StompAuthChannelInterceptorTest) are the detailed proof; this confirms the
 * interceptor is really wired into the running app.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectTopicSubscriptionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private ApiTestClient api;
    private WebSocketStompClient stompClient;

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
        projectId = api.createProject(ownerToken, "Live board");
        api.invite(ownerToken, projectId, memberEmail, "MEMBER");

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
    }

    /** Records whether the server accepted the connection or answered with an ERROR frame. */
    private static class ServerAnswer extends StompSessionHandlerAdapter {
        final CompletableFuture<Boolean> connected = new CompletableFuture<>();
        final CompletableFuture<Boolean> error = new CompletableFuture<>();

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            connected.complete(true);
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            error.complete(true);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            error.complete(true);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        /** "ERROR" if the server sent an ERROR frame within 5 seconds, otherwise "NOTHING". */
        String errorWithinFiveSeconds() {
            try {
                error.get(5, TimeUnit.SECONDS);
                return "ERROR";
            } catch (Exception e) {
                return "NOTHING";
            }
        }

        /** "CONNECTED", "ERROR", or "NOTHING" if the server said neither within 5 seconds. */
        String firstAnswer() {
            try {
                return (String) CompletableFuture.anyOf(
                        connected.thenApply(x -> "CONNECTED"),
                        error.thenApply(x -> "ERROR")).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                return "NOTHING";
            }
        }
    }

    private StompSession connectWithToken(String token, ServerAnswer answer) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return stompClient.connectAsync("ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(), connectHeaders, answer).get(5, TimeUnit.SECONDS);
    }

    private StompSession.Subscription subscribeToProject(StompSession session, CompletableFuture<Map> received) {
        return session.subscribe("/topic/project/" + projectId + "/tasks", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((Map) payload);
            }
        });
    }

    @Test
    void member_canSubscribe_andReceivesLiveTaskEvents() throws Exception {
        ServerAnswer answer = new ServerAnswer();
        StompSession session = connectWithToken(memberToken, answer);
        CompletableFuture<Map> received = new CompletableFuture<>();
        subscribeToProject(session, received);

        // SUBSCRIBE is asynchronous, so keep creating tasks until the first event arrives.
        Map event = null;
        for (int attempt = 0; attempt < 5 && event == null; attempt++) {
            api.post("/api/tasks", ownerToken, Map.of("title", "Live task", "status", "TODO",
                    "priority", "LOW", "projectId", projectId), Map.class);
            try {
                event = received.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                // not subscribed yet, try again
            }
        }

        assertNotNull(event, "a member should receive live task events");
        assertEquals("CREATED", event.get("type"));
    }

    @Test
    void nonMember_subscribeIsRejected() throws Exception {
        ServerAnswer answer = new ServerAnswer();
        StompSession session = connectWithToken(strangerToken, answer);
        CompletableFuture<Map> received = new CompletableFuture<>();
        subscribeToProject(session, received);

        assertEquals("ERROR", answer.errorWithinFiveSeconds());
    }

    @Test
    void connectWithoutToken_isRejected() {
        ServerAnswer answer = new ServerAnswer();
        stompClient.connectAsync("ws://localhost:" + port + "/ws", answer);

        assertEquals("ERROR", answer.firstAnswer());
    }
}
