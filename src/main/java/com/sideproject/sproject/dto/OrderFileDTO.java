package com.sideproject.sproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFileDTO {
    private Long fileId;
    private String originalName;
    private String filePath;
    private String contentType;

}