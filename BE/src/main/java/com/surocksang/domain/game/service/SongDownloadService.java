package com.surocksang.domain.game.service;

import com.surocksang.domain.game.repository.S3Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongDownloadService {
    
    private final S3Processor s3Processor;
    
    public InputStream downloadSong(Long musicId) {
        try {
            return s3Processor.getObject("musicId");
        } catch (S3Exception e) {
            log.error("Error downloading song from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download song: " + musicId, e);
        }
    }
}