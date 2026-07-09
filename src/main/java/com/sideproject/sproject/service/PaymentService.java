package com.sideproject.sproject.service;

import com.sideproject.sproject.repository.OrderMessageRepository;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.sideproject.sproject.dto.PaymentDTO;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderMessage;
import com.sideproject.sproject.entity.Payment;
import com.sideproject.sproject.repository.OrderRepository;
import com.sideproject.sproject.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final OrderMessageRepository orderMessageRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${portone.imp-key}")
    private String impKey;
    @Value("${portone.imp-secret}")
    private String impSecret;

    // 결제 준비
    @Transactional
    public PaymentDTO preparePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 결제 금액 가져오기
        Integer amount = order.getTotalPrice();
        // 중복 방지용 현재 시간 붙이기
        String merchantUid = "ORDER_" + orderId + "_" + System.currentTimeMillis();

        Payment payment = paymentRepository.findByOrderId_OrderId(orderId)
                .orElseGet(() -> Payment.builder()
                        .orderId(order) // 처음 결제 시도할 때만 새로 만듦
                        .build());

        // 재시도 할 때마다 값 다시 세팅
        payment.setMerchantUid(merchantUid);
        payment.setAmount(amount); // 금액이 0원이 되지 않도록 확실히 덮어씌움
        payment.setPaymentStatus("READY");
        paymentRepository.save(payment);
        return toDTO(payment);
    }

    // 결제 검증
    @Transactional
    public void verifyPayment(String impUid, String merchantUid) throws Exception {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문번호입니다."));

        // 포트원 서버에서 실제 결제된 금액 조회 (merchant_uid 기반)
        Integer paidAmount = getPaymentAmount(merchantUid);

        if (!paidAmount.equals(payment.getAmount())) {
            throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
        }

        payment.setPaymentUid(impUid);
        payment.setPaymentStatus("PAID");

        Order order = payment.getOrderId();
        order.setOrderStatus("PROCESSING");

        // 결제 완료 시스템 메시지 추가
        OrderMessage paidMsg = OrderMessage.builder()
                .orderId(order)
                .senderId(order.getBuyerId())
                .messageType("PAID")
                .content("결제가 완료되었습니다.")
                .build();
        orderMessageRepository.save(paidMsg);

        paymentRepository.save(payment);
    }

    // merchant_uid로 결제 정보 조회 (단건조회 API 대체)
    private Integer getPaymentAmount(String merchantUid) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map response = restTemplate.exchange(
                "https://api.iamport.kr/payments/find/" + merchantUid + "/paid",
                HttpMethod.GET,
                entity,
                Map.class).getBody();

        Map<String, Object> responseBody = (Map<String, Object>) response.get("response");
        return (Integer) responseBody.get("amount");
    }

    // 결제 토근
    private String getAccessToken() {

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = new HashMap<>();
        body.put("imp_key", impKey);
        body.put("imp_secret", impSecret);
        Map response = restTemplate.postForObject(
                "https://api.iamport.kr/users/getToken",
                body,
                Map.class);

        Map<String, Object> responseBody = (Map<String, Object>) response.get("response");
        return (String) responseBody.get("access_token");

    }

    // 환불
    public void cancelPayment(String impUid, String reason) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAccessToken());
        headers.set("Content-Type", "application/json");

        Map<String, String> body = new HashMap<>();
        body.put("imp_uid", impUid);
        body.put("reason", reason);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.iamport.kr/payments/cancel",
                entity,
                Map.class);

        Map responseBody = (Map) response.getBody().get("response");
        if (responseBody == null) {
            throw new IllegalStateException("환불 처리에 실패했습니다.");
        }
    }

    private PaymentDTO toDTO(Payment payment) {
        return PaymentDTO.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId().getOrderId())
                .paymentUid(payment.getPaymentUid())
                .merchantUid(payment.getMerchantUid())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .regDate(payment.getRegDate())
                .modifiedDate(payment.getModifiedDate())
                .build();
    }

}
