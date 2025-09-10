package com.surocksang.domain.game.service;

import com.surocksang.domain.game.dto.ChartDto;
import com.surocksang.domain.game.dto.MusicChartResponse;
import com.surocksang.domain.entity.Chart;
import com.surocksang.domain.entity.Music;
import com.surocksang.domain.game.repository.ChartRepository;
import com.surocksang.domain.game.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChartService {
    
    private final ChartRepository chartRepository;
    private final MusicRepository musicRepository;
    
    public MusicChartResponse getMusicWithCharts(Long musicId) {
        log.info("Getting charts for music ID: {}", musicId);
        
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new RuntimeException("Music not found with id: " + musicId));
        
        List<Chart> charts = chartRepository.findByMusicIdOrderBySequence(musicId);
        
        List<ChartDto> chartDtos = charts.stream()
                .map(this::convertToChartDto)
                .collect(Collectors.toList());
        
        return new MusicChartResponse(
                music.getId(),
                music.getTitle(),
                music.getSinger(),
                music.getSongTime(),
                music.getAlbumImageUrl(),
                chartDtos
        );
    }
    
    public List<ChartDto> getChartsByMusicId(Long musicId) {
        log.info("Getting charts for music ID: {}", musicId);
        
        if (!musicRepository.existsById(musicId)) {
            throw new RuntimeException("Music not found with id: " + musicId);
        }
        
        List<Chart> charts = chartRepository.findByMusicIdOrderBySequence(musicId);
        
        return charts.stream()
                .map(this::convertToChartDto)
                .collect(Collectors.toList());
    }
    
    private ChartDto convertToChartDto(Chart chart) {
        return new ChartDto(
                chart.getId(),
                chart.getSequence(),
                chart.getLyrics(),
                chart.getStartedAt()
        );
    }
}