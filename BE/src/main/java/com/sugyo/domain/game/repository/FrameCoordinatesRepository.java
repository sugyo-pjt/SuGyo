package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.FrameCoordinates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrameCoordinatesRepository extends JpaRepository<FrameCoordinates, Long> {

    @Query("SELECT fc FROM FrameCoordinates fc WHERE fc.music.id = :musicId AND fc.timePassed = :timePassed")
    Optional<FrameCoordinates> findByMusicIdAndTimePassed(@Param("musicId") Long musicId, @Param("timePassed") Double timePassed);

    Optional<FrameCoordinates> findTop1ByMusicIdOrderByTimePassedDesc(Long musicId);
}