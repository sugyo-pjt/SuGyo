package com.surocksang.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicChartResponse {
    
    private Long musicId;
    private String title;
    private String singer;
    private LocalTime songTime;
    private String albumImageUrl;
    private List<ChartDto> charts;
}