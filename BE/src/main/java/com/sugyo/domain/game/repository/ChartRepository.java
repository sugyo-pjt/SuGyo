package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.Chart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChartRepository extends JpaRepository<Chart, Long> {

    List<Chart> findAllByMusicId(Long musicId);

}
