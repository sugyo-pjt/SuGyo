package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.music.domain.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FrameCoordinatesRepository extends JpaRepository<FrameCoordinates, Long> {

    @Query("SELECT fc FROM FrameCoordinates fc WHERE fc.music.id = :musicId AND fc.timePassed = :timePassed")
    Optional<FrameCoordinates> findByMusicIdAndTimePassed(@Param("musicId") Long musicId, @Param("timePassed") Double timePassed);

    @Query("SELECT fc FROM FrameCoordinates fc WHERE fc.music.id = :musicId " +
            "AND fc.timePassed >= :startTime AND fc.timePassed <= :endTime " +
            "ORDER BY fc.timePassed ASC")
    List<FrameCoordinates> findByMusicIdAndTimeRange(
            @Param("musicId") Long musicId,
            @Param("startTime") Double startTime,
            @Param("endTime") Double endTime);

    Optional<FrameCoordinates> findTop1ByMusicIdOrderByTimePassedDesc(Long musicId);

    List<FrameCoordinates> findByMusic(Music music);
}
