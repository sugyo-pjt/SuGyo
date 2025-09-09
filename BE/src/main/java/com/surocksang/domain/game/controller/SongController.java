package com.surocksang.domain.game.controller;

import com.surocksang.domain.game.service.SongDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {
    private final SongDownloadService songDownloadService;
    
    @GetMapping("/download/{fileName}")
    public ResponseEntity<?> downloadSong(@PathVariable Long musicId) {
        try {
            InputStream songStream = songDownloadService.downloadSong(musicId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(songStream));
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}