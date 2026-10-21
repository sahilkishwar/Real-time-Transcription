package com.procucev.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcriptions")
public class Transcription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "detected_language", length = 100)
    private String detectedLanguage = "Hinglish (Hindi + English)";

    @Column(name = "language_ratio", length = 100)
    private String languageRatio;

    @Column(name = "audio_duration_seconds")
    private Double audioDurationSeconds = 0.0;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @Column(name = "character_count")
    private Integer characterCount = 0;

    @Column(name = "confidence")
    private Double confidence = 0.95;

    @Column(name = "provider", length = 100)
    private String provider = "gemini-live";

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Transcription() {
    }

    public Transcription(String title, String content, String detectedLanguage, String provider) {
        this.title = title;
        this.content = content;
        this.detectedLanguage = detectedLanguage;
        this.provider = provider;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        recalculateCounts();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateCounts();
    }

    public void recalculateCounts() {
        if (content != null) {
            String trimmed = content.trim();
            this.characterCount = trimmed.length();
            if (trimmed.isEmpty()) {
                this.wordCount = 0;
            } else {
                this.wordCount = trimmed.split("\\s+").length;
            }
        } else {
            this.wordCount = 0;
            this.characterCount = 0;
        }
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
        recalculateCounts();
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
}
