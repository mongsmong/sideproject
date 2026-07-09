package com.sideproject.sproject.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sideproject.sproject.entity.Review;


public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Optional<Review> findByOrderId_OrderId(Long orderId);

    // 게시글의 모든 리뷰 (여러 거래에서 나온 것들 모아보기)
    List<Review> findByOrderId_BoardId_BoardIdOrderByRegDateDesc(Long boardId);
}
    
