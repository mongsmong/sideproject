package com.sideproject.sproject.entity; 


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

import com.sideproject.sproject.common.AccountRole;

@Entity
@Getter @Setter
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
    
    @Column(nullable = false)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountRole role;
    
    @CreationTimestamp
    private LocalDateTime regDate;
 
    

}