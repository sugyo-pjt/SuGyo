package com.sugyo.domain.game.service;

import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.common.repository.ObjectStorageRepository;
import com.sugyo.domain.game.entity.Chart;
import com.sugyo.domain.game.entity.Music;
import com.sugyo.domain.game.entity.RhythmGameRank;
import com.sugyo.domain.game.dto.response.MusicChartResponseDto;
import com.sugyo.domain.game.dto.response.MusicListResponseDto;
import com.sugyo.domain.game.dto.response.MusicUrlResponseDto;
import com.sugyo.domain.game.dto.response.MusicWithScoreDto;
import com.sugyo.domain.game.dto.response.MusicRankingResponseDto;
import com.sugyo.domain.game.dto.response.RankingUserDto;
import com.sugyo.domain.game.dto.response.MyRankInfoDto;
import com.sugyo.domain.game.repository.MusicRepository;
import com.sugyo.domain.game.repository.RankRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        List<RhythmGameRank> topRanks = rankRepository.findTop5ByMusicIdOrderByScoreDesc(musicId);
        
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
        Optional<RhythmGameRank> myBestScore = rankRepository.findTopByMusicIdAndUserIdOrderByScoreDesc(musicId, userId);
        if (myBestScore.isPresent()) {
            RhythmGameRank myRank = myBestScore.get();
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

}