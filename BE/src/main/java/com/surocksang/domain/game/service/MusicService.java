package com.surocksang.domain.game.service;

import com.surocksang.common.exception.ApplicationException;
import com.surocksang.common.exception.CommonErrorCode;
import com.surocksang.common.exception.GlobalErrorCode;
import com.surocksang.common.repository.ObjectStorageRepository;
import com.surocksang.config.properties.ObjectStorageProperties;
import com.surocksang.domain.entity.Music;
import com.surocksang.domain.game.dto.response.MusicListResponseDto;
import com.surocksang.domain.game.dto.response.MusicUrlResponseDto;
import com.surocksang.domain.game.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {

    private final MusicRepository musicRepository;
    private final ObjectStorageRepository objectStorageRepository;

    public List<MusicListResponseDto> getAllMusic() {
        try{
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
        }catch (Exception e){
            throw new ApplicationException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public MusicUrlResponseDto getMusic(Long musicId) {
        try {
            Music music = musicRepository.findById(musicId)
                    .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

            String musicUrl = objectStorageRepository.getDownloadUrl(music.getSongUrl());

            return MusicUrlResponseDto.builder()
                    .musicUrl(musicUrl)
                    .build();

        } catch (S3Exception e) {
            log.error("Error downloading song from S3: {}", e.getMessage());
            throw new ApplicationException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}