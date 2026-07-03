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
import org.springframework.data.domain.Sort;
import com.sideproject.sproject.dto.BoardDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.repository.AccountRepository;
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

    // 게시글 목록 페이지(데이터 + html)
    @GetMapping("/list")
public String listBoards(
        @RequestParam(required = false) String category, // 검색 파라미터 추가
        @RequestParam(required = false) String keyword,  // 검색 파라미터 추가
        @PageableDefault(size = 5, sort = "boardId", direction = Sort.Direction.DESC) Pageable pageable,
        Model model) {

    // 1. 검색 조건(category, keyword)과 페이징 정보(pageable)를 서비스로 같이 넘깁니다.
    Page<BoardDTO> boards = boardService.getBoards(category, keyword, pageable);
    model.addAttribute("boards", boards);

    // 2. 실시간 인기 해시태그 TOP 5를 가져와서 모델에 담아줍니다.
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
    // 1. 현재 로그인한 사용자의 ID(username) 가져오기
    String username = principal.getName();
    
    // 2. DB에서 실제 Account 객체 찾기
    Account writer = accountRepository.findByUsername(username)
                     .orElseThrow(() -> new IllegalArgumentException("로그인한 사용자를 찾을 수 없습니다."));

    // 3. 서비스의 등록 메서드 호출 (DTO와 writer를 함께 전달)
    boardService.saveBoard(dto, writer);

    // 4. 등록 후 목록 페이지로 이동
    return "redirect:/board/list";
}



    // 마이페이지 내 글 목록으로 이동
    @PostMapping("/toggleStatus/{boardId}")
    public String toggleStatus(@PathVariable Long boardId) {
        boardService.togglePostStatus(boardId);
        return "redirect:/mypage/myPosts";
    }

     // 게시글 상세 페이지
    @GetMapping("/detail/{boardId}")
    public String detail(@PathVariable Long boardId, Model model) {
        BoardDTO board = boardService.getBoardById(boardId);
        model.addAttribute("board", board);
        return "board/detail";
    }

    

    // 로그인 확인 
    
    

}