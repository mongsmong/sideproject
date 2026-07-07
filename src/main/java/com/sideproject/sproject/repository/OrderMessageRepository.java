package com.sideproject.sproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sideproject.sproject.entity.OrderMessage;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    List<OrderMessage> findByOrderId_OrderIdOrderByRegDateAsc(Long orderId);

    @Query("SELECT MAX(m.regDate) FROM OrderMessage m WHERE m.orderId.orderId = :orderId")
    LocalDateTime findLastMessageDate(@Param("orderId") Long orderId);

}