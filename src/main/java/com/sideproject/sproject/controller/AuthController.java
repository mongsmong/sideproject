package com.sideproject.sproject.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {
    
    //로그인 폼
    @GetMapping("/login")
    public void loginForm(String errorMsg, String logoutMsg
        , Model model
    ) { 
        if(errorMsg != null) {
            model.addAttribute("errorMsg", "아이디 비밀번호 확인");
        }
        if(logoutMsg != null) {
            model.addAttribute("logoutMsg", "로그아웃되었습니다.");
        }
    }
}

