package com.sideproject.sproject.entity; 

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity // 이 클래스가 JPA 엔티티임을 정의
// Board 클래스의 구조에 맞게 DB 테이블이 생성됨
// @Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long accountId;
    @Column(nullable = false, unique = true) // PK는 아님
    private String username; // 계정 아이디 
    @Column(nullable = false)
    private String password;
    @Column(nullable = false, unique = true)
    private String nickname;
    @Column(nullable = false, unique = true)
    private String email;
    @CreationTimestamp
    private LocalDateTime regDate;

   
}