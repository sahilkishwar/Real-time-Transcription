package com.procucev.ai.dto;

import java.util.Map;

public class StatsResponse {

    private long totalTranscriptions;
    private long totalWords;
    private double totalDurationSeconds;
    private Map<String, Long> languageDistribution;
    private int favoriteCount;

    public StatsResponse() {
    }

    public StatsResponse(long totalTranscriptions, long totalWords, double totalDurationSeconds, Map<String, Long> languageDistribution, int favoriteCount) {
        this.totalTranscriptions = totalTranscriptions;
        this.totalWords = totalWords;
        this.totalDurationSeconds = totalDurationSeconds;
        this.languageDistribution = languageDistribution;
        this.favoriteCount = favoriteCount;
    }

    public long getTotalTranscriptions() {
        return totalTranscriptions;
    }

    public void setTotalTranscriptions(long totalTranscriptions) {
        this.totalTranscriptions = totalTranscriptions;
    }

    public long getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(long totalWords) {
        this.totalWords = totalWords;
    }

    public double getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(double totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public Map<String, Long> getLanguageDistribution() {
        return languageDistribution;
    }

    public void setLanguageDistribution(Map<String, Long> languageDistribution) {
        this.languageDistribution = languageDistribution;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }
}
