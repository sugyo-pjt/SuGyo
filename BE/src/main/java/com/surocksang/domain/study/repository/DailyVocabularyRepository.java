package com.surocksang.domain.study.repository;

import com.surocksang.domain.study.entity.DailyVocabulary;
import com.surocksang.domain.study.dto.response.StudyWordItemDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyVocabularyRepository extends JpaRepository<DailyVocabulary, Long> {
    
    @Query("SELECT new com.surocksang.domain.study.dto.response.StudyWordItemDto(" +
           "v.id, v.word, v.description, v.videoUrl) " +
           "FROM DailyVocabulary dv " +
           "JOIN dv.vocabulary v " +
           "WHERE dv.daily.id = :dailyId")
    List<StudyWordItemDto> findWordItemsByDailyId(@Param("dailyId") Long dailyId);
}