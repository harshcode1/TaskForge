package com.projectmgmttool.backend.security;

import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.service.ProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security for the WebSocket, applied to every frame a client sends:
 *
 * - CONNECT must carry "Authorization: Bearer <jwt>" in the STOMP headers (the
 *   browser WebSocket handshake can't send headers, so /ws itself stays open
 *   and the check happens here). The user is remembered for the session.
 * - SUBSCRIBE is only allowed to /topic/project/{id}/tasks, and only if the
 *   user is the owner or a member of that project. Anything else is refused.
 * - SEND to /topic/** is refused: the server is the only one allowed to publish
 *   there, otherwise any client could forge events for everyone else.
 *
 * Throwing from preSend makes Spring answer with a STOMP ERROR frame.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/project/([0-9a-fA-F-]{36})/tasks$");

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ProjectAccessService projectAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> rejectSendToTopic(accessor);
            default -> { }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing or malformed Authorization header");
        }
        String token = header.substring(7);

        UserDetails user;
        boolean valid;
        try {
            user = userDetailsService.loadUserByUsername(jwtUtil.extractUsername(token));
            valid = jwtUtil.validateToken(token, user.getUsername());
        } catch (Exception e) {
            throw new MessagingException("Invalid or expired token");
        }
        if (!valid) {
            throw new MessagingException("Invalid or expired token");
        }
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        Matcher topic = PROJECT_TOPIC.matcher(destination == null ? "" : destination);
        if (user == null || !topic.matches()) {
            throw new MessagingException("Subscription not allowed");
        }

        try {
            projectAccessService.requireMember(UUID.fromString(topic.group(1)), user.getName());
        } catch (CustomApiException | IllegalArgumentException e) {
            throw new MessagingException("Subscription not allowed");
        }
    }

    private void rejectSendToTopic(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith("/topic")) {
            throw new MessagingException("Clients may not publish to /topic");
        }
    }
}
