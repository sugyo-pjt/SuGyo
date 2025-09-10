package com.surocksang.common.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FilePath {
    USER_PROFILE("/user/profile");

    private final String path;
}
