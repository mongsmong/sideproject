package com.sideproject.sproject.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    private Long paymentId;
    private Long orderId; // Order 엔티티 참조 대신 ID만
    private String paymentUid; // 포트원 발급 결제 고유 번호
    private String merchantUid; // 우리 서버 발행 주문 번호
    private String paymentStatus; // READY / PAID / CANCELLED
    private Integer amount; // 실제 결제 금액
    private LocalDateTime regDate; // 결제 시도 일시
    private LocalDateTime modifiedDate; // 결제 상태 변경 일시
}
