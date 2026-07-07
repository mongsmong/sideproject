package com.sideproject.sproject.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMessageDTO {
    private Long messageId;
    private Long orderId;
    private String senderUsername;
    private String senderNickname;
    private String senderProfileImageUrl;
    private String content;
    private String messageType;
    private LocalDateTime regDate;
    private LocalDate requestDeadline;
    private Integer requestPrice;
    private List<OrderFileDTO> files;
    private String orderTitle;
    private Integer totalPrice;

}
