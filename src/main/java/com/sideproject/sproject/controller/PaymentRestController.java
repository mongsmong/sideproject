package com.sideproject.sproject.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sideproject.sproject.dto.PaymentDTO;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.Payment;
import com.sideproject.sproject.repository.OrderRepository;
import com.sideproject.sproject.repository.PaymentRepository;
import com.sideproject.sproject.service.PaymentService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentRestController {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // 테스트 결제용
    @PostMapping("/pay-test")
    public String payTest(@RequestParam Long orderId) {
    // 1. 주문 조회
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문 내역 없음"));

    // 2. 주문 상태 변경 (String 타입 그대로 사용)
    order.setOrderStatus("PROCESSING");
    orderRepository.save(order);

    // 3. 결제 테이블 상태 변경 
    // (Repository에 오타난 메서드명 findByOrder_OderId 그대로 사용)
    Payment payment = paymentRepository.findByOrderId_OrderId(orderId)
            .orElseGet(() -> Payment.builder()
                    .orderId(order)
                    .amount(order.getTotalPrice()) // Order 엔티티에서 금액 가져오기
                    .paymentStatus("READY")
                    .build());

    // 4. 결제 상태 변경 (String 타입)
    payment.setPaymentStatus("PAID"); 
    paymentRepository.save(payment);

    return "redirect:/order/detail/" + orderId;
}


    // 결제 준비
    // 결제창 띄울 때 필요
    @PostMapping("/prepare/{orderId}")
    public ResponseEntity<PaymentDTO> preparePayment(@PathVariable Long orderId) {
    PaymentDTO dto = paymentService.preparePayment(orderId);
        return ResponseEntity.ok(dto);
        
    }

    // 결제 검증 
    // 결제 완료 후 imp_uid 보낼때 호출
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment (@RequestBody Map<String, String>body) throws Exception {
        String impUid = body.get("imp_uid");
        String merchantUid = body.get("merchant_uid");

        paymentService.verifyPayment(impUid, merchantUid);

        return ResponseEntity.ok("결제 검증 및 상태 업데이트가 완료되었습니다.");
        
    }
    
}
