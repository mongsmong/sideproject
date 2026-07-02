package com.sideproject.sproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}