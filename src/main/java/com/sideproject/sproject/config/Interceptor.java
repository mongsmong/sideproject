package com.sideproject.sproject.config;

import java.security.Principal;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.stereotype.Component;

import com.sideproject.sproject.repository.AccountRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Interceptor implements HandlerInterceptor { // cpmmon에 필요한 변수 전역
 
    private final AccountRepository accountRepository;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler, ModelAndView modelAndView) {
        if (modelAndView == null) return;

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            accountRepository.findByUsername(principal.getName()).ifPresent(account -> {
                modelAndView.addObject("profileImageUrl", account.getProfileImageUrl());
                modelAndView.addObject("nickname", account.getNickname());
                modelAndView.addObject("myUsername", account.getUsername());
            });
        }
    }
}