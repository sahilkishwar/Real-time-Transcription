package com.procucev.ai.config;

import com.procucev.ai.controller.LiveStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveStreamWebSocketHandler liveStreamWebSocketHandler;

    public WebSocketConfig(LiveStreamWebSocketHandler liveStreamWebSocketHandler) {
        this.liveStreamWebSocketHandler = liveStreamWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveStreamWebSocketHandler, "/ws/transcribe")
                .setAllowedOrigins("*");
    }
}
