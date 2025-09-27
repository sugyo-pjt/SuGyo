package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.ChartAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChartAnswerRepository extends JpaRepository<ChartAnswer, Long> {

    @Query("SELECT ca FROM ChartAnswer ca " +
           "JOIN ca.chart c " +
           "WHERE c.music.id = :musicId AND c.sequence = :segment")
    List<ChartAnswer> findByMusicIdAndSegment(@Param("musicId") Long musicId, @Param("segment") Integer segment);
}