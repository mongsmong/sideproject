package com.sideproject.sproject.controller;

import java.io.IOException;
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sideproject.sproject.dto.AccountDTO;
import com.sideproject.sproject.service.AccountService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

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

}