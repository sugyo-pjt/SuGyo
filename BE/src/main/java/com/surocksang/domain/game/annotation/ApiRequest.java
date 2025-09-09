package com.surocksang.domain.game.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD}) // 메서드에만 Annotation이 적용되도록 함
@Retention(RetentionPolicy.RUNTIME) // Runtime에 적용
@RequestBody // 적용할 Annotation
public @interface ApiRequest { // @interface -> Annotation 생성
    Content[] content() default {};
    boolean required() default false;
    String description() default "";
}
