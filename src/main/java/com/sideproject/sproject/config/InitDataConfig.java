package com.sideproject.sproject.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sideproject.sproject.service.AccountService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class InitDataConfig { 
    
    private final AccountService accountService;

    // Spring Boot가 실행될 때 딱 한번 실행되는 초기 데이터 생성 코드
    @Bean
    public CommandLineRunner init() { 
        return agrs -> {
            accountService.creatAdmin();
        };
    }        
}
