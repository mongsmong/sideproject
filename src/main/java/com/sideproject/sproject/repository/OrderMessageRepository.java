package com.sideproject.sproject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sideproject.sproject.entity.OrderMessage;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    List<OrderMessage> findByOrderId_OrderIdOrderByRegDateAsc(Long orderId);

}