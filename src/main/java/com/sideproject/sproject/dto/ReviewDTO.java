package com.sideproject.sproject.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {

    private Long reviewId;
    private Long boardId;    // 게시글 연결을 위한 ID
    private Long buyerId;    // 작성자(구매자) 연결을 위한 ID
    private String buyerNickname; // 화면 표시용 닉네임
    private Integer rating;  // 별점
    private String content;  // 리뷰 내용
    private LocalDateTime regDate;
}