package com.sugyo.domain.study.service;

import com.sugyo.auth.dto.CustomUserDetails;
import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.domain.study.dto.response.DayProgressDto;
import com.sugyo.domain.study.dto.response.StudyDayResponseDto;
import com.sugyo.domain.study.dto.response.StudyProgressDetailsResponseDto;
import com.sugyo.domain.study.dto.response.StudyProgressResponseDto;
import com.sugyo.domain.study.dto.response.StudyWordItemDto;
import com.sugyo.domain.study.entity.Daily;
import com.sugyo.domain.study.entity.UserDailyVocabulary;
import com.sugyo.domain.study.entity.Vocabulary;
import com.sugyo.domain.study.repository.DailyRepository;
import com.sugyo.domain.study.repository.DailyVocabularyRepository;
import com.sugyo.domain.study.repository.UserDailyVocabularyRepository;
import com.sugyo.domain.study.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
                .map(dv -> StudyWordItemDto.from(dv.getVocabulary()))
                .toList();
        return StudyDayResponseDto.builder()
                .day(daily.getDay())
                .items(items)
                .build();
    }

    public List<StudyWordItemDto> searchVocabulary(String keyword) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByWordContaining(keyword);
        return vocabularies.stream()
                .map(StudyWordItemDto::from)
                .collect(Collectors.toList());
    }
}
