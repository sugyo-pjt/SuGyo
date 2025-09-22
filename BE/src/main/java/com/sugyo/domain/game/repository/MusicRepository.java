package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.Music;
import com.sugyo.domain.game.dto.response.MusicWithScoreDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {
    @Query("SELECT new com.sugyo.domain.game.dto.response.MusicWithScoreDto(" +
           "m.id, m.title, m.singer, m.songTime, m.albumImageUrl, " +
           "MAX(r.score)) " +
           "FROM Music m LEFT JOIN RhythmGameRank r ON m.id = r.music.id AND r.user.id = :userId " +
           "GROUP BY m.id, m.title, m.singer, m.songTime, m.albumImageUrl " +
           "ORDER BY m.id")
    List<MusicWithScoreDto> findAllMusicWithUserScore(@Param("userId") Long userId);
}