package com.surocksang.domain.study.service;

import com.surocksang.auth.dto.CustomUserDetails;
import com.surocksang.common.exception.ApplicationException;
import com.surocksang.common.exception.GlobalErrorCode;
import com.surocksang.domain.entity.UserDailyVocabulary;
import com.surocksang.domain.game.repository.UserDailyVocabularyRepository;
import com.surocksang.domain.study.dto.response.StudyProgressResponseDto;
import com.surocksang.domain.study.dto.response.StudyProgressDetailsResponseDto;
import com.surocksang.domain.study.dto.response.DayProgressDto;
import com.surocksang.domain.entity.Daily;
import com.surocksang.domain.game.repository.DailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyService {

    private final UserDailyVocabularyRepository userDailyVocabularyRepository;
    private final DailyRepository dailyRepository;

    public StudyProgressResponseDto getStudyProgress(CustomUserDetails user) {

        Long userId = user.getId();

        if(userId == null){
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
}