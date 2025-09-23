package com.sugyo.domain.study.repository;

import com.sugyo.domain.study.entity.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByWordContaining(String keyword);
    Optional<List<Vocabulary>> findByMotionId(long motionId);
}
