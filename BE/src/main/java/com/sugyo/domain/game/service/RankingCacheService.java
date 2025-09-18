package com.sugyo.domain.game.service;

import com.sugyo.domain.game.dto.response.RankingUserDto;
import com.sugyo.domain.game.entity.RhythmGameRank;
import com.sugyo.domain.game.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingCacheService {

    private final RankRepository rankRepository;

    @Cacheable(value = "musicRanking", key = "#musicId")
    public List<RankingUserDto> getCachedTop5Ranking(Long musicId) {
        log.info("Cache miss - Loading top 5 ranking from DB for musicId: {}", musicId);

        // DB에서 Top 5 조회
        List<RhythmGameRank> topRanks = rankRepository.findTop5ByMusicIdOrderByScoreDesc(musicId);

        AtomicInteger rank = new AtomicInteger(1);
        return topRanks.stream()
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
    }

    @CachePut(value = "musicRanking", key = "#musicId")
    public List<RankingUserDto> updateTop5RankingCache(Long musicId, List<RhythmGameRank> topRanks) {
        log.info("Updating top 5 ranking cache for musicId: {}", musicId);

        AtomicInteger rank = new AtomicInteger(1);
        return topRanks.stream()
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
    }

    @CacheEvict(value = "musicRanking", key = "#musicId")
    public void evictRankingCache(Long musicId) {
        log.info("Evicting ranking cache for musicId: {}", musicId);
    }
}