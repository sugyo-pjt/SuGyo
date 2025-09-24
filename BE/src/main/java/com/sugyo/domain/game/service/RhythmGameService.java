package com.sugyo.domain.game.service;

import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.common.repository.ObjectStorageRepository;
import com.sugyo.domain.game.entity.Chart;
import com.sugyo.domain.game.entity.Music;
import com.sugyo.domain.game.entity.GameResult;
import com.sugyo.domain.game.dto.response.MusicChartResponseDto;
import com.sugyo.domain.game.dto.response.MusicListResponseDto;
import com.sugyo.domain.game.dto.response.MusicUrlResponseDto;
import com.sugyo.domain.game.dto.response.MusicWithScoreDto;
import com.sugyo.domain.game.dto.response.MusicRankingResponseDto;
import com.sugyo.domain.game.dto.response.RankingUserDto;
import com.sugyo.domain.game.dto.response.MyRankInfoDto;
import com.sugyo.domain.game.dto.request.GameResultRequestDto;
import com.sugyo.domain.game.dto.response.GameResultResponseDto;
import com.sugyo.domain.game.dto.request.GamePlayRequestDto;
import com.sugyo.domain.game.repository.MusicRepository;
import com.sugyo.domain.game.repository.RankRepository;
import com.sugyo.domain.user.repository.UserRepository;
import com.sugyo.domain.user.domain.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
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
    private final WebClient webClient;

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
                            .albumImageUrl(musicWithScore.getAlbumImageUrl() !=null ? imageUrl : null)
                            .myScore(musicWithScore.getMyScore() != null ? musicWithScore.getMyScore().longValue() : null)
                            .build();
                })
                .toList();
    }

    @Transactional
    public MusicUrlResponseDto getMusic(Long musicId) {
            Music music = musicRepository.findById(musicId)
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
        if(userId == null){
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
                        .recordDate(rhythmGameRank.getRecordTime())
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
                    .recordDate(myRank.getRecordTime())
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
        String response = webClient.get()
                .retrieve()
                .bodyToMono(String.class)
                .subscribe()
                .toString();

        System.out.println("AI 서버 응답: " + response);
    }

}
