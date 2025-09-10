package com.surocksang.domain.game.service;

import com.surocksang.common.repository.ObjectStorageRepository;
import com.surocksang.config.properties.ObjectStorageProperties;
import com.surocksang.domain.entity.Music;
import com.surocksang.domain.game.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {

    private final MusicRepository musicRepository;
    private final ObjectStorageRepository objectStorageRepository;

    public String getMusic(Long musicId) {
        try {
            Music music = musicRepository.findById(musicId)
                    .orElseThrow(() -> new RuntimeException("Music not found with id: " + musicId));

            return objectStorageRepository.getDownloadUrl(music.getSongUrl());
        } catch (S3Exception e) {
            log.error("Error downloading song from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download song: " + musicId, e);
        }
    }
}