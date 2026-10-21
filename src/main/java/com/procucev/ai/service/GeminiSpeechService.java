package com.procucev.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class GeminiSpeechService implements SpeechToTextService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSpeechService.class);

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiSpeechService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY");
    }

    @Override
    public String getEngineName() {
        return "Google Gemini Multilingual Audio Engine (" + model + ")";
    }

    @Override
    public String transcribeAudio(byte[] audioData, String mimeType, String languageHint) throws Exception {
        return transcribeAudio(audioData, mimeType, languageHint, null);
    }

    @Override
    public String transcribeAudio(byte[] audioData, String mimeType, String languageHint, String apiKeyOverride) throws Exception {
        String activeKey = (apiKeyOverride != null && !apiKeyOverride.trim().isEmpty()) ? apiKeyOverride.trim() : this.apiKey;

        if (activeKey == null || activeKey.trim().isEmpty() || activeKey.equals("YOUR_GEMINI_API_KEY")) {
            throw new IllegalStateException("Google Gemini API key is not configured on the server. Please click the 🔑 key icon in the navbar to enter your Gemini API key for backend audio transcription.");
        }

        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be empty");
        }

        String base64Audio = Base64.getEncoder().encodeToString(audioData);
        String cleanMimeType = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "audio/webm";

        // Prompt specifically tailored for code-mixed Hindi & English (Hinglish)
        String prompt = "You are an expert multilingual speech-to-text transcription engine specialized in code-switching and code-mixing between Hindi and English (Hinglish). " +
                "Transcribe this audio clip exactly as spoken. " +
                "If the speaker uses Hindi words (e.g. 'aaj', 'kal', 'haan', 'theek hai', 'batao', 'kaise ho', 'kya scene hai') mixed with English words, " +
                "accurately transcribe both. Transcribe conversational Hindi in standard Romanized transliteration (or Devanagari if the speaker spoke formally) alongside English. " +
                "Do NOT add preamble, commentary, timestamps, or markdown formatting. Output ONLY the transcribed text.";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt),
                                        Map.of("inline_data", Map.of(
                                                "mime_type", cleanMimeType,
                                                "data", base64Audio
                                        ))
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 1024
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, activeKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String text = parts.get(0).path("text").asText("");
                    return text.trim();
                }
            }
            return "";
        } else {
            log.error("Gemini API error ({}): {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API error: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    @Override
    public String polishHinglishText(String text, String mode) throws Exception {
        return polishHinglishText(text, mode, null);
    }

    @Override
    public String polishHinglishText(String text, String mode, String apiKeyOverride) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String activeKey = (apiKeyOverride != null && !apiKeyOverride.trim().isEmpty()) ? apiKeyOverride.trim() : this.apiKey;
        if (activeKey == null || activeKey.trim().isEmpty() || activeKey.equals("YOUR_GEMINI_API_KEY")) {
            return localRuleBasedPolish(text, mode);
        }

        String instruction;
        switch (mode.toLowerCase()) {
            case "devanagari":
                instruction = "Convert the Hindi portions of this Hinglish sentence to authentic Devanagari script (हिंदी) while preserving English loanwords/technical words in English alphabet. Text: ";
                break;
            case "romanized":
                instruction = "Standardize and clean up this Hinglish sentence in Romanized script. Fix phonetic typos and stuttering while keeping the original colloquial tone. Text: ";
                break;
            case "english_translation":
                instruction = "Translate this Hinglish (Hindi + English mixture) sentence accurately into fluent, natural English. Text: ";
                break;
            case "punctuate":
                instruction = "Add appropriate punctuation (periods, commas, question marks, capitalization) to this speech transcript without changing words. Text: ";
                break;
            case "clean":
            default:
                instruction = "Clean up and standardize this real-time multilingual Hinglish transcript. Fix speech disfluencies (um, uh, repeated words), apply correct punctuation and capitalization, and format clearly. Text: ";
                break;
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", instruction + text + "\nOutput ONLY the refined text.")
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 512
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, activeKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("").trim();
                }
            }
        }

        return localRuleBasedPolish(text, mode);
    }

    private String localRuleBasedPolish(String text, String mode) {
        String clean = text.trim();
        // Capitalize first letter
        if (!clean.isEmpty()) {
            clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        }
        // Ensure ends with period or question mark
        if (!clean.isEmpty() && !clean.endsWith(".") && !clean.endsWith("?") && !clean.endsWith("!")) {
            clean = clean + ".";
        }
        return clean;
    }

    private static final List<String> HINGLISH_FALLBACK_TRANSCRIPTS = List.of(
            "Haan bhai, real-time transcription microphone stream perfectly capture ho gayi hai with mixed Hindi and English.",
            "Aaj hum log meeting discuss karenge regarding deployment and real-time audio visualization.",
            "Backend server par audio chunks successfully receive ho gaye hain with zero packet loss.",
            "Project architecture is ready aur client dashboard CRUD operations smoothly synchronize ho rahe hain."
    );
    private static final java.util.concurrent.atomic.AtomicInteger fallbackIndex = new java.util.concurrent.atomic.AtomicInteger(0);

    private String simulateTranscription(String languageHint) {
        int idx = Math.abs(fallbackIndex.getAndIncrement() % HINGLISH_FALLBACK_TRANSCRIPTS.size());
        return HINGLISH_FALLBACK_TRANSCRIPTS.get(idx);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
