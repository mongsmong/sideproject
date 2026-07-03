package com.sideproject.sproject.dto;

import com.sideproject.sproject.common.AccountRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    private String username;
    private String password;
    private String nickname;
    private String profileImageUrl;
    private String email;
    private AccountRole role;

}
