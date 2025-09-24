package com.sugyo.domain.game.exception;

import lombok.Getter;

@Getter
public class WebSocketException extends RuntimeException{

    private final WebSocketErrorCode errorCode;

    public WebSocketException(WebSocketErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
