package com.sideproject.sproject.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sideproject.sproject.entity.BoardFile;

public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {
    List<BoardFile> findByBoard_BoardIdOrderByFileIdAsc(Long boardId);
}