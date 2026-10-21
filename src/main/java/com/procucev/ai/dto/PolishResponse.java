package com.procucev.ai.dto;

public class PolishResponse {
    private String originalText;
    private String polishedText;
    private String mode;
    private String detectedLanguage;
    private String languageRatio;

    public PolishResponse() {
    }

    public PolishResponse(String originalText, String polishedText, String mode, String detectedLanguage, String languageRatio) {
        this.originalText = originalText;
        this.polishedText = polishedText;
        this.mode = mode;
        this.detectedLanguage = detectedLanguage;
        this.languageRatio = languageRatio;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getPolishedText() {
        return polishedText;
    }

    public void setPolishedText(String polishedText) {
        this.polishedText = polishedText;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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
}
