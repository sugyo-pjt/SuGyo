package com.sugyo.domain.study.repository;

import com.sugyo.domain.study.entity.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByWordContaining(String keyword);
}
