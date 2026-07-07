package com.sideproject.sproject.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardDTO {
    
    // 게시글 기본 정보
    private Long boardId;
    private String title;
    private String content;
    private String category;
    private int basePrice;
    private String postStatus; 
    private String hashtag;
    private LocalDateTime regDate; // 리스트/상세 페이지에 필요
    private List<String> questionTitles;
    private String postType;
 

    // 작성자 관련 정보 (화면 표시 및 로직용)
    private Long writerId;         // DB 식별자 (FK)
    private String writerNickname; // 화면 표시용 (닉네임)
    private String writerUsername; // 본인 확인 및 로직 처리용 (로그인 ID)
    private String writerProfileImageUrl;
}