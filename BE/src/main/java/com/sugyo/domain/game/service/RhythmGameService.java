package com.sugyo.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.common.repository.ObjectStorageRepository;
import com.sugyo.domain.game.dto.EasyGameMotionFrame;
import com.sugyo.domain.game.dto.request.GamePlayRequestDto;
import com.sugyo.domain.game.dto.MotionFrame;
import com.sugyo.domain.game.dto.Pose;
import com.sugyo.domain.game.domain.BodyPart;
import com.sugyo.domain.game.entity.Chart;
import com.sugyo.domain.game.entity.ChartAnswer;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.game.dto.response.MusicChartResponseDto;
import com.sugyo.domain.game.dto.response.MusicListResponseDto;
import com.sugyo.domain.game.dto.response.MusicRankingResponseDto;
import com.sugyo.domain.game.dto.response.MusicUrlResponseDto;
import com.sugyo.domain.game.dto.response.MusicWithScoreDto;
import com.sugyo.domain.game.dto.response.MyRankInfoDto;
import com.sugyo.domain.game.dto.response.RankingUserDto;
import com.sugyo.domain.game.entity.Chart;
import com.sugyo.domain.game.entity.ChartAnswer;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.game.repository.ChartAnswerRepository;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.repository.RankRepository;
import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.music.repository.MusicRepository;
import com.sugyo.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RhythmGameService {

    private final MusicRepository musicRepository;
    private final ObjectStorageRepository objectStorageRepository;
    private final RankRepository rankRepository;
    private final UserRepository userRepository;
    private final ChartAnswerRepository chartAnswerRepository;
    private final FrameCoordinatesRepository frameCoordinatesRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

//    @Transactional
//    public List<MusicListResponseDto> getAllMusic() {
//        List<Music> musicList = musicRepository.findAll();
//        return musicList.stream()
//                .map(music -> {
//                    String imageUrl = objectStorageRepository.getDownloadUrl(music.getAlbumImageUrl());
//                    return MusicListResponseDto.builder()
//                            .id(music.getId())
//                            .title(music.getTitle())
//                            .singer(music.getSinger())
//                            .songTime(music.getSongTime())
//                            .albumImageUrl(imageUrl)
//                            .build();
//                })
//            .toList();
//    }

    @Transactional
    public List<MusicListResponseDto> getAllMusicWithScore(Long userId) {
        List<MusicWithScoreDto> musicListWithScore = musicRepository.findAllMusicWithUserScore(userId);
        return musicListWithScore.stream()
                .map(musicWithScore -> {
                    String imageUrl = objectStorageRepository.getDownloadUrl(musicWithScore.getAlbumImageUrl());
                    return MusicListResponseDto.builder()
                            .id(musicWithScore.getId())
                            .title(musicWithScore.getTitle())
                            .singer(musicWithScore.getSinger())
                            .songTime(musicWithScore.getSongTime())
                            .albumImageUrl(musicWithScore.getAlbumImageUrl() != null ? imageUrl : null)
                            .myScore(musicWithScore.getMyScore() != null ? musicWithScore.getMyScore().longValue() : null)
                            .build();
                })
                .toList();
    }

    @Transactional
    public MusicUrlResponseDto getMusic(Long musicId) {
        Music  music = musicRepository.findById(musicId)
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        String musicUrl = objectStorageRepository.getDownloadUrl(music.getSongUrl());

        return MusicUrlResponseDto.builder()
                .musicUrl(musicUrl)
                .build();

    }

    @Transactional
    public List<MusicChartResponseDto> getMusicChart(Long musicId) {
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        List<Chart> charts = music.getChart().stream()
                .toList();

        log.info(music.toString());

        return charts.stream()
                .map(chart -> {
                    List<MusicChartResponseDto.CorrectAnswerDto> correctAnswers =
                            chart.getChartAnswers().stream()
                                    .map(answer -> MusicChartResponseDto.CorrectAnswerDto.builder()
                                            .correctStartedIndex(answer.getStartedIndex())
                                            .correctEndedIndex(answer.getEndedIndex())
                                            .actionStartedAt(answer.getStartedAt())
                                            .actionEndedAt(answer.getEndedAt())
                                            .build())
                                    .toList();

                    return MusicChartResponseDto.builder()
                            .segment(chart.getSequence())
                            .barStartedAt(chart.getStartedAt())
                            .lyrics(chart.getLyrics())
                            .correct(correctAnswers)
                            .build();
                })
                .toList();

    }

    @Transactional
    public MusicRankingResponseDto getMusicRanking(Long musicId, Long userId) {
        if (userId == null) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED);
        }
        // 음악 존재 확인
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        // 상위 5명 랭킹 조회
        List<GameResult> topRanks = rankRepository.findTop5ByMusicIdOrderByScoreDesc(musicId);

        AtomicInteger rank = new AtomicInteger(1);
        List<RankingUserDto> ranking = topRanks.stream()
                .limit(5)
                .map(rhythmGameRank -> RankingUserDto.builder()
                        .rank(rank.getAndIncrement())
                        .userId(rhythmGameRank.getUser().getId())
                        .userNickName(rhythmGameRank.getUser().getNickname())
                        .userProfileUrl(rhythmGameRank.getUser().getProfileImageUrl())
                        .score(rhythmGameRank.getScore())
                        .recordDate(rhythmGameRank.getUpdatedAt())
                        .build())
                .toList();

        // 내 정보 조회
        MyRankInfoDto myInfo = null;
        Optional<GameResult> myBestScore = rankRepository.findTopByMusicIdAndUserIdOrderByScoreDesc(musicId, userId);
        if (myBestScore.isPresent()) {
            GameResult myRank = myBestScore.get();
            Integer myRankPosition = rankRepository.findRankByMusicIdAndScore(musicId, myRank.getScore());
            myInfo = MyRankInfoDto.builder()
                    .rank(myRankPosition)
                    .score(myRank.getScore())
                    .recordDate(myRank.getCreatedAt())
                    .build();
        }

        return MusicRankingResponseDto.builder()
                .musicId(music.getId())
                .musicTitle(music.getTitle())
                .ranking(ranking)
                .myInfo(myInfo)
                .build();
    }

