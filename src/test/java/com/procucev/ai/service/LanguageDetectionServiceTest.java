package com.procucev.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LanguageDetectionService Tests - Code-Mixing & Hinglish Detection")
class LanguageDetectionServiceTest {

    private LanguageDetectionService detectionService;

    @BeforeEach
    void setUp() {
        detectionService = new LanguageDetectionService();
    }

    @Test
    @DisplayName("Should detect Hinglish when text contains mixed Hindi and English words")
    void testHinglishDetection() {
        String input = "Aaj hum log meeting discuss karenge regarding deployment and real-time audio visualization";
        LanguageDetectionService.DetectionResult result = detectionService.analyze(input);

        assertNotNull(result);
        assertEquals("Hinglish (Hindi + English)", result.getPrimaryLanguage());
        assertTrue(result.getLanguageRatio().contains("Hindi:"));
        assertTrue(result.getLanguageRatio().contains("English:"));
        assertTrue(result.getConfidence() > 0.85);
    }

    @Test
    @DisplayName("Should detect Devanagari Hindi with English loanwords")
    void testDevanagariHinglishDetection() {
        String input = "ग्राहक ने application की performance और speech recognition accuracy की बहुत प्रशंसा की है";
        LanguageDetectionService.DetectionResult result = detectionService.analyze(input);

        assertNotNull(result);
        assertEquals("Hinglish (Devanagari + English)", result.getPrimaryLanguage());
        assertTrue(result.getConfidence() > 0.9);
    }

    @Test
    @DisplayName("Should detect pure English technical sentence")
    void testEnglishDetection() {
        String input = "The Spring Boot application manages bidirectional audio streaming using WebSockets";
        LanguageDetectionService.DetectionResult result = detectionService.analyze(input);

        assertNotNull(result);
        assertEquals("English", result.getPrimaryLanguage());
        assertTrue(result.getLanguageRatio().contains("English: 90%") || result.getLanguageRatio().contains("English: 100%"));
    }

    @Test
    @DisplayName("Should gracefully handle null or empty inputs")
    void testEmptyAndNullInput() {
        LanguageDetectionService.DetectionResult nullResult = detectionService.analyze(null);
        assertEquals("Unknown", nullResult.getPrimaryLanguage());
        assertEquals("N/A", nullResult.getLanguageRatio());
        assertEquals(0.0, nullResult.getConfidence());

        LanguageDetectionService.DetectionResult emptyResult = detectionService.analyze("   ");
        assertEquals("Unknown", emptyResult.getPrimaryLanguage());
    }
}
