package com.projectmgmttool.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Live board updates — when one user drags a card or edits a task, everyone
 * else looking at the same project's board sees it change without a manual
 * refresh. Broadcast-only: the actual mutation always goes through the
 * existing authenticated REST endpoints first (TaskController etc.); this
 * channel just tells other open tabs "something in project X changed, go
 * refetch" — it never carries the task data itself, so there's no new
 * surface here for sensitive data to leak through even without per-message
 * auth on the socket. The topic name is scoped by project id, so only a
 * client that already knows that id (because it legitimately loaded that
 * project over the real API) has any reason to subscribe to it.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
