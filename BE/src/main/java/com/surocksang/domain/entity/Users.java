//package com.surocksang.domain.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "Users")
//@Getter
//@Setter
//@NoArgsConstructor
//public class Users {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, length = 100, unique = true)
//    private String email;
//
//    @Column(nullable = false, length = 100)
//    private String password;
//
//    @Column(nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(nullable = false)
//    private LocalDateTime updatedAt;
//
//    @Column(nullable = false, length = 10)
//    private String nickname;
//
//    @Column(length = 255)
//    private String profileImageUrl;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//}