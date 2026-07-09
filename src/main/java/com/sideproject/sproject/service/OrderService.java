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
import com.sideproject.sproject.dto.ChatRoomDTO;
import com.sideproject.sproject.dto.OrderDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Board;
import com.sideproject.sproject.entity.ChatRoom;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderFile;
import com.sideproject.sproject.entity.OrderMessage;
import com.sideproject.sproject.entity.Payment;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.ChatRoomRepository;
import com.sideproject.sproject.repository.OrderFileRepository;
import com.sideproject.sproject.repository.OrderMessageRepository;
import com.sideproject.sproject.repository.OrderRepository;
import com.sideproject.sproject.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
public class OrderService {
        private final OrderRepository orderRepository;
        private final OrderMessageRepository orderMessageRepository;
        private final AccountRepository accountRepository;
        private final OrderFileRepository orderFileRepository;
        private final ChatRoomRepository chatRoomRepository;

        @Transactional
        public Long saveOrder(OrderDTO dto, Board board, Account buyer) {

                // 1. 채팅방 찾기 또는 생성 (게시글+의뢰인당 하나만 유지)
                ChatRoom chatRoom = chatRoomRepository.findByBoardId_BoardIdAndBuyerId_AccountId(
                                board.getBoardId(), buyer.getAccountId())
                                .orElseGet(() -> chatRoomRepository.save(
                                                ChatRoom.builder()
                                                                .boardId(board)
                                                                .buyerId(buyer)
                                                                .build()));

                // 2. 슬롯 체크 (신청마다 매번 체크, 새 거래니까)
                Account writer = board.getAccount();
                if (writer.getMaxSlots() != null && !writer.isAllowOverbooking()) {
                        long activeCount = orderRepository.countActiveOrdersByWriter(writer.getAccountId());
                        if (activeCount >= writer.getMaxSlots()) {
                                throw new IllegalStateException("작가의 작업 슬롯이 가득 찼습니다. 나중에 다시 시도해주세요.");
                        }
                }

                // 3. 신청할 때마다 항상 새로운 Order(거래) 생성
                Order order = Order.builder()
                                .chatRoom(chatRoom)
                                .boardId(board)
                                .buyerId(buyer)
                                .content(dto.getContent())
                                .deadline(dto.getDeadline())
                                .totalPrice(dto.getTotalPrice())
                                .orderStatus("REQUEST")
                                .build();
                orderRepository.save(order);

                // 4. 신청 알림 메시지 저장 (이 Order에 귀속)
                OrderMessage newRequestMsg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(buyer)
                                .messageType("NEW_REQUEST")
                                .content(dto.getContent())
                                .requestDeadline(dto.getDeadline())
                                .requestPrice(dto.getTotalPrice())
                                .build();
                orderMessageRepository.save(newRequestMsg);

                return chatRoom.getChatRoomId();
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

        // 채팅방 조회
        public ChatRoomDTO getChatRoomDetail(Long chatRoomId) {
                ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

                return ChatRoomDTO.builder()
                                .chatRoomId(chatRoom.getChatRoomId())
                                .boardId(chatRoom.getBoardId().getBoardId())
                                .boardTitle(chatRoom.getBoardId().getTitle())
                                .boardWriterUsername(chatRoom.getBoardId().getAccount().getUsername())
                                .boardWriterProfileImageUrl(chatRoom.getBoardId().getAccount().getProfileImageUrl())
                                .buyerUsername(chatRoom.getBuyerId().getUsername())
                                .buyerNickname(chatRoom.getBuyerId().getNickname())
                                .buyerProfileImageUrl(chatRoom.getBuyerId().getProfileImageUrl())
                                .build();
        }

        // 작업 목록 조회
        public List<OrderDTO> getWorkList(String username) {
                return orderRepository.findAllByUsername(username)
                                .stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        // 작업 목록 role 조회
        public List<OrderDTO> getWorkList(String username, String role) {

                //  role 파라미터(all/writer/buyer) 조회할 거래 목록을 다르게 
                List<Order> orders;
                if ("writer".equals(role)) {
                        orders = orderRepository.findAllByWriterUsername(username);
                } else if ("buyer".equals(role)) {
                        orders = orderRepository.findByBuyerId_Username(username);
                } else {
                        orders = orderRepository.findAllByUsername(username);
                }

                // 이미 끝난 거래 < 상태 목록 정의 :: 맨 아래로
                List<String> finishedStatuses = List.of("COMPLETED", "REFUNDED", "CANCELED");

                return orders.stream()
                                .map(this::toDTO) // Order 엔티티 리스트를 화면에서 쓸 OrderDTO 리스트로 변환
                                // 3. sorted()에 비교 규칙(Comparator)을 직접 넣어서 정렬
                                // 자바의 sorted는 (a, b)를 비교해서
                                // 음수 반환 → a가 앞으로, 양수 반환 → b가 앞으로, 0 → 순서 유지
                                .sorted((a, b) -> {

                                        // 3-1. 지금 비교 중인 두 거래가 각각 "끝난 거래"인지 여부를 미리 판단
                                        boolean aFinished = finishedStatuses.contains(a.getOrderStatus());
                                        boolean bFinished = finishedStatuses.contains(b.getOrderStatus());

                                        // 3-2. 한쪽만 끝났고 한쪽은 진행 중이면,
                                        // 무조건 "진행 중인 쪽"이 위로 오도록 정렬
                                        // (a가 끝났으면 1을 반환해서 a를 뒤로 보냄,
                                        // a가 진행중이면 -1을 반환해서 a를 앞으로 보냄)
                                        if (aFinished != bFinished) {
                                                return aFinished ? 1 : -1;
                                        }

                                        // 3-3. 둘 다 아직 진행 중인 경우 → 마감일이 빠른 순서로 정렬
                                        // 마감일이 없는 거래는 뒤로 보냄 (기준을 정하기 애매하니까)
                                        if (!aFinished) {
                                                if (a.getDeadline() == null)
                                                        return 1; // a만 마감일 없음 → a를 뒤로
                                                if (b.getDeadline() == null)
                                                        return -1; // b만 마감일 없음 → b를 뒤로
                                                return a.getDeadline().compareTo(b.getDeadline()); // 마감일 오름차순(빠른 게 위)
                                        }

                                        // 3-4. 둘 다 이미 끝난 거래인 경우 → 최근에 생성된 순서로 정렬
                                        // (b.compareTo(a)로 바꿔서 내림차순, 즉 최신이 위로 오게 함)
                                        return b.getRegDate().compareTo(a.getRegDate());
                                })
                                .collect(Collectors.toList()); // 정렬 끝난 스트림을 다시 List로 변환
        }

        // 회원의 주문 내역 목록 조회
        public List<OrderDTO> getOrderList(String username) {
                List<ChatRoom> buyerRooms = chatRoomRepository.findByBuyerId_Username(username);
                List<ChatRoom> writerRooms = chatRoomRepository.findByBoardId_Account_Username(username);

                buyerRooms.addAll(writerRooms);

                return buyerRooms.stream()
                                .distinct()
                                .map(room -> {
                                        Order latestOrder = orderRepository
                                                        .findTopByChatRoom_ChatRoomIdOrderByRegDateDesc(
                                                                        room.getChatRoomId())
                                                        .orElse(null);

                                        LocalDateTime lastMsgDate = orderMessageRepository
                                                        .findLastMessageDateByChatRoom(room.getChatRoomId());

                                        return OrderDTO.builder()
                                                        .chatRoomId(room.getChatRoomId())
                                                        .boardTitle(room.getBoardId().getTitle())
                                                        .boardWriterUsername(
                                                                        room.getBoardId().getAccount().getUsername())
                                                        .boardWriterProfileImageUrl(room.getBoardId().getAccount()
                                                                        .getProfileImageUrl())
                                                        .buyerUsername(room.getBuyerId().getUsername())
                                                        .buyerNickname(room.getBuyerId().getNickname())
                                                        .buyerProfileImageUrl(room.getBuyerId().getProfileImageUrl())
                                                        .orderStatus(latestOrder != null ? latestOrder.getOrderStatus()
                                                                        : "REQUEST")
                                                        .regDate(room.getRegDate())
                                                        .lastMessageDate(lastMsgDate)
                                                        .build();
                                })
                                .sorted((a, b) -> {
                                        LocalDateTime dateA = a.getLastMessageDate() != null ? a.getLastMessageDate()
                                                        : a.getRegDate();
                                        LocalDateTime dateB = b.getLastMessageDate() != null ? b.getLastMessageDate()
                                                        : b.getRegDate();
                                        return dateB.compareTo(dateA);
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

        private final PaymentRepository paymentRepository;
        private final PaymentService paymentService;

        // 환불 요청
        @Transactional
        public void requestRefund(Long orderId, String username, String reason) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

                boolean isBuyer = order.getBuyerId().getUsername().equals(username);
                boolean isWriter = order.getBoardId().getAccount().getUsername().equals(username);
                if (!isBuyer && !isWriter) {
                        throw new IllegalStateException("권한이 없습니다.");
                }

                List<String> allowed = List.of("PROCESSING", "SUBMITTED", "REVISION_REQUESTED");
                if (!allowed.contains(order.getOrderStatus())) {
                        throw new IllegalStateException("지금은 환불 요청이 불가능한 상태입니다.");
                }

                order.setPreRefundStatus(order.getOrderStatus());
                order.setRefundRequestedBy(username);
                order.setOrderStatus("REFUND_REQUESTED");

                Account sender = accountRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                OrderMessage msg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(sender)
                                .messageType("REFUND_REQUEST")
                                .content(reason)
                                .build();
                orderMessageRepository.save(msg);
        }

        // 환불 동의 (상대방만 가능)
        @Transactional
        public void agreeRefund(Long orderId, String username) throws Exception {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

                if (!"REFUND_REQUESTED".equals(order.getOrderStatus())) {
                        throw new IllegalStateException("환불 요청 상태가 아닙니다.");
                }
                if (username.equals(order.getRefundRequestedBy())) {
                        throw new IllegalStateException("본인이 요청한 환불에는 동의할 수 없습니다. 상대방의 동의가 필요합니다.");
                }

                Payment payment = paymentRepository.findByOrderId_OrderId(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));

                // 포트원 환불 API 호출
                paymentService.cancelPayment(payment.getPaymentUid(), "상호 합의에 의한 환불");

                payment.setPaymentStatus("REFUNDED");
                payment.setRefundedAmount(payment.getAmount());
                paymentRepository.save(payment);

                order.setOrderStatus("REFUNDED");
                order.setPreRefundStatus(null);
                order.setRefundRequestedBy(null);

                Account sender = accountRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                OrderMessage msg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(sender)
                                .messageType("REFUNDED")
                                .content("환불이 완료되었습니다. (" + payment.getAmount() + "원)")
                                .build();
                orderMessageRepository.save(msg);
        }

        // 환불 거절 (상대방만 가능, 원래 상태로 복원)
        @Transactional
        public void rejectRefund(Long orderId, String username) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

                if (!"REFUND_REQUESTED".equals(order.getOrderStatus())) {
                        throw new IllegalStateException("환불 요청 상태가 아닙니다.");
                }
                if (username.equals(order.getRefundRequestedBy())) {
                        throw new IllegalStateException("본인이 요청한 환불은 거절할 수 없습니다.");
                }

                order.setOrderStatus(order.getPreRefundStatus());
                order.setPreRefundStatus(null);
                order.setRefundRequestedBy(null);

                Account sender = accountRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                OrderMessage msg = OrderMessage.builder()
                                .orderId(order)
                                .senderId(sender)
                                .messageType("REFUND_REJECTED")
                                .content("환불 요청이 거절되었습니다.")
                                .build();
                orderMessageRepository.save(msg);
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
                                .chatRoomId(order.getChatRoom() != null ? order.getChatRoom().getChatRoomId() : null)
                                .buyerUsername(order.getBuyerId().getUsername()) // 구매자
                                .buyerNickname(order.getBuyerId().getNickname())
                                .buyerProfileImageUrl(order.getBuyerId().getProfileImageUrl())
                                .content(order.getContent())
                                .deadline(order.getDeadline())
                                .totalPrice(order.getTotalPrice())
                                .orderStatus(order.getOrderStatus())
                                .regDate(order.getRegDate())
                                .lastMessageDate(orderMessageRepository.findLastMessageDate(order.getOrderId()))
                                .refundRequestedBy(order.getRefundRequestedBy()) // 환불요청자
                                .build();
        }
}