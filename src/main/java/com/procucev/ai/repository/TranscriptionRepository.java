package com.procucev.ai.repository;

import com.procucev.ai.model.Transcription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {

    @Query("SELECT t FROM Transcription t WHERE " +
           "(:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:language IS NULL OR :language = '' OR t.detectedLanguage = :language) AND " +
           "(:favorite IS NULL OR t.isFavorite = :favorite)")
    Page<Transcription> searchTranscriptions(
            @Param("keyword") String keyword,
            @Param("language") String language,
            @Param("favorite") Boolean favorite,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.wordCount), 0) FROM Transcription t")
    Long sumTotalWords();

    @Query("SELECT COALESCE(SUM(t.audioDurationSeconds), 0.0) FROM Transcription t")
    Double sumTotalAudioDuration();

    @Query("SELECT t.detectedLanguage, COUNT(t) FROM Transcription t GROUP BY t.detectedLanguage")
    List<Object[]> countByLanguageGroup();

    long countByIsFavoriteTrue();

    List<Transcription> findTop5ByOrderByCreatedAtDesc();
}
