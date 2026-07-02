package com.sideproject.sproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePost {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId; 

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    private LocalDateTime regDate;

    @ManyToOne(fetch = FetchType.LAZY) // ★유저와의 관계 설정
    @JoinColumn(name = "account_id")
    private Account account;
}