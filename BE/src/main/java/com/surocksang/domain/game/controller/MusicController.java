package com.surocksang.domain.game.controller;

import com.surocksang.domain.game.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class MusicController {
    private final MusicService musicService;
    
    @GetMapping("/download/{musicId}")
    public ResponseEntity<?> getMusicUrl(@PathVariable Long musicId) {
        try {
            String musicUrl = musicService.getMusic(musicId);

            return new ResponseEntity<>(musicUrl, HttpStatus.OK);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}