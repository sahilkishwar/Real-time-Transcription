package com.procucev.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class TranscriptionRequest {

    private String title;

    @NotBlank(message = "Transcription content cannot be empty")
    private String content;

    private String notes;
    private String detectedLanguage;
    private String languageRatio;
    private Double audioDurationSeconds;
    private Double confidence;
    private String provider;
    private String tags;
    private Boolean isFavorite;

    public TranscriptionRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }

    public String getLanguageRatio() {
        return languageRatio;
    }

    public void setLanguageRatio(String languageRatio) {
        this.languageRatio = languageRatio;
    }

    public Double getAudioDurationSeconds() {
        return audioDurationSeconds;
    }

    public void setAudioDurationSeconds(Double audioDurationSeconds) {
        this.audioDurationSeconds = audioDurationSeconds;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Boolean favorite) {
        isFavorite = favorite;
    }
}
