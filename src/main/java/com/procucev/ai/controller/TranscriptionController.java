package com.procucev.ai.controller;

import com.procucev.ai.dto.StatsResponse;
import com.procucev.ai.dto.TranscriptionRequest;
import com.procucev.ai.dto.TranscriptionResponse;
import com.procucev.ai.service.TranscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transcriptions")
@CrossOrigin(origins = "*")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping
    public ResponseEntity<TranscriptionResponse> createTranscription(@Valid @RequestBody TranscriptionRequest request) {
        TranscriptionResponse created = transcriptionService.createTranscription(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TranscriptionResponse>> getTranscriptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<TranscriptionResponse> results = transcriptionService.getTranscriptions(
                keyword, language, favorite, page, size, sortBy, sortDir);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> getTranscriptionById(@PathVariable Long id) {
        TranscriptionResponse response = transcriptionService.getTranscriptionById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> updateTranscription(
            @PathVariable Long id,
            @RequestBody TranscriptionRequest request) {
        TranscriptionResponse updated = transcriptionService.updateTranscription(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<TranscriptionResponse> toggleFavorite(@PathVariable Long id) {
        TranscriptionResponse updated = transcriptionService.toggleFavorite(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTranscription(@PathVariable Long id) {
        transcriptionService.deleteTranscription(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Transcription " + id + " deleted successfully"
        ));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        int deletedCount = transcriptionService.batchDelete(ids);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deletedCount,
                "message", deletedCount + " transcriptions deleted successfully"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getDashboardStats() {
        StatsResponse stats = transcriptionService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportTranscriptions(@RequestParam(defaultValue = "json") String format) {
        try {
            String content = transcriptionService.exportTranscriptions(format);
            String filename = "transcriptions-" + System.currentTimeMillis();
            MediaType mediaType = MediaType.APPLICATION_JSON;

            if ("csv".equalsIgnoreCase(format)) {
                mediaType = MediaType.parseMediaType("text/csv");
                filename += ".csv";
            } else if ("txt".equalsIgnoreCase(format)) {
                mediaType = MediaType.TEXT_PLAIN;
                filename += ".txt";
            } else if ("srt".equalsIgnoreCase(format)) {
                mediaType = MediaType.TEXT_PLAIN;
                filename += ".srt";
            } else {
                filename += ".json";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to export: " + e.getMessage());
        }
    }
}
