package com.sideproject.sproject.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sideproject.sproject.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long>{
    Optional<Payment> findByMerchantUid(String merchantUid); // 컬럼명을 바탕으로 연결 
    Optional<Payment> findByOrderId_OrderId(Long orderId);
    
    
    
}
