package com.sideproject.sproject.controller;

import com.sideproject.sproject.service.OrderMessageService;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sideproject.sproject.common.AccountRole;
import com.sideproject.sproject.dto.BoardDTO;
import com.sideproject.sproject.dto.ChatRoomDTO;
import com.sideproject.sproject.dto.OrderDTO;
import com.sideproject.sproject.dto.OrderMessageDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Board;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.BoardRepository;
import com.sideproject.sproject.service.BoardService;
import com.sideproject.sproject.service.OrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final BoardRepository boardRepository;
    private final AccountRepository accountRepository;
    private final BoardService boardService;
    private final OrderMessageService orderMessageService;

    // 주문 작성
    @GetMapping("/create/{boardId}")
    public String OrderForm(@PathVariable Long boardId, Model model) {
        BoardDTO boardDTO = boardService.getBoardById(boardId);
        model.addAttribute("boardId", boardId);
        model.addAttribute("board", boardDTO);
        return "/order/create";
    }

    // 주문 만들기 (중복 방지 & 소통창 바로 가기)
    @PostMapping("/create/{boardId}")
    public String createOrder(@PathVariable Long boardId, OrderDTO dto, Principal principal,
            RedirectAttributes redirectAttributes) {
        // 로그인 사용자 정보 체크
        if (principal == null) {
            return "redirect:/auth/login";
        }
        // System.out.println("프론트에서 넘어온 DTO: " + dto.toString());

        Board board = boardRepository.findById(dto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        Account buyer = accountRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        if (board.getAccount().getUsername().equals(principal.getName())) {
            throw new IllegalStateException("본인의 게시글에는 신청할 수 없습니다.");
        }
        if (buyer.getRole() != AccountRole.ROLE_CLIENT) {
            throw new IllegalStateException("신청 권한이 없습니다.");
        }

        // 4. 저장 및 중복 체크

        try {
            Long chatRoomId = orderService.saveOrder(dto, board, buyer);
            return "redirect:/order/detail/" + chatRoomId;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/board/detail/" + boardId;
        }

    }

    // 주문 캔슬
    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, Principal principal) {
        orderService.cancelOrder(orderId, principal.getName());
        return "redirect:/order/info/" + orderId;
    }

    // 주문 내역 목록 화면
    @GetMapping("/list")
    public String orderList(Principal principal, Model model) {

        // 시큐리티에서 현재 사용자username 가져오기
        String username = principal.getName();
        // DTO 리스트
        List<OrderDTO> orderList = orderService.getOrderList(username);
        // model로 HTML로 전달
        model.addAttribute("orderList", orderList);
        model.addAttribute("currentUsername", username);

        return "/order/list";
    }


    // 작업내역목록화면
    @GetMapping("/worklist")
    public String workList(@RequestParam(required = false, defaultValue = "all") String role,
            Principal principal, Model model) {
        List<OrderDTO> workList = orderService.getWorkList(principal.getName(), role);
        model.addAttribute("workList", workList);
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("roleFilter", role);
        return "order/worklist";
    }

    @GetMapping("/worklist")
    public String workList(Principal principal, Model model) {
        List<OrderDTO> workList = orderService.getWorkList(principal.getName());
        model.addAttribute("workList", workList);
        model.addAttribute("currentUsername", principal.getName());
        return "order/worklist";
    }

    // 의뢰창
    @GetMapping("/detail/{chatRoomId}")
    public String orderDetail(@PathVariable Long chatRoomId, Principal principal, Model model) {
        ChatRoomDTO chatRoom = orderService.getChatRoomDetail(chatRoomId);
        List<OrderMessageDTO> messages = orderMessageService.getMessagesByChatRoom(chatRoomId);

        String currentUsername = principal.getName();
        boolean isBuyer = chatRoom.getBuyerUsername().equals(currentUsername);
        boolean isWriter = chatRoom.getBoardWriterUsername().equals(currentUsername);

        model.addAttribute("room", chatRoom);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUsername", currentUsername);
        model.addAttribute("isBuyer", isBuyer);
        model.addAttribute("isWriter", isWriter);

        return "order/detail";
    }

    // 메시지 전송 (OrderMessageController에서 여기로 이동)
    @PostMapping("/detail/{chatRoomId}/send")
    public String sendMessage(@PathVariable Long chatRoomId,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> attachments,
            Principal principal) throws IOException {
        orderMessageService.sendMessage(chatRoomId, principal.getName(), content, attachments);
        return "redirect:/order/detail/" + chatRoomId;
    }

    // 상세 페이지
    @GetMapping("/info/{orderId}")
    public String orderInfo(@PathVariable Long orderId, Model model, Principal principal) {
        OrderDTO orderDTO = orderService.getOrderDetail(orderId);
        List<OrderMessageDTO> messages = orderMessageService.getMessages(orderId);

        String currentUsername = principal.getName();
        boolean isBuyer = currentUsername.equals(orderDTO.getBuyerUsername());
        boolean isWriter = currentUsername.equals(orderDTO.getBoardWriterUsername());

        model.addAttribute("order", orderDTO);
        model.addAttribute("messages", messages);
        model.addAttribute("isBuyer", isBuyer);
        model.addAttribute("isWriter", isWriter);
        model.addAttribute("currentUsername", currentUsername);

        return "/order/info";
    }

    // 작업물 컨펌
    @PostMapping("/confirm/{orderId}")
    public String confirmRequest(@PathVariable Long orderId,
            @RequestParam Integer totalPrice,
            @RequestParam(required = false) String orderTitle,
            Principal principal) {
        orderService.confirmRequest(orderId, totalPrice, orderTitle, principal.getName());
        return "redirect:/order/info/" + orderId;
    }

    // 작업물 제출
    @PostMapping("/submit/{orderId}")
    public String submitWork(@PathVariable Long orderId,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> files,
            Principal principal) throws IOException {
        orderService.submitWork(orderId, principal.getName(), content, files);
        return "redirect:/order/info/" + orderId;
    }

    // 수정 요청
    @PostMapping("/revision/{orderId}")
    public String requestRevision(@PathVariable Long orderId,
            @RequestParam String content,
            Principal principal) {
        orderService.requestRevision(orderId, principal.getName(), content);
        return "redirect:/order/info/" + orderId;
    }

    @GetMapping("/info/{orderId}/request/{messageId}")
    public String orderRequestInfo(@PathVariable Long orderId, @PathVariable Long messageId,
            Model model, Principal principal) {
        OrderDTO orderDTO = orderService.getOrderDetail(orderId);
        OrderMessageDTO request = orderMessageService.getMessage(messageId);

        String currentUsername = principal.getName();
        model.addAttribute("order", orderDTO);
        model.addAttribute("request", request);
        model.addAttribute("isBuyer", currentUsername.equals(orderDTO.getBuyerUsername()));
        model.addAttribute("isWriter", currentUsername.equals(orderDTO.getBoardWriterUsername()));

        return "/order/info-request";
    }

    @PostMapping("/approve/{orderId}")
    public String approveWork(@PathVariable Long orderId, Principal principal,
                                RedirectAttributes redirectAttributes) {
        orderService.approveWork(orderId, principal.getName());
        redirectAttributes.addFlashAttribute("showReviewPrompt", true);
        return "redirect:/order/info/" + orderId;
    }

    // 환불용 컨트롤러 [요청:동의:반려]
    @PostMapping("/refund/request/{orderId}")
    public String requestRefund(@PathVariable Long orderId,
            @RequestParam String reason,
            Principal principal) {
        orderService.requestRefund(orderId, principal.getName(), reason);
        return "redirect:/order/info/" + orderId;
    }

    @PostMapping("/refund/agree/{orderId}")
    public String agreeRefund(@PathVariable Long orderId, Principal principal) throws Exception {
        orderService.agreeRefund(orderId, principal.getName());
        return "redirect:/order/info/" + orderId;
    }

    @PostMapping("/refund/reject/{orderId}")
    public String rejectRefund(@PathVariable Long orderId, Principal principal) {
        orderService.rejectRefund(orderId, principal.getName());
        return "redirect:/order/info/" + orderId;
    }
}
