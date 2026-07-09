package com.sideproject.sproject.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sideproject.sproject.dto.ReviewDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.Review;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.OrderRepository;
import com.sideproject.sproject.repository.ReviewRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void register(Long orderId, String username, Integer rating, String content) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getBuyerId().getUsername().equals(username)) {
            throw new IllegalStateException("리뷰 작성 권한이 없습니다.");
        }

        if (!"COMPLETED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("완료된 작업에만 리뷰를 작성할 수 있습니다.");
        }

        if (reviewRepository.findByOrderId_OrderId(orderId).isPresent()) {
            throw new IllegalStateException("이미 리뷰를 작성하셨습니다.");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }

        Account buyer = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Review review = Review.builder()
                .orderId(order)
                .buyerId(buyer)
                .rating(rating)
                .content(content)
                .build();

        reviewRepository.save(review);
    }

    public List<ReviewDTO> getReviewsByBoard(Long boardId) {
        return reviewRepository.findByOrderId_BoardId_BoardIdOrderByRegDateDesc(boardId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public boolean hasReview(Long orderId) {
        return reviewRepository.findByOrderId_OrderId(orderId).isPresent();
    }

    private ReviewDTO toDTO(Review review) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .orderId(review.getOrderId().getOrderId())
                .orderTitle(review.getOrderId().getOrderTitle() != null
                        ? review.getOrderId().getOrderTitle()
                        : review.getOrderId().getBoardId().getTitle())
                .buyerId(review.getBuyerId().getAccountId())
                .buyerNickname(review.getBuyerId().getNickname())
                .buyerProfileImageUrl(review.getBuyerId().getProfileImageUrl())
                .rating(review.getRating())
                .content(review.getContent())
                .regDate(review.getRegDate())
                .build();
    }
}