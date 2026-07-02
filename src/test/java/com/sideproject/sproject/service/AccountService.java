package com.sideproject.sproject.service;

import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // 생성자 주입을 자동으로 해줍니다 (롬복 기능)
@Transactional(readOnly = true) // 데이터를 읽기 전용으로 설정
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Long register(Account account) {
        accountRepository.save(account);
        return account.getAccountId();
    }
}