package com.sugyo.domain.game.repository;

import com.sugyo.domain.game.entity.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {
    
    List<Music> findByTitleContaining(String title);
    
    List<Music> findBySingerContaining(String singer);
    
    @Query("SELECT m FROM Music m WHERE m.title LIKE %:keyword% OR m.singer LIKE %:keyword%")
    List<Music> findByTitleOrSingerContaining(@Param("keyword") String keyword);
}