package com.sideproject.sproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "ORDER_REQUEST")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId; // PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board boardId;

    @Column(length = 100)
    private String orderTitle; // 작업물 제목 (결제 요청 시 작가가 수정 가능, 없으면 게시글 제목 사용)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Account buyerId;

    @Column(nullable = false)
    private String content; // 의뢰인이 작성한 신청서 본문

    private LocalDate deadline; // 마감일

    @Column(nullable = false, length = 20)
    @ColumnDefault("'REQUEST'") // 기본값 REQUEST
    private String orderStatus;

    @Column(nullable = false)
    private Integer totalPrice; // 최종 결제 금액

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime regDate; // 주문 일시

    @UpdateTimestamp
    @Column
    private LocalDateTime modifiedDate; // 주문 상태 변경 일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(length = 20)
    private String preRefundStatus; // 환불 요청 전 상태 (거절 복원용)

    @Column
    private String refundRequestedBy; // 환불 요청한 쪽의 username
}