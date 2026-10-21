package com.procucev.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procucev.ai.dto.StatsResponse;
import com.procucev.ai.dto.TranscriptionRequest;
import com.procucev.ai.dto.TranscriptionResponse;
import com.procucev.ai.model.Transcription;
import com.procucev.ai.repository.TranscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TranscriptionService {

    private final TranscriptionRepository repository;
    private final LanguageDetectionService languageDetectionService;
    private final ObjectMapper objectMapper;

    public TranscriptionService(TranscriptionRepository repository,
                                LanguageDetectionService languageDetectionService,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.languageDetectionService = languageDetectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TranscriptionResponse createTranscription(TranscriptionRequest request) {
        Transcription transcription = new Transcription();
        transcription.setContent(request.getContent().trim());

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            transcription.setTitle(request.getTitle().trim());
        } else {
            // Generate clean title from first few words
            String content = transcription.getContent();
            String autoTitle = content.length() > 40 ? content.substring(0, 40) + "..." : content;
            transcription.setTitle(autoTitle);
        }

        transcription.setNotes(request.getNotes());
        transcription.setProvider(request.getProvider() != null ? request.getProvider() : "browser-live-speech");
        transcription.setAudioDurationSeconds(request.getAudioDurationSeconds() != null ? request.getAudioDurationSeconds() : 0.0);
        transcription.setConfidence(request.getConfidence() != null ? request.getConfidence() : 0.95);
        transcription.setTags(request.getTags());
        transcription.setIsFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false);

        // Multilingual Analysis
        LanguageDetectionService.DetectionResult detection = languageDetectionService.analyze(transcription.getContent());
        if (request.getDetectedLanguage() != null && !request.getDetectedLanguage().trim().isEmpty() && !request.getDetectedLanguage().equalsIgnoreCase("Auto")) {
            transcription.setDetectedLanguage(request.getDetectedLanguage());
        } else {
            transcription.setDetectedLanguage(detection.getPrimaryLanguage());
        }

        transcription.setLanguageRatio(detection.getLanguageRatio());

        Transcription saved = repository.save(transcription);
        return TranscriptionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<TranscriptionResponse> getTranscriptions(String keyword, String language, Boolean favorite,
                                                        int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort sort = Sort.by(direction, sortProperty);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Page<Transcription> result = repository.searchTranscriptions(
                (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null,
                (language != null && !language.trim().isEmpty() && !"all".equalsIgnoreCase(language)) ? language.trim() : null,
                favorite,
                pageable
        );

        return result.map(TranscriptionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TranscriptionResponse getTranscriptionById(Long id) {
        Transcription transcription = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transcription not found with ID: " + id));
        return TranscriptionResponse.fromEntity(transcription);
    }

    @Transactional
    public TranscriptionResponse updateTranscription(Long id, TranscriptionRequest request) {
        Transcription existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transcription not found with ID: " + id));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            existing.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            existing.setContent(request.getContent().trim());
            // Re-analyze language if content changed
            LanguageDetectionService.DetectionResult detection = languageDetectionService.analyze(existing.getContent());
            existing.setDetectedLanguage(detection.getPrimaryLanguage());
            existing.setLanguageRatio(detection.getLanguageRatio());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }
        if (request.getTags() != null) {
            existing.setTags(request.getTags());
        }
        if (request.getIsFavorite() != null) {
            existing.setIsFavorite(request.getIsFavorite());
        }
        if (request.getDetectedLanguage() != null && !request.getDetectedLanguage().trim().isEmpty()) {
            existing.setDetectedLanguage(request.getDetectedLanguage());
        }

        Transcription updated = repository.save(existing);
        return TranscriptionResponse.fromEntity(updated);
    }

    @Transactional
    public TranscriptionResponse toggleFavorite(Long id) {
        Transcription existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transcription not found with ID: " + id));
        existing.setIsFavorite(!Boolean.TRUE.equals(existing.getIsFavorite()));
        return TranscriptionResponse.fromEntity(repository.save(existing));
    }

    @Transactional
    public void deleteTranscription(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Transcription not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Transcription> toDelete = repository.findAllById(ids);
        repository.deleteAll(toDelete);
        return toDelete.size();
    }

    @Transactional
    public long deleteAllTranscriptions() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }

    @Transactional(readOnly = true)
    public StatsResponse getDashboardStats() {
        long total = repository.count();
        long totalWords = repository.sumTotalWords();
        double totalDuration = repository.sumTotalAudioDuration();

        List<Object[]> languageGroups = repository.countByLanguageGroup();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (Object[] row : languageGroups) {
            String lang = (String) row[0];
            Long count = (Long) row[1];
            if (lang != null) {
                distribution.put(lang, count);
            }
        }

        int favoriteCount = (int) repository.countByIsFavoriteTrue();

        return new StatsResponse(total, totalWords, totalDuration, distribution, favoriteCount);
    }

    @Transactional(readOnly = true)
    public String exportTranscriptions(String format) throws Exception {
        List<Transcription> all = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        String cleanFormat = (format != null) ? format.toLowerCase() : "json";

        switch (cleanFormat) {
            case "csv":
                StringBuilder csv = new StringBuilder();
                csv.append("ID,Title,Content,Language,Language Ratio,Word Count,Duration (s),Created At,Tags\n");
                for (Transcription t : all) {
                    csv.append(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%.1f\",\"%s\",\"%s\"\n",
                            t.getId(),
                            escapeCsv(t.getTitle()),
                            escapeCsv(t.getContent()),
                            escapeCsv(t.getDetectedLanguage()),
                            escapeCsv(t.getLanguageRatio()),
                            t.getWordCount() != null ? t.getWordCount() : 0,
                            t.getAudioDurationSeconds() != null ? t.getAudioDurationSeconds() : 0.0,
                            t.getCreatedAt() != null ? t.getCreatedAt().toString() : "",
                            escapeCsv(t.getTags() != null ? t.getTags() : "")
                    ));
                }
                return csv.toString();

            case "txt":
                StringBuilder txt = new StringBuilder();
                txt.append("=========================================\n");
                txt.append(" PROCUCEV AI - TRANSCRIPTION HISTORY\n");
                txt.append(" Generated: ").append(new Date()).append("\n");
                txt.append(" Total Records: ").append(all.size()).append("\n");
                txt.append("=========================================\n\n");
                for (Transcription t : all) {
                    txt.append("[").append(t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("] ")
                       .append(t.getTitle()).append("\n")
                       .append("Language: ").append(t.getDetectedLanguage()).append(" (").append(t.getLanguageRatio()).append(")\n")
                       .append("Transcript: ").append(t.getContent()).append("\n")
                       .append("--------------------------------------------------\n\n");
                }
                return txt.toString();

            case "srt":
                StringBuilder srt = new StringBuilder();
                int index = 1;
                for (Transcription t : all) {
                    srt.append(index++).append("\n");
                    srt.append("00:00:00,000 --> 00:00:05,000\n");
                    srt.append(t.getContent()).append("\n\n");
                }
                return srt.toString();

            case "json":
            default:
                List<TranscriptionResponse> dtos = all.stream()
                        .map(TranscriptionResponse::fromEntity)
                        .collect(Collectors.toList());
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dtos);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
