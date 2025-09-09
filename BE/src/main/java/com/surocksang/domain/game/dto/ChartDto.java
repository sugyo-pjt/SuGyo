package com.surocksang.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChartDto {
    
    private Long id;
    private Integer sequence;
    private String lyrics;
    private LocalTime startedAt;
}