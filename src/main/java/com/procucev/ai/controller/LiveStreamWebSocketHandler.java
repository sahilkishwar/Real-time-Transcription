package com.procucev.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procucev.ai.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveStreamWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveStreamWebSocketHandler.class);

    private final LanguageDetectionService languageDetectionService;
    private final ObjectMapper objectMapper;

    // Track active sessions and their streaming audio buffers
    private final Map<String, ByteArrayOutputStream> audioBuffers = new ConcurrentHashMap<>();

    public LiveStreamWebSocketHandler(LanguageDetectionService languageDetectionService, ObjectMapper objectMapper) {
        this.languageDetectionService = languageDetectionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connected: session ID={}", session.getId());
        audioBuffers.put(session.getId(), new ByteArrayOutputStream());

        Map<String, Object> welcome = Map.of(
                "type", "CONNECTED",
                "sessionId", session.getId(),
                "message", "Real-time multilingual transcription WebSocket channel ready"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            Map<?, ?> json = objectMapper.readValue(payload, Map.class);
            String type = (String) json.get("type");

            if ("ANALYZE_STREAM".equals(type)) {
                String liveText = (String) json.get("text");
                if (liveText != null) {
                    LanguageDetectionService.DetectionResult result = languageDetectionService.analyze(liveText);
                    Map<String, Object> resp = Map.of(
                            "type", "STREAM_METRICS",
                            "primaryLanguage", result.getPrimaryLanguage(),
                            "languageRatio", result.getLanguageRatio(),
                            "confidence", result.getConfidence(),
                            "wordCount", liveText.trim().isEmpty() ? 0 : liveText.trim().split("\\s+").length
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
                }
            } else if ("PING".equals(type)) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "PONG", "timestamp", System.currentTimeMillis()))));
            }
        } catch (Exception e) {
            log.warn("Error processing WebSocket text frame: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket closed: session ID={}, status={}", session.getId(), status);
        audioBuffers.remove(session.getId());
    }
}
