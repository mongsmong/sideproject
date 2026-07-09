package com.sideproject.sproject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderFile;

public interface OrderFileRepository extends JpaRepository<OrderFile, Long> {
    List<OrderFile> findByMessageId_MessageId(Long messageId);


   

}
