package com.sugyo.domain.game.entity;

import com.sugyo.common.domain.BaseTimeEntity;
import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer score;

    public static GameResult create(User user, Music music, int score) {
        return GameResult.builder()
                .music(music)
                .user(user)
                .score(score)
                .build();
    }

    public boolean updateScoreIfHigher(int newScore) {
        if (this.score < newScore) {
            this.score = newScore;
            return true;
        }
        return false;
    }
}

