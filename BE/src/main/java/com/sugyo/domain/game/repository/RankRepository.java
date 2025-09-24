package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RankRepository extends JpaRepository<GameResult, Long> {

    @Query("SELECT r FROM RhythmGameRank r WHERE r.music.id = :musicId ORDER BY r.score DESC")
    List<GameResult> findTop5ByMusicIdOrderByScoreDesc(@Param("musicId") Long musicId);

    @Query("SELECT r FROM RhythmGameRank r WHERE r.music.id = :musicId AND r.user.id = :userId ORDER BY r.score DESC")
    Optional<GameResult> findTopByMusicIdAndUserIdOrderByScoreDesc(@Param("musicId") Long musicId, @Param("userId") Long userId);

    @Query("SELECT COUNT(r2) + 1 FROM RhythmGameRank r2 WHERE r2.music.id = :musicId AND r2.score > :score")
    Integer findRankByMusicIdAndScore(@Param("musicId") Long musicId, @Param("score") Integer score);

    @Query("SELECT r FROM RhythmGameRank r WHERE r.music.id = :musicId AND r.user.id = :userId")
    Optional<GameResult> findByMusicIdAndUserId(@Param("musicId") Long musicId, @Param("userId") Long userId);
}
