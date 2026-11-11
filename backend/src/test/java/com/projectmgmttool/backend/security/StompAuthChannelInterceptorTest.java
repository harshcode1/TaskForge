package com.projectmgmttool.backend.security;

import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.service.ProjectAccessService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The STOMP security rules, one frame at a time:
 * CONNECT must carry a valid JWT, SUBSCRIBE is only allowed to the live topic
 * of a project the user belongs to, and clients may not SEND to /topic/**
 * (that would let anyone forge "task deleted" events for everyone else).
 */
@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    private static final String EMAIL = "member@example.com";

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private ProjectAccessService access;

    @Mock
    private MessageChannel channel;

    private final UUID projectId = UUID.randomUUID();

    private Message<byte[]> frame(StompCommand command, String destination, String authorizationHeader, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Principal signedInUser() {
        return () -> EMAIL;
    }

    private String projectTopic() {
        return "/topic/project/" + projectId + "/tasks";
    }

    private UserDetails userDetails() {
        return new User(EMAIL, "ignored", List.of());
    }

    // ---- CONNECT ----

    @Test
    void connect_withValidToken_setsTheUser() {
        when(jwtUtil.extractUsername("good")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtil.validateToken("good", EMAIL)).thenReturn(true);

        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer good", null), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(accessor.getUser());
        assertEquals(EMAIL, accessor.getUser().getName());
    }

    @Test
    void connect_withoutAuthorizationHeader_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), channel));
    }

    @Test
    void connect_withMalformedAuthorizationHeader_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Token good", null), channel));
    }

    @Test
    void connect_withGarbageToken_isRejected() {
        when(jwtUtil.extractUsername("garbage")).thenThrow(new JwtException("bad token"));

        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer garbage", null), channel));
    }

    @Test
    void connect_withTokenForAnUnknownUser_isRejected() {
        when(jwtUtil.extractUsername("good")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenThrow(new UsernameNotFoundException("gone"));

        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer good", null), channel));
    }

    @Test
    void connect_withTokenThatFailsValidation_isRejected() {
        when(jwtUtil.extractUsername("old")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtil.validateToken("old", EMAIL)).thenReturn(false);

        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer old", null), channel));
    }

    // ---- SUBSCRIBE ----

    @Test
    void subscribe_projectTopic_asMember_isAllowed() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, projectTopic(), null, signedInUser());

        assertSame(message, interceptor.preSend(message, channel));
        verify(access).requireMember(projectId, EMAIL);
    }

    @Test
    void subscribe_projectTopic_asNonMember_isRejected() {
        when(access.requireMember(projectId, EMAIL)).thenThrow(new CustomApiException("Not found", 404));

        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, projectTopic(), null, signedInUser()), channel));
    }

    @Test
    void subscribe_withoutAuthenticatedUser_isRejected() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, projectTopic(), null, null), channel));
        verifyNoInteractions(access);
    }

    @Test
    void subscribe_toAnyOtherDestination_isRejected() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/anything-else", null, signedInUser()), channel));
        verifyNoInteractions(access);
    }

    @Test
    void subscribe_toAWildcardOrNestedTopic_isRejected() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/project/" + projectId + "/tasks/extra", null, signedInUser()), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/project/*/tasks", null, signedInUser()), channel));
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/project/not-a-uuid/tasks", null, signedInUser()), channel));
        verifyNoInteractions(access);
    }

    // ---- SEND ----

    @Test
    void send_toATopic_isRejected_evenForAMember() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, projectTopic(), null, signedInUser()), channel));
    }

    @Test
    void send_toAnApplicationDestination_isNotBlockedByThisInterceptor() {
        Message<byte[]> message = frame(StompCommand.SEND, "/app/anything", null, signedInUser());

        assertSame(message, interceptor.preSend(message, channel));
    }

    // ---- other frames ----

    @Test
    void disconnect_passesThrough() {
        Message<byte[]> message = frame(StompCommand.DISCONNECT, null, null, null);

        assertSame(message, interceptor.preSend(message, channel));
        verify(jwtUtil, never()).extractUsername(any());
    }
}
