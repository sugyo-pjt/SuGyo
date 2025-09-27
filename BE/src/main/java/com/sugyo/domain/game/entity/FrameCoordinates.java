package com.sugyo.domain.game.entity;

import com.sugyo.domain.game.dto.MotionFrame;
import com.sugyo.domain.music.domain.Music;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

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
    private Double timePassed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frame_data", nullable = false, columnDefinition = "json")
    private List<MotionFrame> frameData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;
}
