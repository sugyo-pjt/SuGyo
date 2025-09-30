package com.sugyo.domain.study.repository;

import com.sugyo.domain.study.entity.MusicVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MusicVocabularyRepository extends JpaRepository<MusicVocabulary, Long> {
    long countByMusicId(long musicId);
    List<MusicVocabulary> findAllByMusicId(long musicId);
}
