package com.sideproject.sproject.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardFileDTO {
    private Long fileId;
    private String originalName;
    private String filePath;
    private String contentType;
}