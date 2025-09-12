package com.sugyo.domain.study.repository;

import com.sugyo.domain.study.entity.UserDailyVocabulary;
import com.sugyo.domain.study.dto.response.DayProgressDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyVocabularyRepository extends JpaRepository<UserDailyVocabulary, Long> {
    
    List<UserDailyVocabulary> findByUserId(Long userId);
    
    @Query("SELECT new com.sugyo.domain.study.dto.response.DayProgressDto(d.id, d.day, u.correctCount, d.totalCount) " +
            "FROM Daily d " +
            "LEFT JOIN UserDailyVocabulary u ON d.id = u.daily.id AND u.user.id = :userId " +
            "ORDER BY d.day ASC")
    List<DayProgressDto> findDayProgressByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(d.day) " +
            "FROM UserDailyVocabulary u " +
            "JOIN u.daily d " +
            "WHERE u.user.id = :userId")
    Integer findMaxProgressDayByUserId(@Param("userId") Long userId);
}