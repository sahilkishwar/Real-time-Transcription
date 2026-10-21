package com.procucev.ai;

import com.procucev.ai.model.Transcription;
import com.procucev.ai.repository.TranscriptionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class RealtimeTranscriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealtimeTranscriptionApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDemoData(TranscriptionRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                // Sample 1: Authentic Hinglish Code-Mixed (Meeting)
                Transcription t1 = new Transcription();
                t1.setTitle("Sprint Planning - Backend API discussion");
                t1.setContent("Aaj hum log WebSocket endpoint implement karenge because real-time transcription latency bohot low honi chahiye. Database schema finalized hai and client side visualizer is ready.");
                t1.setNotes("Discussion with engineering lead regarding low-latency audio streaming.");
                t1.setDetectedLanguage("Hinglish (Hindi + English)");
                t1.setLanguageRatio("Hindi: 42% | English: 58%");
                t1.setAudioDurationSeconds(12.4);
                t1.setConfidence(0.96);
                t1.setProvider("gemini-live");
                t1.setTags("sprint,backend,websocket,hinglish");
                t1.setIsFavorite(true);
                repository.save(t1);

                // Sample 2: Hinglish Daily Standup
                Transcription t2 = new Transcription();
                t2.setTitle("Daily Standup - Deployment updates");
                t2.setContent("Kal sham ko server restart ho gaya tha par abhi Docker container perfectly run kar raha hai. Main aaj evening tak cloud deployment link share kar dunga.");
                t2.setNotes("DevOps update on Docker and Render cloud deployment.");
                t2.setDetectedLanguage("Hinglish (Hindi + English)");
                t2.setLanguageRatio("Hindi: 50% | English: 50%");
                t2.setAudioDurationSeconds(9.8);
                t2.setConfidence(0.94);
                t2.setProvider("browser-live-speech");
                t2.setTags("standup,devops,docker");
                t2.setIsFavorite(false);
                repository.save(t2);

                // Sample 3: Hindi Devanagari with English loanwords
                Transcription t3 = new Transcription();
                t3.setTitle("Customer Feedback Review");
                t3.setContent("ग्राहक ने application की performance और speech recognition accuracy की बहुत प्रशंसा की है। Next release में हमें multilingual export feature जोड़ना होगा।");
                t3.setNotes("Product feedback review for multilingual speech-to-text accuracy.");
                t3.setDetectedLanguage("Hinglish (Devanagari + English)");
                t3.setLanguageRatio("Hindi: 65% | English: 35%");
                t3.setAudioDurationSeconds(15.2);
                t3.setConfidence(0.97);
                t3.setProvider("gemini-live");
                t3.setTags("feedback,release,multilingual");
                t3.setIsFavorite(true);
                repository.save(t3);

                // Sample 4: English Technical Demo
                Transcription t4 = new Transcription();
                t4.setTitle("Architecture Walkthrough");
                t4.setContent("The Spring Boot application manages bidirectional audio streaming using WebSockets and stores persistent transcription records in a relational database with full CRUD capabilities.");
                t4.setNotes("Technical overview of Spring Boot 3 + JPA architecture.");
                t4.setDetectedLanguage("English");
                t4.setLanguageRatio("Hindi: 0% | English: 100%");
                t4.setAudioDurationSeconds(11.1);
                t4.setConfidence(0.99);
                t4.setProvider("browser-live-speech");
                t4.setTags("architecture,springboot,jpa");
                t4.setIsFavorite(false);
                repository.save(t4);
            }
        };
    }
}
