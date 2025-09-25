package com.sugyo.domain.study.repository;

import com.sugyo.domain.study.entity.Daily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyRepository extends JpaRepository<Daily, Long> {

}