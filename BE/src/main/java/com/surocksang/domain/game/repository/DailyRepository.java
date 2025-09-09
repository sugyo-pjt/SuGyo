package com.surocksang.domain.game.repository;

import com.surocksang.domain.entity.Daily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyRepository extends JpaRepository<Daily, Long> {
    
    Optional<Daily> findByDay(Integer day);
    
    boolean existsByDay(Integer day);
}