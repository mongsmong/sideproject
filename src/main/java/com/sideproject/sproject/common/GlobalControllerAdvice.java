package com.sideproject.sproject.common; // 본인 패키지에 맞게 수정

import com.sideproject.sproject.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final AccountRepository accountRepository;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Principal principal) {
        if (principal != null) {
            accountRepository.findByUsername(principal.getName())
                .ifPresent(account -> model.addAttribute("nickname", account.getNickname()));
        }
    }
}