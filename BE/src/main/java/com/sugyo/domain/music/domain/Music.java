package com.sugyo.domain.music.domain;

import com.sugyo.domain.game.entity.Chart;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Music {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String singer;
    
    @Column(nullable = false)
    private LocalTime songTime;
    
    @Column
    private String albumImageUrl;

    @Column
    private String songUrl;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id")
    private List<Chart> chart;
}
