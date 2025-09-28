package com.sugyo.domain.game.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.dto.MotionFrame;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Converter
@Component
@RequiredArgsConstructor
@Slf4j
public class MotionFrameListConverter implements AttributeConverter<List<MotionFrame>, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(List<MotionFrame> motionFrames) {
        try {
            return objectMapper.writeValueAsString(motionFrames);
        } catch (JsonProcessingException e) {
            log.error("Error converting MotionFrame list to JSON", e);
            return null;
        }
    }

    @Override
    public List<MotionFrame> convertToEntityAttribute(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<MotionFrame>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to MotionFrame list: {}", json, e);
            return null;
        }
    }
}