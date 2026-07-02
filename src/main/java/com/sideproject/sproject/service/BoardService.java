package com.sideproject.sproject.service;

import com.sideproject.sproject.dto.BoardDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Board;
import com.sideproject.sproject.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest; 
import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    // 게시글 등록
    @Transactional
    public int saveBoard(BoardDTO boardDTO, Account writer) { // Account를 파라미터로 받음
        String type = "OFFER";
        if("구해요".equals(boardDTO.getCategory())){
            type = "REQUEST";
        }



        Board board = Board.builder()
                .title(boardDTO.getTitle())
                .content(boardDTO.getContent())
                .category(boardDTO.getCategory())
                .hashtag(boardDTO.getHashtag()) 
                .postType(type)
                .basePrice(boardDTO.getBasePrice())
                .account(writer) // 여기서 Account 객체를 넘겨주면 JPA가 writer_id 컬럼에 알아서 ID를 넣음
                .postStatus("ON") // 빌더에 기본값 외에 명시할 것이 있다면 여기서 처리
                .build();
        // 게시글 데이터 JPA를 이용하여 DB에 저장
        // board에 attachs 엔티티가 적용되어 있으므로
        // board 테이블 insert와 attach 테이블의 insert가
        // 한 번에 실행됨(JPA 연결 특성으로 인해)
        
        boardRepository.save(board);
        return 1;
    }

    // 전체 게시글 조회(페이징 처리)
    @Transactional(readOnly = true)
public Page<BoardDTO> getBoards(String category, String keyword, Pageable pageable) {
    Page<Board> boards;

    // 1. 카테고리와 검색어가 둘 다 있을 때
    if (category != null && !category.isEmpty() && keyword != null && !keyword.isEmpty()) {
        boards = boardRepository.searchByCategoryAndKeyword(category, keyword, pageable);
    }
    // 2. 카테고리만 있을 때
    else if (category != null && !category.isEmpty()) {
        boards = boardRepository.findByCategory(category, pageable);
    }
    // 3. 검색어만 있을 때
    else if (keyword != null && !keyword.isEmpty()) {
        boards = boardRepository.searchByKeyword(keyword, pageable);
    }
    // 4. 둘 다 없을 때 (전체 조회)
    else {
        boards = boardRepository.findAll(pageable);
    }

    // 작성해두신 toDTO 메서드를 활용하여 Page를 변환합니다.
    return boards.map(this::toDTO);
}

    // 게시글 상세 조회
    public BoardDTO getBoardById(Long boardId) {
        // Optional<T> findById(ID id) :
        // - T : JpaRepository<T, id>에서 T에 설정된 엔티티(Board)
        // - id : JpaRepository<T, id>에서 id에 설정된 PK(Long boardId)
        // Optional<Board> : 게시글이 존재하면 Optional안에 Board 객체가 존재
        // 존재하지 않으면 Optional.empty()
        // orElseThrow() : Optional안에 Board 객체가 없으면
        // throw new Exception - exception을 실행
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        return toDTO(board);
    }

    // 게시글 상태 토글 (ON <-> OFF)
    public void togglePostStatus(Long boardId) {
        Board post = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        String newStatus = post.getPostStatus().equals("ON") ? "OFF" : "ON";
        post.setPostStatus(newStatus);
    }




public List<String> getPopularHashtags() {

    return boardRepository.findPopularHashtags(PageRequest.of(0, 5));
}

    // Board -> BoardDTO 변경 메소드
    private BoardDTO toDTO(Board board) {
        Long writerId = null;
        String writerNickname = "알수없음";
        String writerUsername = "알수없음";

        // board.getAccount()가 null이 아닌 경우에만 안전하게 정보 추출
        if (board.getAccount() != null) {
            // 테이블 설계상 PK가 accountid이므로 getAccountId() 호출
            writerId = board.getAccount().getAccountId();
            writerNickname = board.getAccount().getNickname();
            writerUsername = board.getAccount().getUsername();
        }

        return BoardDTO.builder()
                .boardId(board.getBoardId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .postType(board.getPostType())
                .basePrice(board.getBasePrice())
                .hashtag(board.getHashtag())
                .postStatus(board.getPostStatus())
                .regDate(board.getRegDate())
                .writerId(board.getAccount().getAccountId())
                .writerNickname(board.getAccount().getNickname())
                .writerUsername(board.getAccount().getUsername()) 
                .build();
    }
}