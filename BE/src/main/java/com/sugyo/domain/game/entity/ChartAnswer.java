package com.sugyo.domain.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "chart_answer")
@Getter
@Setter
@NoArgsConstructor
public class ChartAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalTime startedAt;
    
    @Column(nullable = false)
    private LocalTime endedAt;
    
    @Column(nullable = false)
    private Integer startedIndex;
    
    @Column(nullable = false)
    private Integer endedIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart_id", nullable = false)
    private Chart chart;
}