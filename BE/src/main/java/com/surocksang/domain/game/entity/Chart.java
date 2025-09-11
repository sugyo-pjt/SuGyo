package com.surocksang.domain.game.entity;

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

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart_id")
    private List<ChartAnswer> chartAnswers;
}