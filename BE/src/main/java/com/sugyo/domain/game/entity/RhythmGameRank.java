package com.sugyo.domain.game.entity;

import com.sugyo.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rhythm_game_rank")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RhythmGameRank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Integer score;
    
    @Column(name = "record_time", nullable = false)
    @CreationTimestamp
    private LocalDateTime recordTime;
}

