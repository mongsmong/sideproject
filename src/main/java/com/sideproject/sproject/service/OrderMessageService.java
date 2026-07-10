package com.sideproject.sproject.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sideproject.sproject.entity.OrderFile;
import com.sideproject.sproject.repository.OrderFileRepository;
import com.sideproject.sproject.dto.OrderFileDTO;
import com.sideproject.sproject.dto.OrderMessageDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.ChatRoom;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderMessage;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.ChatRoomRepository;
import com.sideproject.sproject.repository.OrderMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderMessageService {
        private final AccountRepository accountRepository;
        private final OrderMessageRepository orderAttachRepository;
        private final OrderFileRepository orderFileRepository;
        private final Cloudinary cloudinary;
        private final ChatRoomRepository chatRoomRepository;

        // 메시지 저장
        @Transactional
        public void sendMessage(Long chatRoomId, String senderUsername, String content, List<MultipartFile> attachments)
                        throws IOException {
                ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
                Account sender = accountRepository.findByUsername(senderUsername)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                OrderMessage attach = OrderMessage.builder()
                                .chatRoom(chatRoom)
                                .senderId(sender)
                                .content(content)
                                .messageType("TALK")
                                .build();
                orderAttachRepository.save(attach);

                if (attachments != null) {
                        for (MultipartFile file : attachments) {
                                if (file.isEmpty())
                                        continue;

                                Map uploadResult = cloudinary.uploader().upload(
                                                file.getBytes(),
                                                ObjectUtils.asMap("resource_type", "auto"));

                                String fileUrl = (String) uploadResult.get("secure_url");
                                String storedName = (String) uploadResult.get("public_id");

                                OrderFile orderFile = OrderFile.builder()
                                                .messageId(attach)
                                                .originalName(file.getOriginalFilename())
                                                .storedName(storedName)
                                                .filePath(fileUrl)
                                                .fileSize(file.getSize())
                                                .contentType(file.getContentType())
                                                .build();
                                orderFileRepository.save(orderFile);
                        }
                }
        }

        // 메시지 목록 조회 (채팅방 전체, detail.html용)
        public List<OrderMessageDTO> getMessagesByChatRoom(Long chatRoomId) {
                return orderAttachRepository.findAllByChatRoomId(chatRoomId)
                                .stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        // 메시지 목록 조회 (특정 거래 하나만, info.html/info-request.html용) ← 새로 추가
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
                Long orderIdValue = null;
                String orderTitleValue = null;
                Integer totalPriceValue = null;

                if (attach.getOrderId() != null) {
                        Order order = attach.getOrderId();
                        orderIdValue = order.getOrderId();
                        orderTitleValue = order.getOrderTitle() != null ? order.getOrderTitle()
                                        : order.getBoardId().getTitle();
                        totalPriceValue = order.getTotalPrice();
                }

                return OrderMessageDTO.builder()
                                .messageId(attach.getMessageId())
                                .orderId(orderIdValue)
                                .orderTitle(orderTitleValue)
                                .totalPrice(totalPriceValue)
                                .senderUsername(attach.getSenderId().getUsername())
                                .senderNickname(attach.getSenderId().getNickname())
                                .senderProfileImageUrl(attach.getSenderId().getProfileImageUrl())
                                .content(attach.getContent())
                                .messageType(attach.getMessageType())
                                .regDate(attach.getRegDate())
                                .requestDeadline(attach.getRequestDeadline())
                                .requestPrice(attach.getRequestPrice())
                                .files(orderFileRepository.findByMessageId_MessageId(attach.getMessageId())
                                                .stream()
                                                .map(f -> OrderFileDTO.builder()
                                                                .fileId(f.getFileId())
                                                                .originalName(f.getOriginalName())
                                                                .filePath(f.getFilePath())
                                                                .contentType(f.getContentType())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }
}
