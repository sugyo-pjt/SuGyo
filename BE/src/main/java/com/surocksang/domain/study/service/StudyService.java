package com.surocksang.domain.study.service;

import com.surocksang.common.exception.ApplicationException;
import com.surocksang.common.exception.GlobalErrorCode;
import com.surocksang.domain.entity.UserDailyVocabulary;
import com.surocksang.domain.game.repository.UserDailyVocabularyRepository;
import com.surocksang.domain.study.dto.response.StudyProgressResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyService {

    private final UserDailyVocabularyRepository userDailyVocabularyRepository;

    public StudyProgressResponseDto getStudyProgress(Long userId) {
        try {
            List<UserDailyVocabulary> completedLearnings = userDailyVocabularyRepository
                    .findByUserId(userId);

            Integer maxProgressDay = completedLearnings.stream()
                    .mapToInt(learning -> learning.getDaily().getDay())
                    .max()
                    .orElse(0);

            return StudyProgressResponseDto.builder()
                    .progressDay(maxProgressDay)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving study progress for userId: {}", userId, e);
            throw new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}