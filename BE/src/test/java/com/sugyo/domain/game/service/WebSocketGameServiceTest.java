package com.sugyo.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.domain.GameActionType;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.domain.GameState;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.dto.response.GameActionResponse;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.repository.GameResultRepository;
import com.sugyo.domain.music.repository.MusicRepository;
import com.sugyo.domain.user.domain.User;
import com.sugyo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketGameServiceTest {

    @InjectMocks
    private WebSocketGameService gameService;

    // --- Mock 객체들 ---
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private GameResultRepository gameResultRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MusicRepository musicRepository;
    @Mock
    private FrameCoordinatesRepository frameCoordinatesRepository;
    @Mock
    private WebSocketSession webSocketSession;

    // --- 테스트용 공통 객체 ---
    private GameSessionContext context;
    private User testUser;
    private Music testMusic;

    @BeforeEach
    void setUp() {
        // Mock 객체들을 초기화하고, 테스트에 필요한 공통 객체들을 설정합니다.
        context = new GameSessionContext("1", 1L, webSocketSession, 100.0);
        testUser = User.builder()
                .id(1L)
                .build(); // 실제 User 객체 또는 Mock 객체
        testMusic = new Music(); // 실제 Music 객체 또는 Mock 객체
        testMusic.setId(1L);
    }

    @Nested
    @DisplayName("processPlay 메서드 테스트")
    class ProcessPlayTests {

        @Test
        @DisplayName("PERFECT 판정 시 점수와 콤보가 정상적으로 오르고 메시지를 전송한다")
        void givenPerfectSimilarity_whenProcessPlay_thenUpdateScoreAndCombo() throws IOException {
            // given
            GameActionRequest request = new GameActionRequest(GameActionType.PLAY, 50.0, List.of());
            FrameCoordinates coords = new FrameCoordinates(/* ... */);

            // Mockito를 사용하여 정적(static) 메서드를 Mocking
            try (MockedStatic<JsonSimilarityComparator> mockedStatic = mockStatic(JsonSimilarityComparator.class)) {
                // isFrameSimilar 내부의 정적 메서드가 0.95를 반환하도록 설정
                mockedStatic.when(() -> JsonSimilarityComparator.calculateMotionSimilarity(any(), any(), anyInt(), anyInt()))
                        .thenReturn(0.95);

                when(frameCoordinatesRepository.findByMusicIdAndTimePassed(anyLong(), anyDouble())).thenReturn(Optional.of(coords));
                when(objectMapper.writeValueAsString(any(GameActionResponse.class))).thenReturn("{\"judgment\":\"PERFECT\"}");
                when(webSocketSession.isOpen()).thenReturn(true);

                // when
                gameService.processPlay(context, request);

                // then
                assertThat(context.getScore().get()).isGreaterThan(0);
                assertThat(context.getCombo().get()).isEqualTo(1);
                assertThat(context.getPerfectCount().get()).isEqualTo(1);

                // sendMessageToClient가 한 번 호출되었는지 검증
                verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
            }
        }

        @Test
        @DisplayName("게임이 PAUSED 상태일 때는 아무 동작도 하지 않는다")
        void givenPausedState_whenProcessPlay_thenDoNothing() {
            // given
            context.changeState(GameState.PAUSED);
            GameActionRequest request = new GameActionRequest(GameActionType.PLAY, 50.0, List.of());

            // when
            gameService.processPlay(context, request);

            // then
            // webSocketSession.sendMessage()가 절대 호출되지 않았음을 검증
            try {
                verify(webSocketSession, never()).sendMessage(any());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 다른 repository들과의 상호작용도 없었는지 추가로 검증할 수 있음
            verifyNoInteractions(frameCoordinatesRepository);
            verifyNoInteractions(gameResultRepository);
        }

        @Test
        @DisplayName("마지막 노트를 플레이하면 finishGame을 호출한다")
        void givenLastNote_whenProcessPlay_thenFinishGame() {
            // given
            // 마지막 노트 시간과 동일한 시간의 요청 생성
            GameActionRequest request = new GameActionRequest(GameActionType.PLAY, 100.0, List.of());
            FrameCoordinates coords = new FrameCoordinates();

            try (MockedStatic<JsonSimilarityComparator> mockedStatic = mockStatic(JsonSimilarityComparator.class)) {
                mockedStatic.when(() -> JsonSimilarityComparator.calculateMotionSimilarity(any(), any(), anyInt(), anyInt())).thenReturn(0.8);
                when(frameCoordinatesRepository.findByMusicIdAndTimePassed(anyLong(), anyDouble())).thenReturn(Optional.of(coords));

                // finishGame 내부에서 필요한 Mock 설정
                when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
                when(musicRepository.findById(anyLong())).thenReturn(Optional.of(testMusic));
                when(gameResultRepository.findByUserAndMusic(any(), any())).thenReturn(Optional.empty());

                // when
                gameService.processPlay(context, request);

                // then
                // finishGame이 호출되었는지 간접적으로 검증 (예: FINISHED 상태 변경)
                assertThat(context.getGameState()).isEqualTo(GameState.FINISHED);
                // gameResultRepository.save가 호출되었는지 검증
                verify(gameResultRepository, times(1)).save(any(GameResult.class));
            }
        }
    }

    @Nested
    @DisplayName("finishGame 메서드 테스트")
    class FinishGameTests {

        @Test
        @DisplayName("최초 기록 시 새로운 GameResult를 저장한다")
        void whenFinishGameFirstTime_thenSaveNewResult() {
            // given
            context.getScore().set(50000);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(musicRepository.findById(1L)).thenReturn(Optional.of(testMusic));
            // findByUserAndMusic이 비어있는 Optional을 반환하도록 설정 (최초 기록)
            when(gameResultRepository.findByUserAndMusic(testUser, testMusic)).thenReturn(Optional.empty());

            // when
            gameService.finishGame(context);

            // then
            // gameResultRepository.save가 한 번 호출되었는지 검증
            verify(gameResultRepository, times(1)).save(any(GameResult.class));
        }

        @Test
        @DisplayName("기존 기록보다 높을 시 최고점을 갱신한다")
        void givenHigherScore_whenFinishGame_thenUpdateHighScore() {
            // given
            context.getScore().set(60000);
            GameResult existingResult = mock(GameResult.class); // GameResult를 Mock으로 만듦

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(musicRepository.findById(1L)).thenReturn(Optional.of(testMusic));
            when(gameResultRepository.findByUserAndMusic(testUser, testMusic)).thenReturn(Optional.of(existingResult));
            // updateScoreIfHigher가 true를 반환하도록 설정
            when(existingResult.updateScoreIfHigher(60000)).thenReturn(true);

            // when
            gameService.finishGame(context);

            // then
            // updateScoreIfHigher가 호출되었는지 검증
            verify(existingResult, times(1)).updateScoreIfHigher(60000);
            // 새로운 save는 호출되지 않았음을 검증 (@Transactional에 의한 dirty-checking)
            verify(gameResultRepository, never()).save(any(GameResult.class));
        }

        @Test
        @DisplayName("User를 찾지 못하면 EntityNotFoundException을 던진다")
        void givenNonExistentUser_whenFinishGame_thenThrowException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThrows(EntityNotFoundException.class, () -> {
                gameService.finishGame(context);
            });
        }
    }
}

