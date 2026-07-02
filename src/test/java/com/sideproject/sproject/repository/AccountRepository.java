package com.sideproject.sproject.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sideproject.sproject.entity.Account;
public interface AccountRepository extends JpaRepository<Account, Long> {
Optional<Account> findByUsername(String username); // 컬럼명을 바탕으로 연결 

}
