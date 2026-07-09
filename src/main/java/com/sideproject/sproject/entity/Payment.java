package com.sideproject.sproject.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "PAYMENT")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order orderId;

    @Column(length = 100)
    private String paymentUid; // 포트원 발급 결제 고유 번호 (NULL 허용)

    @Column(length = 100, unique = true)
    private String merchantUid; // 우리 서버 발행 주문 번호 (NULL 허용)

    @Column(nullable = false, length = 20)
    @ColumnDefault("'READY'")
    private String paymentStatus; // READY / PAID / CANCELLED

    @Column(nullable = false)
    private Integer amount; // 실제 결제 금액

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime regDate; // 결제 시도 일시

    @UpdateTimestamp
    @Column
    private LocalDateTime modifiedDate; // 결제 상태 변경 일시

    // 환불용
    @Column
    private Integer refundedAmount; // 가격

    @Column(length = 500)
    private String refundReason; // 사유
}
