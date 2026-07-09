package com.sideproject.sproject.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "REVIEW") // 테이블 명시
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id") // DB 컬럼명과 매핑
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false, unique = true) // BOARD 테이블의 FK 매핑
    private Board boardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id") // Account 테이블(의뢰인) FK 매핑
    private Account buyerId; // 엔티티 클래스명이 Member라고 가정

    @Column(nullable = false)
    private Integer rating; // 별점

    @Column(nullable = false, length = 255) // VARCHAR
    private String content; // 후기 본문

    @Column(name = "reg_date", updatable = false)
    @CreationTimestamp // 자동으로 현재 시간 입력
    private LocalDateTime regDate;
}