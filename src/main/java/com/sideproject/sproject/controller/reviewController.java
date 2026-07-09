package com.sideproject.sproject.controller;

import com.sideproject.sproject.service.ReviewService;
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sideproject.sproject.dto.OrderDTO;
import com.sideproject.sproject.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class reviewController {
    private final ReviewService reviewService;
    private final OrderService orderService;





    // orderId 가져오기
    @GetMapping("/register/{orderId}")
    public String registerForm(@PathVariable Long orderId, Model model){
        OrderDTO order = orderService.getOrderDetail(orderId);
        model.addAttribute("order", order);
        return "review/register";
    }

    // 리뷰 작성
    @PostMapping("/register/orderId")       
    public String register(@PathVariable Long orderId,
                            @RequestParam Integer rating,
                            @RequestParam String content,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            reviewService.register(orderId, principal.getName(), rating, content);
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());    
        }
        return "redirect:/order/info" + orderId;
    }
    

}
