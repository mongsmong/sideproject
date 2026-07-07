package com.sideproject.sproject.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "ORDER_ATTACH")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private Order orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Account senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = true) 
    private ChatRoom chatRoom;

    @Column(length = 1000)
    private String content;

    @Column(length = 50)
    private String messageType; // 예: "TALK" (일반채팅), "PAY_REQUEST" (결제요청)

    @Column
    private LocalDate requestDeadline;

    @Column
    private Integer requestPrice;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime regDate;

}