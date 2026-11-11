package com.projectmgmttool.backend.config;

import com.projectmgmttool.backend.security.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
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
 * refetch" — it never carries the task data itself.
 *
 * The socket is authenticated at the STOMP level, not by the HTTP filter
 * chain: StompAuthChannelInterceptor requires a JWT on CONNECT, only lets a
 * project's owner and members SUBSCRIBE to that project's topic, and stops
 * clients from publishing to /topic themselves.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Autowired
    private StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
