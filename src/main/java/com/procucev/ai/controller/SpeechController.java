package com.procucev.ai.controller;

import com.procucev.ai.dto.PolishRequest;
import com.procucev.ai.dto.PolishResponse;
import com.procucev.ai.service.LanguageDetectionService;
import com.procucev.ai.service.SpeechToTextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/speech")
@CrossOrigin(origins = "*")
public class SpeechController {

    private final SpeechToTextService speechToTextService;
    private final LanguageDetectionService languageDetectionService;

    public SpeechController(SpeechToTextService speechToTextService,
                            LanguageDetectionService languageDetectionService) {
        this.speechToTextService = speechToTextService;
        this.languageDetectionService = languageDetectionService;
    }

    @PostMapping("/transcribe-audio")
    public ResponseEntity<Map<String, Object>> transcribeAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestHeader(value = "X-Gemini-API-Key", required = false) String apiKeyHeader) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Uploaded audio file is empty"));
            }

            byte[] bytes = file.getBytes();
            String mimeType = file.getContentType();
            String transcript = speechToTextService.transcribeAudio(bytes, mimeType, language, apiKeyHeader);

            LanguageDetectionService.DetectionResult detection = languageDetectionService.analyze(transcript);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transcript", transcript,
                    "detectedLanguage", detection.getPrimaryLanguage(),
                    "languageRatio", detection.getLanguageRatio(),
                    "confidence", detection.getConfidence(),
                    "engine", speechToTextService.getEngineName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Transcription failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/polish")
    public ResponseEntity<PolishResponse> polishHinglish(
            @RequestBody PolishRequest request,
            @RequestHeader(value = "X-Gemini-API-Key", required = false) String apiKeyHeader) {
        try {
            String polished = speechToTextService.polishHinglishText(request.getText(), request.getMode(), apiKeyHeader);
            LanguageDetectionService.DetectionResult detection = languageDetectionService.analyze(polished);

            PolishResponse response = new PolishResponse(
                    request.getText(),
                    polished,
                    request.getMode(),
                    detection.getPrimaryLanguage(),
                    detection.getLanguageRatio()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PolishResponse(
                    request.getText(),
                    request.getText(),
                    request.getMode(),
                    "Error",
                    e.getMessage()
            ));
        }
    }

    @PostMapping("/detect-language")
    public ResponseEntity<Map<String, Object>> detectLanguage(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        LanguageDetectionService.DetectionResult result = languageDetectionService.analyze(text);
        return ResponseEntity.ok(Map.of(
                "primaryLanguage", result.getPrimaryLanguage(),
                "languageRatio", result.getLanguageRatio(),
                "confidence", result.getConfidence()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEngineStatus() {
        return ResponseEntity.ok(Map.of(
                "engine", speechToTextService.getEngineName(),
                "available", speechToTextService.isAvailable(),
                "multilingualCapabilities", List.of("Hinglish (Hindi + English)", "Hindi (हिंदी)", "English (Indian/Global)"),
                "features", List.of("Real-time browser speech recognition", "Audio file transcription", "AI Hinglish refiner", "Web Audio waveform analyzer")
        ));
    }
}
