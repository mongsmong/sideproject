package com.sideproject.sproject.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sideproject.sproject.entity.Order;

// save() 메소드는 스프링 데이터 JPA가 제공하는 기본 기능 
// 인터페이스가 이를 상속받아야 함
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId_Username(String username);

    List<Order> findByBoardId_Account_Username(String username);

    Optional<Order> findByBoardId_BoardIdAndBuyerId_AccountId(Long boardId, Long buyerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.boardId.account.accountId = :writerId " +
            "AND o.orderStatus IN ('PAYMENT_WAITING', 'PROCESSING', 'SUBMITTED', 'REVISION_REQUESTED')")
    long countActiveOrdersByWriter(@Param("writerId") Long writerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.boardId.account.accountId = :writerId " +
            "AND o.orderStatus = 'COMPLETED'")
    long countCompletedOrdersByWriter(@Param("writerId") Long writerId);

    // 글에 대한 작업수
    @Query("SELECT COUNT(o) FROM Order o WHERE o.boardId.boardId = :boardId AND o.orderStatus = 'COMPLETED'")
    long countCompletedOrdersByBoard(@Param("boardId") Long boardId);
}
