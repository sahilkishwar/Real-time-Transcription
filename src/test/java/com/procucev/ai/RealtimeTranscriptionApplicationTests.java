package com.procucev.ai;

import com.procucev.ai.dto.StatsResponse;
import com.procucev.ai.dto.TranscriptionRequest;
import com.procucev.ai.dto.TranscriptionResponse;
import com.procucev.ai.service.LanguageDetectionService;
import com.procucev.ai.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RealtimeTranscriptionApplicationTests {

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    @Test
    void contextLoads() {
        assertNotNull(transcriptionService);
        assertNotNull(languageDetectionService);
    }

    @Test
    void testLanguageDetectionHinglish() {
        String hinglish = "Aaj hum log meeting discuss karenge and finish the sprint tasks";
        LanguageDetectionService.DetectionResult result = languageDetectionService.analyze(hinglish);
        assertTrue(result.getPrimaryLanguage().contains("Hinglish"));
        assertTrue(result.getLanguageRatio().contains("Hindi:"));
        assertTrue(result.getLanguageRatio().contains("English:"));
    }

    @Test
    void testLanguageDetectionEnglish() {
        String english = "The quick brown fox jumps over the lazy dog in this real-time transcription service";
        LanguageDetectionService.DetectionResult result = languageDetectionService.analyze(english);
        assertEquals("English", result.getPrimaryLanguage());
    }

    @Test
    void testCrudOperations() {
        TranscriptionRequest request = new TranscriptionRequest();
        request.setTitle("Test Transcription");
        request.setContent("Yeh ek test transcription hai with English terms included");
        request.setTags("test,junit");
        request.setIsFavorite(true);

        TranscriptionResponse created = transcriptionService.createTranscription(request);
        assertNotNull(created.getId());
        assertEquals("Test Transcription", created.getTitle());
        assertTrue(created.getIsFavorite());

        TranscriptionResponse found = transcriptionService.getTranscriptionById(created.getId());
        assertEquals(created.getId(), found.getId());

        // Update
        TranscriptionRequest updateReq = new TranscriptionRequest();
        updateReq.setTitle("Updated Title");
        updateReq.setContent("Updated content with new information");
        TranscriptionResponse updated = transcriptionService.updateTranscription(created.getId(), updateReq);
        assertEquals("Updated Title", updated.getTitle());

        // Stats
        StatsResponse stats = transcriptionService.getDashboardStats();
        assertTrue(stats.getTotalTranscriptions() > 0);

        // Delete
        transcriptionService.deleteTranscription(created.getId());
        assertThrows(Exception.class, () -> transcriptionService.getTranscriptionById(created.getId()));
    }
}
