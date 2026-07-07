package com.sideproject.sproject.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sideproject.sproject.entity.OrderFile;

public interface OrderFileRepository extends JpaRepository<OrderFile, Long> {
    List<OrderFile> findByMessageId_MessageId(Long messageId);

   

}
