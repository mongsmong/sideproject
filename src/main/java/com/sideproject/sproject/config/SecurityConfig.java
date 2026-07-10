package com.sideproject.sproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration // 현재 클래스를 설정 컴퍼넌트로 위임
public class SecurityConfig {


    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // SecurityFilterChain : 클라이언트로부터 오는 모든 요청/응답을
        // Filter 영역에서 검사하는 클래스
        // 인증, 인가, CSRF 설정, 로그인/로그아웃 방식 설정
        // HttpSecurity : 보안 규칙을 설정하는 객체

        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 게시글 작성, 수정, 삭제는 일반 사용자만 가능하도록 설정
                        .requestMatchers("/", "/board/list", "/board/detail", "/auth/login", "/account/register", "/css/**", "/js/**")
                        .permitAll() // hasRole("USER") : ROLE 이름이 ROLE_USER인 사용자에 대한 접근 권한 부여

                        // 위 경로 외에 나머지 경로는 모든 권한 접근 가능
                        .anyRequest().authenticated())
                .formLogin(
                        form -> form.loginPage("/auth/login") // 커스텀 로그인 페이지 경로 연결(/templates/auth/login.html 페이지를 의미)
                                .loginProcessingUrl("/auth/login") // login POST 처리 URL(form action 경로와 일치)
                                .defaultSuccessUrl("/board/list", true) // 로그인 성공 시 이동 URL
                                .failureUrl("/auth/login?errorMsg=true") // 로그인 실패 시 이동 URL
                                .permitAll() // 로그인 페이지 URL을 모든 사용자 접근 허용
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout") // 로그아웃 POST 처리 URL
                        .logoutSuccessUrl("/auth/login?logoutMsg=true") // 로그아웃 성공 시 이동 URL
                        .permitAll() // 로그아웃 페이지 URL을 모든 사용자 접근 허용
                );

        return http.build();
    }

    // 비밀번호 암호화 객체 관리
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
