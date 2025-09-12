package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.RhythmGameRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RankRepository extends JpaRepository<RhythmGameRank, Long> {
    

}