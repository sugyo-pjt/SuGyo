package com.sugyo.domain.game.entity;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "frame_coordinates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrameCoordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long timePassed;

    @Column(name = "frame_data", nullable = false, columnDefinition = "json")
    private String frameData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;
}
