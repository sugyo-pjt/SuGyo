package com.sugyo.domain.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.domain.game.dto.MotionFrame;
import com.sugyo.domain.game.dto.request.FrameSaveRequestDto;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.music.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class FrameCoordinatesService {

    private final FrameCoordinatesRepository frameCoordinatesRepository;
    private final MusicRepository musicRepository;
    private final ObjectMapper objectMapper;

    private static final Long MUSIC_ID = 1L;
    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    public void saveFrameCoordinates(FrameSaveRequestDto requestDto) {
        Music music = musicRepository.findById(requestDto.getMusicId())
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));
        try{
            String jsonString = objectMapper.writeValueAsString(requestDto);
            log.info("[save] {}", jsonString);
        }catch (JsonProcessingException e) {
            log.info("[save] {}", e.getMessage());
            return;
        }

        log.info("[save] {}", requestDto.getAllFrames().size());

        for (GameActionRequest gameAction : requestDto.getAllFrames()) {
            FrameCoordinates frameCoordinates = FrameCoordinates.builder()
                    .music(music)
                    .timePassed(gameAction.timestamp())
                    .frameData(gameAction.frames())
                    .build();
            log.info("[save] {}", frameCoordinates.getTimePassed());
            frameCoordinatesRepository.save(frameCoordinates);
        }
    }

    public double calculateSimilarity(GameActionRequest request) {
        double timePassed = request.timestamp();

        Optional<FrameCoordinates> frameCoordinatesOpt = frameCoordinatesRepository
                .findByMusicIdAndTimePassed(MUSIC_ID, timePassed);

        if (frameCoordinatesOpt.isEmpty()) {
            log.warn("해당 타임스탬프에 대한 프레임 좌표를 찾을 수 없습니다. musicId: {}, timePassed: {}",
                    MUSIC_ID, timePassed);
            return 0.0;
        }

        FrameCoordinates frameCoordinates = frameCoordinatesOpt.get();
        List<MotionFrame> dbFrames = frameCoordinates.getFrameData();
        List<MotionFrame> userFrames = request.frames();

        if (dbFrames == null || dbFrames.isEmpty() || userFrames.isEmpty()) {
            log.warn("프레임 데이터가 비어있습니다. dbFrames: {}, userFrames: {}",
                    dbFrames != null ? dbFrames.size() : 0, userFrames.size());
            return 0.0;
        }

        double similarity = JsonSimilarityComparator.calculateMotionSimilarity(
                userFrames, dbFrames, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        log.info("유사도 계산 완료. musicId: {}, timePassed: {}, similarity: {}",
                MUSIC_ID, timePassed, similarity);

        return similarity;
    }

}
