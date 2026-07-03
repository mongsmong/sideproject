package com.sideproject.sproject.service;

import static org.springframework.security.core.userdetails.User.builder;

import java.io.IOException;
import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sideproject.sproject.common.AccountRole;
import com.sideproject.sproject.dto.AccountDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder; // 반드시 final로 선언됨
    private final Cloudinary cloudinary;

    // 관리자 계정 생성 메소드
    public void creatAdmin() {
        // admin 계정이 존재할 경우 메소드 종료 조건문
        if (accountRepository.findByUsername("admin").isPresent()) {
            return;
        }

        Account admin = Account.builder()
                .username("admin")
                .password(passwordEncoder.encode("0622"))
                .email("admin@test.com")
                .nickname("관리자")
                .role(AccountRole.ROLE_ADMIN)
                .build();
        accountRepository.save(admin);
    }

    public int register(AccountDTO accountDTO) {
        // DTO를 엔티티로 변환 (엔티티 생성 로직을 이렇게 분리하면 깔끔합니다!)
        Account account = Account.builder()
                .username(accountDTO.getUsername())
                .password(passwordEncoder.encode(accountDTO.getPassword()))
                .email(accountDTO.getEmail())
                .nickname(accountDTO.getNickname())
                .role(AccountRole.ROLE_CLIENT)
                .build();

        accountRepository.save(account);

        return 1;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. DB에서 사용자 조회
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // 2. 시큐리티용 User 객체 생성 및 반환
        return builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .roles(account.getRole().name().replace("ROLE_", "")) // ROLE_ 접두사 제거
                .build();
    }

    // 프로필 정보 가져오기 
    public AccountDTO getMyInfo(String username) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            return AccountDTO.builder()
                            .username(account.getUsername())
                            .nickname(account.getNickname())
                            .email(account.getEmail())
                            .profileImageUrl(account.getProfileImageUrl())
                            .build();
    }

    // 프로필 업데이트
    @Transactional
    public void updateMyinfo(String username, String nickname, String email, MultipartFile profileImage) throws IOException{
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        account.setNickname(nickname);
        account.setEmail(email);

        if(profileImage != null && !profileImage.isEmpty()) {
            Map uploadResult = cloudinary.uploader().upload(
                profileImage.getBytes(),
                ObjectUtils.asMap("resource_type", "image")
            );

            String imageUrl = (String) uploadResult.get("secure_url");
            account.setProfileImageUrl(imageUrl);
        }
        accountRepository.save(account);
    }

}