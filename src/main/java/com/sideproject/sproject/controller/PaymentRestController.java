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
import org.springframework.web.bind.annotation.ExceptionHandler;
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


    // 예외처리용
    @ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
    public ResponseEntity<String> handlePaymentException(Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
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
    public ResponseEntity<String> verifyPayment(@RequestBody Map<String, String> body) throws Exception {
        String impUid = body.get("imp_uid");
        String merchantUid = body.get("merchant_uid");

        paymentService.verifyPayment(impUid, merchantUid);

        return ResponseEntity.ok("결제 검증 및 상태 업데이트가 완료되었습니다.");

    }

}
