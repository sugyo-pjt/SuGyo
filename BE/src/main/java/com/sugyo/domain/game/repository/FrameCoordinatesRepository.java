package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.FrameCoordinates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrameCoordinatesRepository extends JpaRepository<FrameCoordinates, Long> {
}