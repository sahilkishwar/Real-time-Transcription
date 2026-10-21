package com.procucev.ai.dto;

import com.procucev.ai.model.Transcription;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TranscriptionResponse {

    private Long id;
    private String title;
    private String content;
    private String notes;
    private String detectedLanguage;
    private String languageRatio;
    private Double audioDurationSeconds;
    private Integer wordCount;
    private Integer characterCount;
    private Double confidence;
    private String provider;
    private String tags;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String formattedCreatedAt;
    private String previewSnippet;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    public TranscriptionResponse() {
    }

    public static TranscriptionResponse fromEntity(Transcription entity) {
        TranscriptionResponse response = new TranscriptionResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setContent(entity.getContent());
        response.setNotes(entity.getNotes());
        response.setDetectedLanguage(entity.getDetectedLanguage());
        response.setLanguageRatio(entity.getLanguageRatio());
        response.setAudioDurationSeconds(entity.getAudioDurationSeconds());
        response.setWordCount(entity.getWordCount());
        response.setCharacterCount(entity.getCharacterCount());
        response.setConfidence(entity.getConfidence());
        response.setProvider(entity.getProvider());
        response.setTags(entity.getTags());
        response.setIsFavorite(entity.getIsFavorite());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCreatedAt() != null) {
            response.setFormattedCreatedAt(entity.getCreatedAt().format(FORMATTER));
        }

        if (entity.getContent() != null) {
            String clean = entity.getContent().trim();
            if (clean.length() > 140) {
                response.setPreviewSnippet(clean.substring(0, 140) + "...");
            } else {
                response.setPreviewSnippet(clean);
            }
        }

        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }

    public void setFormattedCreatedAt(String formattedCreatedAt) {
        this.formattedCreatedAt = formattedCreatedAt;
    }

    public String getPreviewSnippet() {
        return previewSnippet;
    }

    public void setPreviewSnippet(String previewSnippet) {
        this.previewSnippet = previewSnippet;
    }
}
