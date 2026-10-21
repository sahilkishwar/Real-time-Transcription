package com.procucev.ai.service;

import com.procucev.ai.model.Transcription;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service dedicated to exporting transcriptions into specialized multimedia
 * subtitle (SRT, WebVTT) and structured Markdown formats with precise timing.
 */
@Service
public class ExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Formats transcription entries as SubRip (SRT) subtitles with sequential running timecodes.
     *
     * @param transcriptions list of records
     * @return SRT formatted string
     */
    public String exportToSrt(List<Transcription> transcriptions) {
        if (transcriptions == null || transcriptions.isEmpty()) {
            return "";
        }

        StringBuilder srt = new StringBuilder();
        long runningMillis = 0;
        int cueIndex = 1;

        for (Transcription item : transcriptions) {
            long durationMillis;
            if (item.getAudioDurationSeconds() != null && item.getAudioDurationSeconds() > 0) {
                durationMillis = (long) (item.getAudioDurationSeconds() * 1000);
            } else {
                int words = (item.getWordCount() != null && item.getWordCount() > 0) ? item.getWordCount() : 10;
                durationMillis = Math.max(2000, words * 400L); // ~150 words per minute
            }

            long start = runningMillis;
            long end = runningMillis + durationMillis;

            srt.append(cueIndex++).append("\n");
            srt.append(formatTimecodeSrt(start)).append(" --> ").append(formatTimecodeSrt(end)).append("\n");
            srt.append(item.getContent() != null ? item.getContent().trim() : "").append("\n\n");

            runningMillis = end + 300; // 300ms gap between cues
        }

        return srt.toString();
    }

    /**
     * Formats transcription entries as WebVTT subtitles with standard cue timings.
     *
     * @param transcriptions list of records
     * @return WebVTT formatted string
     */
    public String exportToWebVtt(List<Transcription> transcriptions) {
        StringBuilder vtt = new StringBuilder();
        vtt.append("WEBVTT - Procucev AI Speech Transcription\n\n");

        if (transcriptions == null || transcriptions.isEmpty()) {
            return vtt.toString();
        }

        long runningMillis = 0;
        int cueIndex = 1;

        for (Transcription item : transcriptions) {
            long durationMillis;
            if (item.getAudioDurationSeconds() != null && item.getAudioDurationSeconds() > 0) {
                durationMillis = (long) (item.getAudioDurationSeconds() * 1000);
            } else {
                int words = (item.getWordCount() != null && item.getWordCount() > 0) ? item.getWordCount() : 10;
                durationMillis = Math.max(2000, words * 400L);
            }

            long start = runningMillis;
            long end = runningMillis + durationMillis;

            vtt.append("cue-").append(cueIndex++).append("\n");
            vtt.append(formatTimecodeVtt(start)).append(" --> ").append(formatTimecodeVtt(end)).append("\n");
            vtt.append(item.getContent() != null ? item.getContent().trim() : "").append("\n\n");

            runningMillis = end + 250;
        }

        return vtt.toString();
    }

    /**
     * Formats transcriptions as a clean Markdown report with executive summary and per-item details.
     *
     * @param transcriptions list of records
     * @return Markdown formatted report
     */
    public String exportToMarkdown(List<Transcription> transcriptions) {
        StringBuilder md = new StringBuilder();
        md.append("# 🎙️ Procucev AI — Real-time Speech Transcription Report\n\n");
        md.append("> Generated: ").append(java.time.Instant.now()).append("\n");
        md.append("> Total Transcriptions: ").append(transcriptions != null ? transcriptions.size() : 0).append("\n\n");

        if (transcriptions == null || transcriptions.isEmpty()) {
            md.append("*No transcription records available.* \n");
            return md.toString();
        }

        int totalWords = transcriptions.stream()
                .mapToInt(t -> t.getWordCount() != null ? t.getWordCount() : 0)
                .sum();
        double totalDurationSec = transcriptions.stream()
                .mapToDouble(t -> t.getAudioDurationSeconds() != null ? t.getAudioDurationSeconds() : 0.0)
                .sum();

        md.append("### 📊 Summary Metrics\n");
        md.append("| Metric | Value |\n");
        md.append("|---|---|\n");
        md.append(String.format("| **Total Transcripts** | `%d` |\n", transcriptions.size()));
        md.append(String.format("| **Total Words Spoken** | `%d` |\n", totalWords));
        md.append(String.format("| **Total Audio Duration** | `%.2f seconds` |\n", totalDurationSec));
        md.append("\n---\n\n");

        md.append("### 📝 Detailed Transcripts\n\n");
        for (int i = 0; i < transcriptions.size(); i++) {
            Transcription item = transcriptions.get(i);
            String title = item.getTitle() != null ? item.getTitle() : "Transcription #" + (i + 1);
            String timestamp = item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_TIME_FORMATTER) : "N/A";
            String lang = item.getDetectedLanguage() != null ? item.getDetectedLanguage() : "Auto";
            String ratio = item.getLanguageRatio() != null ? item.getLanguageRatio() : "N/A";

            md.append(String.format("#### %d. %s\n", (i + 1), title));
            md.append(String.format("- **Timestamp**: `%s`\n", timestamp));
            md.append(String.format("- **Language**: `%s` (%s)\n", lang, ratio));
            md.append(String.format("- **Confidence**: `%.2f%%`\n", (item.getConfidence() != null ? item.getConfidence() * 100 : 95.0)));
            if (item.getTags() != null && !item.getTags().isBlank()) {
                md.append(String.format("- **Tags**: `%s`\n", item.getTags()));
            }
            md.append("\n```text\n");
            md.append(item.getContent() != null ? item.getContent() : "");
            md.append("\n```\n\n");
        }

        return md.toString();
    }

    /**
     * Formats milliseconds to SRT timecode format: HH:mm:ss,SSS
     */
    public static String formatTimecodeSrt(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        long ms = millis % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, ms);
    }

    /**
     * Formats milliseconds to WebVTT timecode format: HH:mm:ss.SSS
     */
    public static String formatTimecodeVtt(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        long ms = millis % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
    }
}
