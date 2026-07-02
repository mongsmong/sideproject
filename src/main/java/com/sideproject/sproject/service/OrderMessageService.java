package com.sideproject.sproject.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sideproject.sproject.dto.OrderFileDTO;
import com.sideproject.sproject.dto.OrderMessageDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderMessage;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.OrderFileRepository;
import com.sideproject.sproject.repository.OrderMessageRepository;
import com.sideproject.sproject.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderMessageService {
        private final OrderRepository orderRepository;
        private final AccountRepository accountRepository;
        private final OrderMessageRepository orderAttachRepository;
        private final OrderFileRepository orderFileRepository;

        // 메시지 저장
        @Transactional
        public void sendMessage(Long orderId, String senderUsername, String content) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
                Account sender = accountRepository.findByUsername(senderUsername)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
                OrderMessage attach = OrderMessage.builder()
                                .orderId(order)
                                .senderId(sender)
                                .content(content)
                                .messageType("TALK")
                                .build();
                orderAttachRepository.save(attach);
        }

        // 메시지 목록 조회
        public List<OrderMessageDTO> getMessages(Long orderId) {
                return orderAttachRepository.findByOrderId_OrderIdOrderByRegDateAsc(orderId)
                                .stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public OrderMessageDTO getMessage(Long messageId) {
                OrderMessage msg = orderAttachRepository.findById(messageId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지입니다."));
                return toDTO(msg);
        }

        // DTO 변환
        private OrderMessageDTO toDTO(OrderMessage attach) {
                return OrderMessageDTO.builder()
                                .messageId(attach.getMessageId())
                                .orderId(attach.getOrderId().getOrderId())
                                .senderUsername(attach.getSenderId().getUsername())
                                .senderNickname(attach.getSenderId().getNickname())
                                .content(attach.getContent())
                                .messageType(attach.getMessageType())
                                .requestDeadline(attach.getRequestDeadline())
                                .requestPrice(attach.getRequestPrice())
                                .regDate(attach.getRegDate())
                                .files(orderFileRepository.findByMessageId_MessageId(attach.getMessageId())
                                                .stream()
                                                .map(f -> OrderFileDTO.builder()
                                                                .fileId(f.getFileId())
                                                                .originalName(f.getOriginalName())
                                                                .filePath(f.getFilePath())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }
}
