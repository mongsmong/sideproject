package com.sideproject.sproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 20)
    private String postType;

    @Column(nullable = false)
    private int basePrice; // base_price

    // REQ-P01, REQ-P05: 상태 관리 필드
    @Builder.Default
    @Column(nullable = false)
    private String postStatus = "ON";

    @CreationTimestamp
    private LocalDateTime regDate;

    @Column(length = 100) // 길이는 원하는 대로 설정
    private String hashtag;
    
    // 테이블 설계의 FK (writer_id) 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private Account account;
}