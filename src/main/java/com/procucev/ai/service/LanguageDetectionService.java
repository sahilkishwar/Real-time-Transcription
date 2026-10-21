package com.procucev.ai.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class LanguageDetectionService {

    private static final Pattern DEVANAGARI_PATTERN = Pattern.compile("[\u0900-\u097F]");

    private static final Set<String> COMMON_HINDI_ROMAN_WORDS = new HashSet<>(Arrays.asList(
            "hai", "hain", "kya", "kyu", "kyun", "kaise", "kaisa", "kaisi", "nahi", "nahin", "na",
            "bahut", "bohot", "achha", "accha", "acchi", "theek", "thik", "kar", "karna", "karte",
            "karein", "kare", "kiya", "raha", "rahe", "rahi", "hoga", "hogi", "hoge", "tha", "thi", "the",
            "aaj", "kal", "parso", "ab", "kab", "jab", "tab", "hum", "main", "tum", "aap", "mera", "meri",
            "mere", "tera", "teri", "tere", "uska", "uski", "unka", "unki", "is", "us", "yeh", "woh",
            "ye", "wo", "sab", "kuch", "koi", "aur", "lekin", "magar", "par", "bhi", "toh", "to",
            "matlab", "shukriya", "dhanyawad", "namaste", "bhai", "yaar", "paas", "door", "upar",
            "neeche", "aage", "peeche", "chalo", "dekho", "suno", "bolo", "baat", "kaam", "waqt",
            "samay", "dost", "ghar", "gaadi", "paani", "khana", "piya", "milte", "samajh", "aaya",
            "aayi", "aaye", "gaya", "gayi", "gaye", "chal", "ruk", "sahi", "galat", "pakka", "zaroor",
            "shayad", "sirf", "batao", "bataiye", "jaise", "waise", "humein", "tumhein", "unhein"
    ));

    private static final Set<String> COMMON_ENGLISH_WORDS = new HashSet<>(Arrays.asList(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on",
            "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we",
            "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their",
            "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when",
            "make", "can", "like", "time", "no", "just", "him", "know", "take", "people", "into",
            "year", "your", "good", "some", "could", "them", "see", "other", "than", "then", "now",
            "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two",
            "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any",
            "these", "give", "day", "most", "us", "meeting", "project", "code", "client", "office",
            "deadline", "call", "email", "server", "deploy", "application", "database", "urgent"
    ));

    private static final Set<String> SHARED_HOMOGRAPHS = new HashSet<>(Arrays.asList(
            "is", "us", "to", "me", "so", "no", "do", "par", "man", "bar"
    ));

    public static class DetectionResult {
        private final String primaryLanguage;
        private final String languageRatio;
        private final double confidence;

        public DetectionResult(String primaryLanguage, String languageRatio, double confidence) {
            this.primaryLanguage = primaryLanguage;
            this.languageRatio = languageRatio;
            this.confidence = confidence;
        }

        public String getPrimaryLanguage() {
            return primaryLanguage;
        }

        public String getLanguageRatio() {
            return languageRatio;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public DetectionResult analyze(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new DetectionResult("Unknown", "N/A", 0.0);
        }

        String[] tokens = text.toLowerCase().split("[^\\p{L}0-9]+");
        int devanagariCount = 0;
        int unambiguousHindiCount = 0;
        int unambiguousEnglishCount = 0;
        int ambiguousCount = 0;

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (DEVANAGARI_PATTERN.matcher(token).find()) {
                devanagariCount++;
            } else if (SHARED_HOMOGRAPHS.contains(token)) {
                ambiguousCount++;
            } else if (COMMON_HINDI_ROMAN_WORDS.contains(token)) {
                unambiguousHindiCount++;
            } else if (COMMON_ENGLISH_WORDS.contains(token)) {
                unambiguousEnglishCount++;
            } else if (token.matches("^[a-z]+$")) {
                // Latin character token - default leaning English
                unambiguousEnglishCount++;
            }
        }

        // Disambiguate homographs
        int finalHindi = devanagariCount + unambiguousHindiCount;
        int finalEnglish = unambiguousEnglishCount;

        if (devanagariCount == 0 && unambiguousHindiCount == 0) {
            finalHindi = 0;
            finalEnglish = Math.max(1, unambiguousEnglishCount + ambiguousCount);
        } else if (finalHindi > finalEnglish) {
            finalHindi += ambiguousCount;
        } else {
            finalEnglish += ambiguousCount;
        }

        int total = finalHindi + finalEnglish;
        if (total == 0) {
            return new DetectionResult("English", "Hindi: 0% | English: 100%", 0.9);
        }

        int hindiPercent = (int) Math.round(((double) finalHindi / total) * 100);
        int englishPercent = 100 - hindiPercent;

        String ratio = String.format("Hindi: %d%% | English: %d%%", hindiPercent, englishPercent);
        String language;
        double confidence;

        if (devanagariCount > 0 && englishPercent >= 15) {
            language = "Hinglish (Devanagari + English)";
            confidence = 0.96;
        } else if (hindiPercent >= 15 && englishPercent >= 15) {
            language = "Hinglish (Hindi + English)";
            confidence = 0.94;
        } else if (hindiPercent >= 85) {
            language = devanagariCount > 0 ? "Hindi (हिंदी)" : "Hindi (Romanized)";
            confidence = 0.95;
        } else {
            language = "English";
            confidence = 0.95;
        }

        return new DetectionResult(language, ratio, confidence);
    }
}
