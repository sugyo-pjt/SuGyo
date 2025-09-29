package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
    Optional<GameResult> findByUserAndMusic(User user, Music music);
}
