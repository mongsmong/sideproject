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

       // 1. 카테고리 + 검색어
       @Query("SELECT b FROM Board b WHERE b.category = :category AND " +
                     "(b.title LIKE %:keyword% OR b.content LIKE %:keyword% OR b.hashtag LIKE %:keyword%)")
       Page<Board> searchByCategoryAndKeyword(@Param("category") String category,
                     @Param("keyword") String keyword,
                     Pageable pageable);

       // 2. 카테고리만
       Page<Board> findByCategory(String category, Pageable pageable);

       // 3. 검색어만
       @Query("SELECT b FROM Board b WHERE " +
                     "b.title LIKE %:keyword% OR b.content LIKE %:keyword% OR b.hashtag LIKE %:keyword%")
       Page<Board> searchByKeyword(@Param("keyword") String keyword,
                     Pageable pageable);

       // 4. 해시태그만
       Page<Board> findByHashtagContaining(String hashtag, Pageable pageable);

       // 5. 카테고리 + 해시태그
       Page<Board> findByCategoryAndHashtagContaining(String category, String hashtag, Pageable pageable);

       // 6. 검색어 + 해시태그
       @Query("SELECT b FROM Board b WHERE b.hashtag LIKE %:hashtag% AND " +
                     "(b.title LIKE %:keyword% OR b.content LIKE %:keyword%)")
       Page<Board> searchByKeywordAndHashtag(@Param("keyword") String keyword,
                     @Param("hashtag") String hashtag,
                     Pageable pageable);

       // 7. 카테고리 + 검색어 + 해시태그 (전체 조합)
       @Query("SELECT b FROM Board b WHERE b.category = :category AND b.hashtag LIKE %:hashtag% AND " +
                     "(b.title LIKE %:keyword% OR b.content LIKE %:keyword%)")
       Page<Board> searchByCategoryAndKeywordAndHashtag(@Param("category") String category,
                     @Param("keyword") String keyword,
                     @Param("hashtag") String hashtag,
                     Pageable pageable);

       // 인기 해시태그
       @Query("SELECT b.hashtag FROM Board b WHERE b.hashtag IS NOT NULL AND b.hashtag != ''")
       List<String> findAllHashtagStrings();

       // 인기순 정렬
       @Query(value = "SELECT b.* FROM board b " +
                     "LEFT JOIN (SELECT board_id, COUNT(*) AS cnt FROM order_request " +
                     "WHERE order_status = 'COMPLETED' GROUP BY board_id) o " +
                     "ON b.board_id = o.board_id " +
                     "ORDER BY COALESCE(o.cnt, 0) DESC, b.board_id DESC", countQuery = "SELECT COUNT(*) FROM board", nativeQuery = true)
       Page<Board> findAllOrderByCompletedCount(Pageable pageable);

       // 구해요 제외 쿼리
       Page<Board> findByCategoryNot(String category, Pageable pageable);

       @Query("SELECT b FROM Board b WHERE b.category != :category AND " +
                     "(b.title LIKE %:keyword% OR b.content LIKE %:keyword% OR b.hashtag LIKE %:keyword%)")
       Page<Board> searchByKeywordExcludingCategory(@Param("category") String category,
                     @Param("keyword") String keyword,
                     Pageable pageable);

}