package com.sideproject.sproject.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sideproject.sproject.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByBoardId_BoardIdAndBuyerId_AccountId(Long boardId, Long buyerId);

    List<ChatRoom> findByBuyerId_Username(String username);

    List<ChatRoom> findByBoardId_Account_Username(String username);

    
}