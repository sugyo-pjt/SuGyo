package com.sugyo.domain.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "chart")
@Getter
@Setter
@NoArgsConstructor
public class Chart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer sequence;
    
    @Column(nullable = false, length = 50)
    private String lyrics;
    
    @Column(nullable = false)
    private LocalTime startedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;

    @OneToMany(mappedBy = "chart", fetch = FetchType.LAZY)
    private List<ChartAnswer> chartAnswers;
}