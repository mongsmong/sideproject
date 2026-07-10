package com.sideproject.sproject.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
    public String handleBusinessException(Exception e, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());

        String uri = request.getRequestURI();

        // 신청서 관련 요청이면 무조건 게시글 상세로 (폼으로 되돌아가지 않게)
        if (uri.startsWith("/order/create")) {
            String boardId = uri.substring(uri.lastIndexOf("/") + 1);
            return "redirect:/board/detail/" + boardId;
        }

        // 리뷰 등록 관련 요청이면 무조건 주문 정보 화면으로 (폼으로 되돌아가지 않게)
        if (uri.startsWith("/review/register")) {
            String orderId = uri.substring(uri.lastIndexOf("/") + 1);
            return "redirect:/order/info/" + orderId;
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/board/list");
    }
}