//    @Transactional
//    public GameResultResponseDto saveGameResult(GameResultRequestDto request, Long userId) {
//        if (userId == null) {
//            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED);
//        }
//
//        // 음악 존재 확인
//        Music music = musicRepository.findById(request.getMusicId())
//                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));
//
//        // 사용자 존재 확인
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));
//
//        // 기존 기록 조회
//        Optional<GameResult> existingRank = rankRepository.findByMusicIdAndUserId(request.getMusicId(), userId);
//
//        boolean isBestRecord = false;
//
//        if (existingRank.isPresent()) {
//            // 기존 기록이 있는 경우 - 최고 점수인지 확인
//            GameResult currentRank = existingRank.get();
//            if (request.getScore() > currentRank.getScore()) {
//                // 최고 기록 갱신
//                currentRank.setScore(request.getScore());
//                currentRank.setRecordTime(LocalDateTime.now());
//                rankRepository.save(currentRank);
//                isBestRecord = true;
//            }
//        } else {
//            // 기존 기록이 없는 경우 - 새로운 기록 생성
//            GameResult newRank = GameResult.builder()
//                    .music(music)
//                    .user(user)
//                    .score(request.getScore())
//                    .build();
//            rankRepository.save(newRank);
//            isBestRecord = true;
//        }
//
//        return GameResultResponseDto.builder()
//                .musicId(request.getMusicId())
//                .isBestRecord(isBestRecord)
//                .build();
//    }

    public void processGamePlay(GamePlayRequestDto request, Long userId) {
        if (userId == null) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED);
        }

        // 1. musicId와 segment로 해당 구간의 ChartAnswer 조회
        List<ChartAnswer> chartAnswers = chartAnswerRepository.findByMusicIdAndSegment(
                request.getMusicId(), request.getSegment());

        if (chartAnswers.isEmpty()) {
            log.warn("No chart answers found for musicId: {} and segment: {}",
                    request.getMusicId(), request.getSegment());
            return;
        }

        // 2. 각 ChartAnswer에 대해 유사도 검사 수행
        for (ChartAnswer answer : chartAnswers) {
            // LocalTime을 milliseconds로 변환 (300ms 단위로 반올림)
            double startTimeMs = convertLocalTimeToMs(answer.getStartedAt());
            double endTimeMs = convertLocalTimeToMs(answer.getEndedAt());

            // 3. 해당 시간 범위의 정답 프레임 데이터 조회
            List<FrameCoordinates> correctFrames = frameCoordinatesRepository.findByMusicId(request.getMusicId());

            if (correctFrames.isEmpty()) {
                log.warn("No correct frames found for musicId: {}, timeRange: {} - {}",
                        request.getMusicId(), startTimeMs, endTimeMs);
                continue;
            }

            // 4. 클라이언트가 보낸 프레임과 정답 프레임 비교
            double similarity = calculateSimilarity(request.getFrames(), correctFrames);

            log.info("Similarity for segment {} in range {} - {}: {}", request.getSegment(), startTimeMs, endTimeMs, similarity);
        }
    }

    private double convertLocalTimeToMs(LocalTime localTime) {
        return localTime.toNanoOfDay() / 1_000_000.0;
    }

    private double calculateSimilarity(List<EasyGameMotionFrame> clientFrames,
                                     List<FrameCoordinates> correctFrames) {
        if (clientFrames.isEmpty() || correctFrames.isEmpty()) {
            return 0.0;
        }

        // 1. EasyGameMotionFrame을 MotionFrame으로 변환
        List<MotionFrame> clientMotionFrames = convertToMotionFrames(clientFrames);

        // 2. FrameCoordinates에서 MotionFrame으로 변환
        List<MotionFrame> correctMotionFrames = convertFrameCoordinatesToMotionFrames(correctFrames);

        if (clientMotionFrames.isEmpty() || correctMotionFrames.isEmpty()) {
            return 0.0;
        }

        // 3. JsonSimilarityComparator의 calculateMotionSimilarity 호출
        return JsonSimilarityComparator.calculateMotionSimilarity(
                clientMotionFrames, correctMotionFrames, 640, 480);
    }

    private List<MotionFrame> convertToMotionFrames(List<EasyGameMotionFrame> easyFrames) {
        List<MotionFrame> motionFrames = new ArrayList<>();

        for (int i = 0; i < easyFrames.size(); i++) {
            EasyGameMotionFrame easyFrame = easyFrames.get(i);
            List<Pose> poses = new ArrayList<>();

            // BODY 포즈 추가
            poses.add(new Pose(BodyPart.BODY, easyFrame.pose()));

            // LEFT_HAND 포즈 추가
            poses.add(new Pose(BodyPart.LEFT_HAND, easyFrame.left()));

            // RIGHT_HAND 포즈 추가
            poses.add(new Pose(BodyPart.RIGHT_HAND, easyFrame.right()));

            // MotionFrame 생성 (frame 번호는 인덱스 + 1)
            motionFrames.add(new MotionFrame(i, poses));
        }

        return motionFrames;
    }

    private List<MotionFrame> convertFrameCoordinatesToMotionFrames(List<FrameCoordinates> frameCoordinates) {
        List<MotionFrame> motionFrames = new ArrayList<>();

        for (int i = 0; i < frameCoordinates.size(); i++) {
            FrameCoordinates frameCoord = frameCoordinates.get(i);

            // FrameCoordinates의 frameData는 List<MotionFrame> 타입
            // 각 FrameCoordinates에서 첫 번째 MotionFrame만 사용
            List<MotionFrame> storedFrames = frameCoord.getFrameData();
            if (!storedFrames.isEmpty()) {
                motionFrames.add(storedFrames.get(0));
            }
        }

        return motionFrames;
    }

}
