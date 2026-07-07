package com.sideproject.sproject.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;
import com.sideproject.sproject.dto.BoardDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.OrderRepository;
import com.sideproject.sproject.service.BoardService;
import java.security.Principal;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    // 게시글 목록 페이지(데이터 + html)
    @GetMapping("/list")
    public String listBoards(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String hashtag,
            @PageableDefault(size = 5, sort = "boardId", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        // category, keyword,pageable 서비스로
        Page<BoardDTO> boards = boardService.getBoards(category, keyword, hashtag, pageable);
        model.addAttribute("boards", boards);
        model.addAttribute("selectedHashtag", hashtag);

        // 실시간 인기 해시태그
        List<String> popularTags = boardService.getPopularHashtags();
        model.addAttribute("popularTags", popularTags);

        return "board/list";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "board/register"; // html 파일 경로
    }

    // 게시글 등록 처리
    @PostMapping("/register")
    public String register(BoardDTO dto, Principal principal) {
        String username = principal.getName();

        Account writer = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("로그인한 사용자를 찾을 수 없습니다."));

        boardService.saveBoard(dto, writer);
        return "redirect:/board/list";
    }

    // 게시글 상세 페이지
    @GetMapping("/detail/{boardId}")
    public String detail(@PathVariable Long boardId, Model model) {
        BoardDTO board = boardService.getBoardById(boardId);
        long completedCount = orderRepository.countCompletedOrdersByBoard(boardId);

        model.addAttribute("board", board);
        model.addAttribute("completedCount", completedCount);

        return "board/detail";
    }

    // 게시글 수정
    @GetMapping("/edit/{boardId}")
    public String editForm(@PathVariable Long boardId, Principal principal, Model model) {
        BoardDTO board = boardService.getBoardById(boardId);

        if (!board.getWriterUsername().equals(principal.getName())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        model.addAttribute("board", board);
        return "board/edit";
    }

    @PostMapping("/edit/{boardId}")
    public String edit(@PathVariable Long boardId, BoardDTO dto, Principal principal) {
        boardService.updateBoard(boardId, dto, principal.getName());
        return "redirect:/account/profile/" + principal.getName();
    }

    // 신청 접수 on/off
    @PostMapping("/toggle/{boardId}")
    public String toggleStatus(@PathVariable Long boardId, Principal principal,
            RedirectAttributes redirectAttributes) {
        boardService.togglePostStatus(boardId, principal.getName());
        redirectAttributes.addFlashAttribute("msg", "게시글 상태가 변경되었습니다.");
        return "redirect:/account/profile/" + principal.getName();

    }
}