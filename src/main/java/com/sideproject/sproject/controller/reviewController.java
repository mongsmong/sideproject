package com.sideproject.sproject.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.sideproject.sproject.dto.OrderDTO;
import com.sideproject.sproject.service.OrderService;
import com.sideproject.sproject.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final OrderService orderService;

    @GetMapping("/register/{orderId}")
    public String registerForm(@PathVariable Long orderId, Model model) {
        OrderDTO order = orderService.getOrderDetail(orderId);
        model.addAttribute("order", order);
        return "review/register";
    }

    @PostMapping("/register/{orderId}")
    public String register(@PathVariable Long orderId,
            @RequestParam Integer rating,
            @RequestParam String content,
            Principal principal) {
        reviewService.register(orderId, principal.getName(), rating, content);
        return "redirect:/order/info/" + orderId;
    }
}