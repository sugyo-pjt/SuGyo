package com.sugyo.domain.study.repository;

import com.sugyo.domain.study.entity.MusicVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicVocabularyRepository extends JpaRepository<MusicVocabulary, Long> {
    long countByMusicId(long musicId);
}
