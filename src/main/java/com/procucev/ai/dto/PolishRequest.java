package com.procucev.ai.dto;

public class PolishRequest {
    private String text;
    private String mode = "clean"; // clean, devanagari, romanized, english_translation, punctuate

    public PolishRequest() {
    }

    public PolishRequest(String text, String mode) {
        this.text = text;
        this.mode = mode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
