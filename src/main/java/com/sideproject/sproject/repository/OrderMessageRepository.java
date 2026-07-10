package com.sideproject.sproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sideproject.sproject.entity.OrderMessage;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {

        // 특정 거래(Order) 하나의 메시지만 (info.html용)
        List<OrderMessage> findByOrderId_OrderIdOrderByRegDateAsc(Long orderId);

        @Query("SELECT MAX(m.regDate) FROM OrderMessage m WHERE m.orderId.orderId = :orderId")
        LocalDateTime findLastMessageDate(@Param("orderId") Long orderId);

        // 채팅방 전체 메시지 모으기 (여러 거래 아우름, detail.html용)
        @Query("SELECT m FROM OrderMessage m " +
                        "LEFT JOIN m.orderId o " +
                        "LEFT JOIN o.chatRoom oc " +
                        "WHERE m.chatRoom.chatRoomId = :chatRoomId OR oc.chatRoomId = :chatRoomId " +
                        "ORDER BY m.regDate ASC")
        List<OrderMessage> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);

        // 메세지 시각 구하는
        @Query("SELECT MAX(m.regDate) FROM OrderMessage m WHERE " +
                        "(m.chatRoom.chatRoomId = :chatRoomId) OR (m.orderId.chatRoom.chatRoomId = :chatRoomId)")
        LocalDateTime findLastMessageDateByChatRoom(@Param("chatRoomId") Long chatRoomId);

}