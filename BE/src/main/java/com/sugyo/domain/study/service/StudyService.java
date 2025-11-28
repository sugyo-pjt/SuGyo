package com.sugyo.domain.study.service;

import com.sugyo.auth.dto.CustomUserDetails;
import com.sugyo.common.annotation.CacheableWithTTL;
import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.domain.study.dto.request.QuizResultRequest;
import com.sugyo.domain.study.dto.response.DayProgressDto;
import com.sugyo.domain.study.dto.response.SearchKeywordResponse;
import com.sugyo.domain.study.dto.response.StudyDayResponseDto;
import com.sugyo.domain.study.dto.response.StudyProgressDetailsResponseDto;
import com.sugyo.domain.study.dto.response.StudyProgressResponseDto;
import com.sugyo.domain.study.dto.response.StudyWordItemDto;
import com.sugyo.domain.study.entity.Daily;
import com.sugyo.domain.study.entity.MusicVocabulary;
import com.sugyo.domain.study.entity.UserDailyVocabulary;
import com.sugyo.domain.study.entity.Vocabulary;
import com.sugyo.domain.study.repository.DailyRepository;
import com.sugyo.domain.study.repository.DailyVocabularyRepository;
import com.sugyo.domain.study.repository.MusicVocabularyRepository;
import com.sugyo.domain.study.repository.UserDailyVocabularyRepository;
import com.sugyo.domain.study.repository.VocabularyRepository;
import com.sugyo.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sugyo.common.exception.GlobalErrorCode.RESOURCE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyService {

    private final UserDailyVocabularyRepository userDailyVocabularyRepository;
    private final DailyRepository dailyRepository;
    private final DailyVocabularyRepository dailyVocabularyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final MusicVocabularyRepository musicVocabularyRepository;

    public StudyProgressResponseDto getStudyProgress(CustomUserDetails user) {

        Long userId = user.getId();

        if (userId == null) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED);
        }

        List<UserDailyVocabulary> completedLearnings = userDailyVocabularyRepository
                .findByUserId(userId);

        Integer maxProgressDay = completedLearnings.stream()
                .mapToInt(learning -> learning.getDaily().getDay())
                .max()
                .orElse(0);

        return StudyProgressResponseDto.builder()
                .progressDay(maxProgressDay)
                .build();

    }

    public StudyProgressDetailsResponseDto getStudyProgressDetails(CustomUserDetails user) {
        Long userId = user.getId();

        if (userId == null) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED);
        }

        List<DayProgressDto> dayProgresses = userDailyVocabularyRepository.findDayProgressByUserId(userId);

        Integer totalDays = Math.toIntExact(dailyRepository.count());
        Integer maxProgressDay =
                userDailyVocabularyRepository.findMaxProgressDayByUserId(userId);

        if (maxProgressDay == null) {
            maxProgressDay = 0;
        }
        return StudyProgressDetailsResponseDto.builder()
                .totalDays(totalDays)
                .progressDay(maxProgressDay)
                .days(dayProgresses)
                .build();
    }

    public StudyDayResponseDto getStudyDay(Long dayId) {
        Daily daily = dailyRepository.findById(dayId)
                .orElseThrow(() -> new ApplicationException(RESOURCE_NOT_FOUND));

        List<StudyWordItemDto> items = dailyVocabularyRepository.findByDailyId(dayId)
                .stream()
                .map(dv -> {
                    Vocabulary vocabulary = dv.getVocabulary();
                    Set<String> wordListByMotion = getWordListByMotion(vocabulary.getMotion().getId());
                    wordListByMotion.remove(vocabulary.getWord());
                    return StudyWordItemDto.from(vocabulary, wordListByMotion);
                })
                .toList();
        return StudyDayResponseDto.builder()
                .day(daily.getDay())
                .items(items)
                .build();
    }

    public List<SearchKeywordResponse> searchVocabulary(String keyword) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByWordContaining(keyword);
        return vocabularies.stream()
                .map(SearchKeywordResponse::from)
                .collect(Collectors.toList());
    }

    public StudyWordItemDto getWordItem(long wordId) {
        Vocabulary vocabulary = vocabularyRepository.findById(wordId)
                .orElseThrow(() -> new ApplicationException(RESOURCE_NOT_FOUND));
        Set<String> wordListByMotion = getWordListByMotion(vocabulary.getMotion().getId());
        wordListByMotion.remove(vocabulary.getWord());
        return StudyWordItemDto.from(vocabulary, wordListByMotion);
    }

    @Transactional
    public void saveQuizResult(Long userId, QuizResultRequest request) {


        Daily daily = dailyRepository.findById(request.getDayId())
                .orElseThrow(() -> new ApplicationException(RESOURCE_NOT_FOUND));

        Optional<UserDailyVocabulary> existingResult = userDailyVocabularyRepository
                .findByUserIdAndDailyId(userId, request.getDayId());

        if (existingResult.isPresent()) {
            UserDailyVocabulary userDailyVocabulary = existingResult.get();
            if (request.getScore() > userDailyVocabulary.getCorrectCount()) {
                userDailyVocabulary.setCorrectCount(request.getScore());
                userDailyVocabularyRepository.save(userDailyVocabulary);
            }
        } else {
            User userEntity = User.builder().id(userId).build();
            UserDailyVocabulary userDailyVocabulary = UserDailyVocabulary.builder()
                    .user(userEntity)
                    .daily(daily)
                    .correctCount(request.getScore())
                    .build();
            userDailyVocabularyRepository.save(userDailyVocabulary);
        }
    }

    private Set<String> getWordListByMotion(long motionId) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByMotionId(motionId)
                .orElseThrow(() -> new ApplicationException(RESOURCE_NOT_FOUND));
        return vocabularies.stream()
                .map(Vocabulary::getWord)
                .collect(Collectors.toSet());
    }

    @CacheableWithTTL(cacheName = "MUSIC-VOCABULARY", ttl = 1, unit = ChronoUnit.HOURS)
    public List<StudyWordItemDto> getAllMusicVocabulary(long musicId) {
        List<MusicVocabulary> musicVocabularies = musicVocabularyRepository.findAllByMusicId(musicId);
        return musicVocabularies.stream()
                .map(musicVocabulary -> {
                    return getWordItem(musicVocabulary.getVocabulary().getId());
                })
                .toList();
    }
}
