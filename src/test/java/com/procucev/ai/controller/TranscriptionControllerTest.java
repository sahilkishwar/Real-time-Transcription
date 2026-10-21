package com.procucev.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procucev.ai.dto.TranscriptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver"
})
@AutoConfigureMockMvc
@DisplayName("TranscriptionController Integration Tests - CRUD & REST Endpoints")
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/transcriptions - Should return paginated list of transcriptions")
    void testGetTranscriptions() throws Exception {
        mockMvc.perform(get("/api/transcriptions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/transcriptions/stats - Should return overview metrics")
    void testGetStats() throws Exception {
        mockMvc.perform(get("/api/transcriptions/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTranscriptions").isNumber())
                .andExpect(jsonPath("$.totalWords").isNumber());
    }

    @Test
    @DisplayName("POST /api/transcriptions - Should create and persist new Hinglish transcription")
    void testCreateTranscription() throws Exception {
        TranscriptionRequest request = new TranscriptionRequest();
        request.setTitle("Integration Test Meeting");
        request.setContent("Aaj hum log integration tests verify karenge with MockMvc and H2 database.");
        request.setNotes("Automated testing suite verification.");
        request.setTags("test,junit,mockmvc");
        request.setProvider("junit-test");

        mockMvc.perform(post("/api/transcriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Integration Test Meeting"))
                .andExpect(jsonPath("$.detectedLanguage").exists());
    }

    @Test
    @DisplayName("GET /api/transcriptions/export - Should export records in JSON format")
    void testExportJson() throws Exception {
        mockMvc.perform(get("/api/transcriptions/export?format=json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".json")));
    }
}
