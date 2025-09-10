package com.surocksang.domain.game.service;

import com.surocksang.common.exception.ApplicationException;
import com.surocksang.common.exception.GlobalErrorCode;
import com.surocksang.common.repository.ObjectStorageRepository;
import com.surocksang.domain.entity.Chart;
import com.surocksang.domain.entity.Music;
import com.surocksang.domain.game.dto.response.GameChartResponseDto;
import com.surocksang.domain.game.dto.response.MusicListResponseDto;
import com.surocksang.domain.game.dto.response.MusicUrlResponseDto;
import com.surocksang.domain.game.repository.MusicRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RhythmGameService {

    private final MusicRepository musicRepository;
    private final ObjectStorageRepository objectStorageRepository;

    @Transactional
    public List<MusicListResponseDto> getAllMusic() {
        List<Music> musicList = musicRepository.findAll();
        return musicList.stream()
                .map(music -> {
                    String imageUrl = objectStorageRepository.getDownloadUrl(music.getAlbumImageUrl());
                    return MusicListResponseDto.builder()
                            .id(music.getId())
                            .title(music.getTitle())
                            .singer(music.getSinger())
                            .songTime(music.getSongTime())
                            .albumImageUrl(imageUrl)
                            .build();
                })
            .toList();
    }

    @Transactional
    public MusicUrlResponseDto getMusic(Long musicId) {
            Music music = musicRepository.findById(musicId)
                    .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

            String musicUrl = objectStorageRepository.getDownloadUrl(music.getSongUrl());

            return MusicUrlResponseDto.builder()
                    .musicUrl(musicUrl)
                    .build();

    }

    @Transactional
    public List<GameChartResponseDto> getMusicChart(Long musicId) {
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        List<Chart> charts = music.getChart().stream()
                .toList();

        log.info(music.toString());

        return charts.stream()
                .map(chart -> {
                    List<GameChartResponseDto.CorrectAnswerDto> correctAnswers =
                            chart.getChartAnswers().stream()
                                    .map(answer -> GameChartResponseDto.CorrectAnswerDto.builder()
                                            .correctStartedIndex(answer.getStartedIndex())
                                            .correctEndedIndex(answer.getEndedIndex())
                                            .actionStartedAt(answer.getStartedAt())
                                            .actionEndedAt(answer.getEndedAt())
                                            .build())
                                    .toList();

                    return GameChartResponseDto.builder()
                            .segment(chart.getSequence())
                            .barStartedAt(chart.getStartedAt())
                            .lyrics(chart.getLyrics())
                            .correct(correctAnswers)
                            .build();
                })
                .toList();

    }

}