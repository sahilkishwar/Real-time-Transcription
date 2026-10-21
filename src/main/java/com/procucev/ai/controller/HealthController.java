package com.procucev.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private final DataSource dataSource;
    private final long startupTimeMs = System.currentTimeMillis();

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "realtime-transcription-service");
        health.put("timestamp", Instant.now().toString());
        health.put("uptimeSeconds", (System.currentTimeMillis() - startupTimeMs) / 1000);

        // JVM Telemetry
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        health.put("jvm", Map.of(
                "usedMemoryMb", usedMemory / (1024 * 1024),
                "totalMemoryMb", totalMemory / (1024 * 1024),
                "maxMemoryMb", runtime.maxMemory() / (1024 * 1024),
                "availableProcessors", runtime.availableProcessors(),
                "javaVersion", System.getProperty("java.version"),
                "jvmUptimeMs", ManagementFactory.getRuntimeMXBean().getUptime()
        ));

        // Database Connectivity Health
        boolean dbHealthy = false;
        long dbLatencyMs = -1;
        try {
            long start = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection()) {
                dbHealthy = conn.isValid(2);
                dbLatencyMs = System.currentTimeMillis() - start;
            }
        } catch (Exception e) {
            dbHealthy = false;
        }

        health.put("database", Map.of(
                "connected", dbHealthy,
                "latencyMs", dbLatencyMs
        ));

        health.put("capabilities", List.of(
                "WebSocket Audio Streaming (/ws/transcribe)",
                "Multilingual Hinglish Code-Mixing Analyzer",
                "Relational Database Persistence (CRUD)",
                "Puter.js Multilingual Whisper STT Engine",
                "Google Gemini Multimodal Audio Processing"
        ));

        return ResponseEntity.ok(health);
    }
}
