package com.surocksang.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends CreatedOnlyEntity {

    @UpdateTimestamp
    @Column(nullable = false)
    protected LocalDateTime updatedAt;
}
