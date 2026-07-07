package com.sideproject.sproject.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sideproject.sproject.dto.OrderDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Board;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderFile;
import com.sideproject.sproject.entity.OrderMessage;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.OrderFileRepository;
import com.sideproject.sproject.repository.OrderMessageRepository;
import com.sideproject.sproject.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
public class OrderService {
        private final OrderRepository orderRepository;
        private final OrderMessageRepository orderMessageRepository;
        private final AccountRepository accountRepository;
        private final OrderFileRepository orderFileRepository;

        @Transactional
        public Long saveOrder(OrderDTO dto, Board board, Account buyer) {

                Optional<Order> existingOrder = orderRepository.findByBoardId_BoardIdAndBuyerId_AccountId(
                                board.getBoardId(), buyer.getAccountId());

                Order order;
                boolean isNewEngagement;

                if (existingOrder.isPresent()) {
                        order = existingOrder.get();
                        isNewEngagement = "COMPLETED".equals(order.getOrderStatus())
                                        || "CANCELED".equals(order.getOrderStatus());
                } else {
                        order = null;
                        isNewEngagement = true;
                }

                // 새로운 거래를 시작하는 경우에만 슬롯 체크
                if (isNewEngagement) {
                        Account writer = board.getAccount();
                        if (writer.getMaxSlots() != null && !writer.isAllowOverbooking()) {
                                long activeCount = orderRepository.countActiveOrdersByWriter(writer.getAccountId());
                                if (activeCount >= writer.getMaxSlots()) {
                                        throw new IllegalStateException("작가의 작업 슬롯이 가득 찼습니다. 나중에 다시 시도해주세요.");
                                }
                        }
                }

                if (order == null) {
                        order = Order.builder()
                                        .boardId(board)
                                        .buyerId(buyer)
                                        .content(dto.getContent())
                                        .deadline(dto.getDeadline())
                                        .totalPrice(dto.getTotalPrice())
                                        .orderStatus("REQUEST")
                                        .build();
                        orderRepository.save(order);
                } else if (isNewEngagement) {
                        order.setOrderStatus("REQUEST");
                        order.setContent(dto.getContent());
                        order.setDeadline(dto.getDeadline());
                        order.setTotalPrice(dto.getTotalPrice());
                        order.setOrderTitle(null);
                }

                OrderMessage newRequestMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(buyer)
                                .messageType("NEW_REQUEST")
                                .content(dto.getContent())
                                .requestDeadline(dto.getDeadline())
                                .requestPrice(dto.getTotalPrice())
                                .build();
                orderMessageRepository.save(newRequestMsg);

                return order.getOrderId();
        }

        // 의뢰 수락
        @Transactional
        public void approveWork(Long orderId, String username) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

                if (!order.getBuyerId().getUsername().equals(username)) {
                        throw new IllegalStateException("승인 권한이 없습니다.");
                }

                if (!"SUBMITTED".equals(order.getOrderStatus())) {
                        throw new IllegalStateException("제출된 작업물이 없습니다.");
                }

                order.setOrderStatus("COMPLETED");

