package com.gullycricket.backend.matches.live.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class LiveMatchWebSocketConfig implements WebSocketConfigurer {

    private final LiveMatchWebSocketHandler handler;
    private final String[] allowedOrigins;

    public LiveMatchWebSocketConfig(
            LiveMatchWebSocketHandler handler,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer(
            @Value("${app.live-scoring.max-message-bytes:4194304}") int maxMessageBytes) {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        int safeLimit = Math.max(64 * 1024, maxMessageBytes);
        container.setMaxTextMessageBufferSize(safeLimit);
        container.setMaxBinaryMessageBufferSize(safeLimit);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/live-matches")
                .setAllowedOrigins(allowedOrigins);
    }
}
