package com.sideproject.sproject.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    
    // 1. 주문 기본 정보
    private Long orderId;       // 주문 ID

    // 2. 게시글 정보
    private Long boardId;       // 게시글 ID
    private String boardTitle;  // 게시글 제목
    private String boardWriterUsername; // 추가: principal.getName()과 비교할 필드
    private String orderTitle;

    // 3. 구매자 정보
    private Long buyerId;       // 구매자 ID
    private String buyerNickname; // 구매자 닉네임
    private String buyerUsername;      // 추가: principal.getName()과 비교할 필드

    // 4. 상세 내용 및 상태
    private String content;     // 요구사항
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;   // 마감일
    private Integer totalPrice;   // 최종 금액
    private String orderStatus;   // 주문 상태
    private LocalDateTime regDate; // 주문 일시 
    

}