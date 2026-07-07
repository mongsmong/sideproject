package com.sideproject.sproject.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long chatRoomId;
    private Long boardId;
    private String boardTitle;
    private String boardWriterUsername;
    private String boardWriterProfileImageUrl;
    private String buyerUsername;
    private String buyerNickname;
    private String buyerProfileImageUrl;
}