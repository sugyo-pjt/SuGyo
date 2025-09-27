package com.sugyo.domain.game.dto.request;

import com.sugyo.domain.game.domain.Judgment;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClientCalculate {
    private double timestamp;
    private Judgment judgment;
}
