package com.sugyo.domain.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.CommonErrorCode;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.domain.Judgment;
import com.sugyo.domain.game.dto.MotionFrame;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.dto.request.GameResultRequestDto;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.game.exception.WebSocketException;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.repository.GameResultRepository;
import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.music.repository.MusicRepository;
import com.sugyo.domain.user.domain.User;
import com.sugyo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sugyo.domain.game.domain.GameState.FINISHED;
import static com.sugyo.domain.game.domain.Judgment.GOOD;
import static com.sugyo.domain.game.domain.Judgment.MISS;
import static com.sugyo.domain.game.domain.Judgment.PERFECT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_MUSIC_ID;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class FrameCoordinatesService {

    private final GameResultRepository gameResultRepository;
    private final UserRepository userRepository;
    private final FrameCoordinatesRepository frameCoordinatesRepository;
    private final MusicRepository musicRepository;
    private final ObjectMapper objectMapper;

    private static final Long MUSIC_ID = 1L;
    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;
    private static final double PERFECT_RATIO_THRESHOLD = 0.9;
    private static final double GOOD_RATIO_THRESHOLD = 0.7;

    public void checkFrameCoordinates(GameResultRequestDto requestDto, Long userId) throws JsonProcessingException {

        try {

            log.debug("[inService]");
            log.debug("DTO :{}", objectMapper.writeValueAsString(requestDto));
            Long musicId = requestDto.getClientCoordinates().getFirst().getMusicId();
            log.debug("UserId {} MusicId : {}", userId, musicId);

            List<FrameCoordinates> correctFrames = frameCoordinatesRepository.findByMusicId(musicId);

            log.debug("correctFrames : ", objectMapper.writeValueAsString(correctFrames));
            Map<Double, FrameCoordinates> frameMap = correctFrames.stream()
                    .collect(Collectors.toMap(FrameCoordinates::getTimePassed, Function.identity()));

            Double lastNoteTimestamp = getLastNoteTimestamp(musicId);

            int score = 0;
            int combo = 0;
//            int perfectCount = 0;
//            int goodCount = 0;
//            int missCount = 0;

            log.debug("frameMap : ", objectMapper.writeValueAsString(frameMap));
            for (GameActionRequest gameAction : requestDto.getClientCoordinates().getFirst().getAllFrames()) {
                FrameCoordinates correctCurrentFrames = frameMap.get(gameAction.timestamp());

                // 유사도 계산
                double similarity = JsonSimilarityComparator.calculateMotionSimilarity(
                        gameAction.frames(), correctCurrentFrames.getFrameData(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

                Judgment judgment = judgeByRatio(similarity);

                // 점수 계산
                score += calculatePoints(judgment, combo);

                switch (judgment) {
                    case PERFECT -> {
                        combo++;
//                        perfectCount++;
                    }
                    case GOOD -> {
                        combo++;
//                        goodCount++;
                    }
                    case MISS -> {
                        combo = 0;
//                        missCount++;
                    }
                }
                log.debug("[CHECK] timestamp={}, score={}, judgment={}, similarity={}", gameAction.timestamp(), score, judgment, similarity);

            }
            if( score != requestDto.getClientCalculateScore()) {
                throw new ApplicationException(CommonErrorCode.TAMPERED_VALUE);
            }

            finishGame(userId, musicId, score);
        } catch (Exception e) {
            log.debug("[ERROR]", e.getMessage());
            e.printStackTrace();
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

    private Judgment judgeByRatio(double ratio) {
        if (PERFECT_RATIO_THRESHOLD <= ratio) return PERFECT;
        if (GOOD_RATIO_THRESHOLD <= ratio) return GOOD;
        return MISS;
    }

    private int calculatePoints(Judgment judgment, int combo) {
        return switch (judgment) {
            case PERFECT -> 100 + (combo / 10);
            case GOOD -> 70 + (combo / 20);
            case MISS -> 0;
        };
    }

    private Double getLastNoteTimestamp(Long musicId) {
        FrameCoordinates frameCoordinates = frameCoordinatesRepository.findTop1ByMusicIdOrderByTimePassedDesc(musicId)
                .orElseThrow(() -> new WebSocketException(INVALID_MUSIC_ID));

        return frameCoordinates.getTimePassed();
    }

    @Transactional
    public void finishGame(Long userId, Long musicId, int score) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        Optional<GameResult> existingResult = gameResultRepository.findByUserAndMusic(user, music);
        if (existingResult.isPresent()) {
            GameResult gameResult = existingResult.get();
            boolean updated = gameResult.updateScoreIfHigher(score);
            if (updated) {
                log.info("최고점 갱신: UserId={}, MusicId={}, New Score={}", user.getId(), music.getId(), score);
            }
        } else {
            GameResult newResult = GameResult.create(user, music, score);
            gameResultRepository.save(newResult);
            log.info("최초 점수 기록: UserId={}, MusicId={}, Score={}", user.getId(), music.getId(), score);
        }

        log.info("게임 정상 종료 및 결과 저장: UserId={}, Score={}", userId, score);
    }
}
