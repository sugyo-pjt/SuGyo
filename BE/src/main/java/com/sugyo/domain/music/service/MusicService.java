package com.sugyo.domain.music.service;

import com.sugyo.domain.music.domain.Music;
import com.sugyo.domain.music.dto.AllMusicInfoForStudyResponse;
import com.sugyo.domain.music.dto.MusicInfoForStudy;
import com.sugyo.domain.music.repository.MusicRepository;
import com.sugyo.domain.study.repository.MusicVocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MusicService {

    private final MusicRepository musicRepository;
    private final MusicVocabularyRepository musicVocabularyRepository;

    public AllMusicInfoForStudyResponse getAllMusicInfoForStudy() {
        List<Music> musics = musicRepository.findAll();

        List<MusicInfoForStudy> allMusicInfo = musics.stream()
                .map(music -> {
                    long countWord = musicVocabularyRepository.countByMusicId(music.getId());
                    return MusicInfoForStudy.from(music, countWord);
                })
                .toList();

        return AllMusicInfoForStudyResponse.builder()
                .musics(allMusicInfo)
                .build();
    }
}
