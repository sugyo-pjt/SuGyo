package com.sugyo.domain.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "daily")
@Getter
@Setter
@NoArgsConstructor
public class Daily {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer day;

    @Column
    private String sentence;

    @Column
    private Integer totalCount;
}