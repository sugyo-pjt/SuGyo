package com.sugyo.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.domain.Judgment;
import com.sugyo.domain.game.dto.MotionFrame;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.dto.response.GameActionResponse;
import com.sugyo.domain.game.dto.response.GameStateResponse;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.game.entity.Music;
import com.sugyo.domain.game.exception.WebSocketException;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.repository.GameResultRepository;
import com.sugyo.domain.game.repository.MusicRepository;
import com.sugyo.domain.user.domain.User;
import com.sugyo.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.sugyo.domain.game.domain.GameState.FINISHED;
import static com.sugyo.domain.game.domain.GameState.PAUSED;
import static com.sugyo.domain.game.domain.GameState.PLAYING;
import static com.sugyo.domain.game.domain.Judgment.GOOD;
import static com.sugyo.domain.game.domain.Judgment.MISS;
import static com.sugyo.domain.game.domain.Judgment.PERFECT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_MUSIC_ID;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebSocketGameService {

    private final ObjectMapper objectMapper;
    private final GameResultRepository gameResultRepository;
    private final UserRepository userRepository;
    private final MusicRepository musicRepository;
    private final FrameCoordinatesRepository frameCoordinatesRepository;

    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final double PERFECT_RATIO_THRESHOLD = 0.9;
    private static final double GOOD_RATIO_THRESHOLD = 0.7;

    public void processPlay(GameSessionContext context, GameActionRequest request) {
        if (!context.isPlaying()) {
            log.debug("PAUSED 상태에서 동작 처리 생략 - UserId: {}", context.getUserId());
            return;
        }

        Optional<FrameCoordinates> frameCoordinatesOpt = frameCoordinatesRepository
                .findByMusicIdAndTimePassed(context.getMusicId(), request.timestamp());

        if (frameCoordinatesOpt.isEmpty()) {
            log.warn("해당 타임스탬프에 대한 프레임 좌표를 찾을 수 없습니다. musicId: {}, timePassed: {}",
                    context.getMusicId(), request.timestamp());
            return;
        }

        FrameCoordinates frameCoordinates = frameCoordinatesOpt.get();
        List<MotionFrame> referenceFrames = frameCoordinates.getFrameData();


        double similarRatio = isFrameSimilar(request.frames(), referenceFrames);
        Judgment judgment = judgeByRatio(similarRatio);

        int points = calculatePoints(judgment, context.getCombo().get());

        context.applyJudgment(points, judgment);

        GameActionResponse response = GameActionResponse.from(context, judgment, points);
        sendMessageToClient(context.getWebSocketSession(), response);

        if (context.getLastNoteTimestamp() <= request.timestamp()) {
            finishGame(context);
        }
    }

    public void pauseGame(GameSessionContext context) {
        context.changeState(PAUSED);
        sendMessageToClient(context.getWebSocketSession(), new GameStateResponse(PAUSED));
        log.info("게임 일시정지: UserId={}, MusicId={}", context.getUserId(), context.getMusicId());
    }

    public void resumeGame(GameSessionContext context) {
        context.changeState(PLAYING);
        sendMessageToClient(context.getWebSocketSession(), new GameStateResponse(PLAYING));
        log.info("게임 재개: UserId={}, MusicId={}", context.getUserId(), context.getMusicId());
    }

    @Transactional
    public void finishGame(GameSessionContext context) {
        context.changeState(FINISHED);

        User user = userRepository.findById(Long.valueOf(context.getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + context.getUserId()));
        Music music = musicRepository.findById(context.getMusicId())
                .orElseThrow(() -> new EntityNotFoundException("Music not found with id: " + context.getMusicId()));
        int finalScore = context.getScore().get();

        Optional<GameResult> existingResult = gameResultRepository.findByUserAndMusic(user, music);
        if (existingResult.isPresent()) {
            GameResult gameResult = existingResult.get();
            boolean updated = gameResult.updateScoreIfHigher(finalScore);
            if (updated) {
                log.info("최고점 갱신: UserId={}, MusicId={}, New Score={}", user.getId(), music.getId(), finalScore);
            }
        } else {
            GameResult newResult = GameResult.create(user, music, finalScore);
            gameResultRepository.save(newResult);
            log.info("최초 점수 기록: UserId={}, MusicId={}, Score={}", user.getId(), music.getId(), finalScore);
        }

        sendMessageToClient(context.getWebSocketSession(), new GameStateResponse(FINISHED));
        log.info("게임 정상 종료 및 결과 저장: UserId={}, Score={}", context.getUserId(), context.getScore().get());
    }

    public void handleAbnormalTermination(GameSessionContext context) {
        log.warn("비정상 연결 종료 처리: UserId={}, SongId={}", context.getUserId(), context.getMusicId());

    }

    public GameSessionContext initializeGameSession(String userId, Long musicId, WebSocketSession session) throws WebSocketException {
        log.debug("Initialize game session: UserId={}, MusicId={}", userId, musicId);
        FrameCoordinates frameCoordinates = frameCoordinatesRepository.findTop1ByMusicIdOrderByTimePassedDesc(musicId)
                .orElseThrow(() -> new WebSocketException(INVALID_MUSIC_ID));

        double lastNoteTimestamp = frameCoordinates.getTimePassed();

        return new GameSessionContext(userId, musicId, session, lastNoteTimestamp);
    }

    private Double isFrameSimilar(List<MotionFrame> userFrame, List<MotionFrame> referenceFrame) {
        return JsonSimilarityComparator.calculateMotionSimilarity(
                userFrame, referenceFrame, 640, 480);

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

    private <T> void sendMessageToClient(WebSocketSession session, T payload) {
        try {
            if (session != null && session.isOpen()) {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(jsonPayload));
            }
        } catch (IOException e) {
            log.error("클라이언트 메시지 전송 실패: SessionId={}, Error: {}", session.getId(), e.getMessage());
        }
    }
}
