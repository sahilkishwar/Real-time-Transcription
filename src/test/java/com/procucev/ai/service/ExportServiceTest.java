package com.procucev.ai.service;

import com.procucev.ai.model.Transcription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExportService Tests")
class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
    }

    private Transcription createMockTranscription(Long id, String title, String content, Double duration) {
        Transcription t = new Transcription();
        t.setId(id);
        t.setTitle(title);
        t.setContent(content);
        t.setAudioDurationSeconds(duration);
        t.setWordCount(content.split("\\s+").length);
        t.setDetectedLanguage("English");
        t.setLanguageRatio("English: 100%");
        t.setConfidence(0.98);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    @DisplayName("Should export empty string for empty transcription list in SRT")
    void testExportEmptySrt() {
        assertEquals("", exportService.exportToSrt(Collections.emptyList()));
        assertEquals("", exportService.exportToSrt(null));
    }

    @Test
    @DisplayName("Should generate valid sequential SRT cues with timecodes")
    void testExportToSrt() {
        List<Transcription> list = List.of(
                createMockTranscription(1L, "Greeting", "Hello everyone welcome to the demo.", 3.5),
                createMockTranscription(2L, "Discussion", "We are testing real-time transcription.", 4.0)
        );

        String srt = exportService.exportToSrt(list);
        assertNotNull(srt);
        assertTrue(srt.contains("1\n00:00:00,000 --> 00:00:03,500"));
        assertTrue(srt.contains("Hello everyone welcome to the demo."));
        assertTrue(srt.contains("2\n00:00:03,800 --> 00:00:07,800"));
        assertTrue(srt.contains("We are testing real-time transcription."));
    }

    @Test
    @DisplayName("Should generate valid WebVTT headers and cues")
    void testExportToWebVtt() {
        List<Transcription> list = List.of(
                createMockTranscription(1L, "Intro", "This is WebVTT export.", 2.0)
        );

        String vtt = exportService.exportToWebVtt(list);
        assertTrue(vtt.startsWith("WEBVTT"));
        assertTrue(vtt.contains("cue-1"));
        assertTrue(vtt.contains("00:00:00.000 --> 00:00:02.000"));
        assertTrue(vtt.contains("This is WebVTT export."));
    }

    @Test
    @DisplayName("Should generate Markdown report with metrics table and code blocks")
    void testExportToMarkdown() {
        List<Transcription> list = List.of(
                createMockTranscription(1L, "Keynote", "Artificial intelligence in speech recognition.", 5.0)
        );

        String md = exportService.exportToMarkdown(list);
        assertTrue(md.contains("# 🎙️ Procucev AI — Real-time Speech Transcription Report"));
        assertTrue(md.contains("| **Total Transcripts** | `1` |"));
        assertTrue(md.contains("#### 1. Keynote"));
        assertTrue(md.contains("Artificial intelligence in speech recognition."));
    }

    @Test
    @DisplayName("Should format timecodes accurately")
    void testTimecodeFormatting() {
        // 3661500 ms = 1 hr, 1 min, 1 sec, 500 ms
        long millis = 3661500;
        assertEquals("01:01:01,500", ExportService.formatTimecodeSrt(millis));
        assertEquals("01:01:01.500", ExportService.formatTimecodeVtt(millis));
    }
}