                OrderMessage approveMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(order.getBuyerId())
                                .messageType("APPROVED")
                                .content("의뢰인이 작업물을 최종 승인했습니다.")
                                .build();
                orderMessageRepository.save(approveMsg);
        }

        // 회원의 주문 내역 목록 조회
        public List<OrderDTO> getOrderList(String username) {
                // 의뢰인+작성자 주문 리스트 조회
                List<Order> buyerOrders = orderRepository.findByBuyerId_Username(username);
                List<Order> sellerOrders = orderRepository.findByBoardId_Account_Username(username);
                buyerOrders.addAll(sellerOrders);

                // 전체 주문 리스트를 DTO로 변환하여 반환
                return buyerOrders.stream()
                                .distinct() // 중복 제거
                                .map(this::toDTO)
                                .sorted((a, b) -> {
                                        LocalDateTime dateA = a.getLastMessageDate() != null ? a.getLastMessageDate()
                                                            : a.getRegDate();
                                        LocalDateTime dateB = b.getLastMessageDate() != null ? b.getLastMessageDate()
                                                            : b.getRegDate();
                                        return dateB.compareTo(dateA); // 최신 채팅이 위로
                                })
                                .collect(Collectors.toList());
        }

        // 내역 목록 상세 조회
        public OrderDTO getOrderDetail(Long orderId) {
                OrderDTO dto = new OrderDTO();
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. id=" + orderId));
                // 3. 구매자 정보 매핑 (추가)
                dto.setBuyerUsername(order.getBuyerId().getUsername());

                // // 4. 작성자 정보 매핑 (추가)
                // dto.setBuyerUsername(order.getBuyer().getUsername());
                // dto.setBoardWriterUsername(order.getBoard().getAccount().getUsername());

                return toDTO(order);

        }

        // 결제요청(의뢰 수락) (REQUEST > PAYMENT_WAITING)
        @Transactional
        public void confirmRequest(Long orderId, Integer updatedPrice, String orderTitle, String username) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. id=" + orderId));

                Account writerId = accountRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                order.setTotalPrice(updatedPrice);
                if (orderTitle != null && !orderTitle.isBlank()) {
                        order.setOrderTitle(orderTitle);
                }
                order.setOrderStatus("PAYMENT_WAITING");

                String displayTitle = order.getOrderTitle() != null ? order.getOrderTitle()
                                : order.getBoardId().getTitle();

                OrderMessage payRequestMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(writerId)
                                .messageType("PAY_REQUEST")
                                .content("[" + displayTitle + "]에 대한 결제 요청이 도착했습니다. (" + updatedPrice + "원)")
                                .build();

                orderMessageRepository.save(payRequestMsg);
        }

        // 의뢰 거절/취소 (REQUEST 또는 PAYMENT_WAITING > CANCELED)
        @Transactional
        public void cancelOrder(Long orderId, String username) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. id=" + orderId));

                boolean isBuyer = order.getBuyerId().getUsername().equals(username);
                boolean isWriter = order.getBoardId().getAccount().getUsername().equals(username);

                if (!isBuyer && !isWriter) {
                        throw new IllegalStateException("취소 권한이 없습니다.");
                }

                // 이미 취소되었거나, 결제가 끝나 진행 중인 주문은 취소 불가
                if ("CANCELED".equals(order.getOrderStatus()) || "PROCESSING".equals(order.getOrderStatus())) {
                        throw new IllegalStateException("이미 취소되었거나 진행 중인 주문은 취소할 수 없습니다.");
                }

                Account actor = accountRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                order.setOrderStatus("CANCELED");

                String actorLabel = isWriter ? "작가" : "의뢰인";
                OrderMessage cancelMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(actor)
                                .messageType("CANCELED")
                                .content(actor.getNickname() + "(" + actorLabel + ")님이 의뢰를 취소했습니다.")
                                .build();

                orderMessageRepository.save(cancelMsg);
        }

        private final Cloudinary cloudinary;

        // 작업물 제출
        @Transactional
        public void submitWork(Long orderId, String username, String content, List<MultipartFile> files)
                        throws IOException {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

                Account writer = accountRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                order.setOrderStatus("SUBMITTED");

                OrderMessage submitMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(writer)
                                .messageType("SUBMIT")
                                .content(content)
                                .build();
                orderMessageRepository.save(submitMsg);

                if (files != null) {
                        for (MultipartFile file : files) {
                                if (file.isEmpty())
                                        continue;

                                Map uploadResult = cloudinary.uploader().upload(
                                                file.getBytes(),
                                                ObjectUtils.asMap("resource_type", "auto") // 이미지/PDF/zip 등 자동 판별
                                );

                                String fileUrl = (String) uploadResult.get("secure_url");
                                String storedName = (String) uploadResult.get("public_id");

                                OrderFile orderFile = OrderFile.builder()
                                                .messageId(submitMsg)
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

        // 수정 요청 (완료 반려)
        @Transactional
        public void requestRevision(Long orderId, String username, String content) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

                if (!order.getBuyerId().getUsername().equals(username)) {
                        throw new IllegalStateException("수정 요청 권한이 없습니다.");
                }

                if (!"SUBMITTED".equals(order.getOrderStatus())) {
                        throw new IllegalStateException("제출된 작업물이 없습니다.");
                }

                order.setOrderStatus("REVISION_REQUESTED");

                OrderMessage revisionMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(order.getBuyerId())
                                .messageType("REVISION_REQUEST")
                                .content(content)
                                .build();
                orderMessageRepository.save(revisionMsg);
        }

        private OrderDTO toDTO(Order order) {
                return OrderDTO.builder()
                                .orderId(order.getOrderId())
                                .boardId(order.getBoardId().getBoardId())
                                .boardTitle(order.getBoardId().getTitle())
                                .orderTitle(order.getOrderTitle() != null ? order.getOrderTitle()
                                                : order.getBoardId().getTitle())
                                .boardWriterUsername(order.getBoardId().getAccount().getUsername()) // 판매자
                                .boardWriterProfileImageUrl(order.getBoardId().getAccount().getProfileImageUrl())

                                .buyerUsername(order.getBuyerId().getUsername()) // 구매자
                                .buyerNickname(order.getBuyerId().getNickname())
                                .buyerProfileImageUrl(order.getBuyerId().getProfileImageUrl())
                                .content(order.getContent())
                                .deadline(order.getDeadline())
                                .totalPrice(order.getTotalPrice())
                                .orderStatus(order.getOrderStatus())
                                .regDate(order.getRegDate())
                                .lastMessageDate(orderMessageRepository.findLastMessageDate(order.getOrderId()))
                                .build();
        }
}