package com.sugyo.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.common.exception.ApplicationException;
import com.sugyo.common.exception.GlobalErrorCode;
import com.sugyo.domain.game.dto.request.FrameSaveRequestDto;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.entity.Music;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FrameCoordinatesService {

    private final FrameCoordinatesRepository frameCoordinatesRepository;
    private final MusicRepository musicRepository;
    private final ObjectMapper objectMapper;

    public void saveFrameCoordinates(FrameSaveRequestDto requestDto) {
        Music music = musicRepository.findById(requestDto.getMusicId())
                .orElseThrow(() -> new ApplicationException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        List<FrameCoordinates> frameCoordinatesList = new ArrayList<>();

        for (GameActionRequest gameAction : requestDto.getAllFrames()) {
            try {

                String frameJson = objectMapper.writeValueAsString(gameAction.frames());
                FrameCoordinates frameCoordinates = FrameCoordinates.builder()
                        .music(music)
                        .timePassed(gameAction.timestamp().longValue())
                        .frameData(frameJson)
                        .build();

                frameCoordinatesList.add(frameCoordinates);
            } catch (Exception e) {
                throw new ApplicationException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        frameCoordinatesRepository.saveAll(frameCoordinatesList);
    }

}