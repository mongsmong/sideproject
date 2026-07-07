package com.sideproject.sproject.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sideproject.sproject.dto.AccountDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Board;
import com.sideproject.sproject.repository.AccountRepository;
import com.sideproject.sproject.repository.BoardRepository;
import com.sideproject.sproject.repository.OrderRepository;
import com.sideproject.sproject.service.AccountService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final BoardRepository boardRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/register")
    public String registerForm() {
        return "account/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute AccountDTO accountDTO, RedirectAttributes redirectAttributes) {
        accountService.register(accountDTO);
        redirectAttributes.addFlashAttribute("msg", "회원가입이 완료되었습니다!");
        return "redirect:/auth/login";
    }


    @GetMapping("/mypage")
    public String myPageRedirect(Principal principal) {
        return "redirect:/account/profile/" + principal.getName();
    }

    @GetMapping("/mypage/info")
    public String myInfoForm(Principal principal, Model model) {
        AccountDTO accountDTO = accountService.getMyInfo(principal.getName());
        model.addAttribute("account", accountDTO);
        return "account/myinfo";
    }

    @PostMapping("/mypage/info")
    public String updateMyInfo(Principal principal,
            @RequestParam String nickname,
            @RequestParam String email,
            @RequestParam(required = false) MultipartFile profileImage,
            RedirectAttributes redirectAttributes) throws IOException {
        accountService.updateMyinfo(principal.getName(), nickname, email, profileImage);
        redirectAttributes.addFlashAttribute("msg", "내 정보가 수정되었습니다.");
        return "redirect:/account/mypage/info";

    }

    @GetMapping("/profile/{username}")
    public String profile(@PathVariable String username, Principal principal, Model model) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        boolean isOwner = principal != null && principal.getName().equals(username);

        List<Board> boards = boardRepository.findByAccount_Username(username);

        long activeCount = orderRepository.countActiveOrdersByWriter(account.getAccountId());
        long completedCount = orderRepository.countCompletedOrdersByWriter(account.getAccountId());

        model.addAttribute("account", account);
        model.addAttribute("boards", boards);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);

        return "account/profile";
    }

    @PostMapping("/profile/slots")
    public String updateSlots(Principal principal,
            @RequestParam(required = false) Integer maxSlots,
            @RequestParam(required = false, defaultValue = "false") boolean allowOverbooking,
            RedirectAttributes redirectAttributes) {
        accountService.updateSlotSettings(principal.getName(), maxSlots, allowOverbooking);
        redirectAttributes.addFlashAttribute("msg", "슬롯 설정이 저장되었습니다.");
        return "redirect:/account/profile/" + principal.getName();
    }
}