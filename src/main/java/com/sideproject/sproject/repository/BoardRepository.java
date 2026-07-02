package com.sideproject.sproject.repository;

import com.sideproject.sproject.entity.Board;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByAccount_Username(String username);


   // 1. 카테고리와 검색어 둘 다 있을 때 
    @Query("SELECT b FROM Board b WHERE b.category = :category AND " + 
           "(b.title LIKE %:keyword% OR b.content LIKE %:keyword% OR b.hashtag LIKE %:keyword%)")
    Page<Board> searchByCategoryAndKeyword(@Param("category") String category, 
                                          @Param("keyword") String keyword, 
                                          Pageable pageable); 

    // 2. 카테고리만 있을 때 (Pageable 추가)
    Page<Board> findByCategory(String category, Pageable pageable); 

    // 3. 검색어만 있을 때 (Pageable 추가)
    @Query("SELECT b FROM Board b WHERE " +
           "b.title LIKE %:keyword% OR b.content LIKE %:keyword% OR b.hashtag LIKE %:keyword%")
    Page<Board> searchByKeyword(@Param("keyword") String keyword, 
                                Pageable pageable); 

    @Query("SELECT b.hashtag FROM Board b WHERE b.hashtag IS NOT NULL AND b.hashtag != '' GROUP BY b.hashtag ORDER BY COUNT(b.boardId) DESC")
    List<String> findPopularHashtags(Pageable pageable);
    
}