package com.procucev.ai.service;

public interface SpeechToTextService {

    /**
     * Transcribe raw audio bytes into text.
     *
     * @param audioData    raw bytes of the audio (e.g. WAV, WebM, MP3, OGG)
     * @param mimeType     MIME type of the audio, e.g. "audio/webm", "audio/wav"
     * @param languageHint optional hint such as "hi-IN", "en-IN", "hinglish"
     * @return transcribed text
     */
    String transcribeAudio(byte[] audioData, String mimeType, String languageHint) throws Exception;

    default String transcribeAudio(byte[] audioData, String mimeType, String languageHint, String apiKeyOverride) throws Exception {
        return transcribeAudio(audioData, mimeType, languageHint);
    }

    String polishHinglishText(String text, String mode) throws Exception;

    default String polishHinglishText(String text, String mode, String apiKeyOverride) throws Exception {
        return polishHinglishText(text, mode);
    }

    /**
     * Whether the backend AI engine is configured with a valid API key.
     */
    boolean isAvailable();

    /**
     * Engine display name (e.g. "Google Gemini Multilingual STT", "Local Whisper STT", etc.)
     */
    String getEngineName();
}
