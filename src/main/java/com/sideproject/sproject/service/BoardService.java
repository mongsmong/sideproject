package com.sideproject.sproject.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sideproject.sproject.dto.BoardDTO;
import com.sideproject.sproject.dto.BoardFileDTO;
import com.sideproject.sproject.dto.OrderDTO;
import com.sideproject.sproject.entity.Account;
import com.sideproject.sproject.entity.Board;
import com.sideproject.sproject.entity.BoardFile;
import com.sideproject.sproject.entity.ChatRoom;
import com.sideproject.sproject.entity.Order;
import com.sideproject.sproject.entity.OrderMessage;
import com.sideproject.sproject.repository.BoardFileRepository;
import com.sideproject.sproject.repository.BoardRepository;
import com.sideproject.sproject.repository.ChatRoomRepository;

import com.sideproject.sproject.repository.OrderMessageRepository;
import com.sideproject.sproject.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final OrderMessageRepository orderMessageRepository;
    private final OrderRepository orderRepository;
    private final BoardRepository boardRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final BoardFileRepository boardFileRepository;
    private final Cloudinary cloudinary;

    // 게시글 작성

    @Transactional
    public int saveBoard(BoardDTO boardDTO, Account writer, List<MultipartFile> files) throws IOException {
        String hashtag = boardDTO.getHashtag() != null ? boardDTO.getHashtag().trim() : null;
        String type = "구해요".equals(boardDTO.getCategory()) ? "REQUEST" : "OFFER";

        Board board = Board.builder()
                .title(boardDTO.getTitle())
                .content(boardDTO.getContent())
                .category(boardDTO.getCategory())
                .hashtag(hashtag)
                .postType(type)
                .basePrice(boardDTO.getBasePrice())
                .account(writer)
                .postStatus("ON")
                .build();

        boardRepository.save(board);
        uploadBoardFiles(board, files);

        return 1;
    }

    private void uploadBoardFiles(Board board, List<MultipartFile> files) throws IOException {
        if (files == null)
            return;

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto"));

            String fileUrl = (String) uploadResult.get("secure_url");
            String storedName = (String) uploadResult.get("public_id");

            BoardFile boardFile = BoardFile.builder()
                    .board(board)
                    .originalName(file.getOriginalFilename())
                    .storedName(storedName)
                    .filePath(fileUrl)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();
            boardFileRepository.save(boardFile);
        }
    }

    // 게시글 수정
    @Transactional
    public void updateBoard(Long boardId, BoardDTO dto, String username, List<MultipartFile> newFiles)
            throws IOException {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!board.getAccount().getUsername().equals(username)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String hashtag = dto.getHashtag() != null ? dto.getHashtag().trim() : null;
        String type = "구해요".equals(dto.getCategory()) ? "REQUEST" : "OFFER";

        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        board.setCategory(dto.getCategory());
        board.setHashtag(hashtag);
        board.setPostType(type);
        board.setBasePrice(dto.getBasePrice());

        boardRepository.save(board);
        uploadBoardFiles(board, newFiles); // 수정 시에도 추가 첨부 가능 (기존 파일 유지)
    }

    // 전체 게시글 조회(페이징 처리)
    @Transactional(readOnly = true)
    public Page<BoardDTO> getBoards(String category, String keyword, String hashtag, Pageable pageable) {
        boolean hasCategory = category != null && !category.isEmpty();
        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        boolean hasHashtag = hashtag != null && !hashtag.isEmpty();

        Page<Board> boards;

        if (hasCategory && hasKeyword && hasHashtag) {
            boards = boardRepository.searchByCategoryAndKeywordAndHashtag(category, keyword, hashtag, pageable);
        } else if (hasCategory && hasHashtag) {
            boards = boardRepository.findByCategoryAndHashtagContaining(category, hashtag, pageable);
        } else if (hasKeyword && hasHashtag) {
            boards = boardRepository.searchByKeywordAndHashtag(keyword, hashtag, pageable);
        } else if (hasHashtag) {
            boards = boardRepository.findByHashtagContaining(hashtag, pageable);
        } else if (hasCategory && hasKeyword) {
            boards = boardRepository.searchByCategoryAndKeyword(category, keyword, pageable);
        } else if (hasCategory) {
            boards = boardRepository.findByCategory(category, pageable);
        } else if (hasKeyword) {
            boards = boardRepository.searchByKeyword(keyword, pageable);
        } else {
            boards = boardRepository.findAll(pageable);
        }

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
    @Transactional
    public void togglePostStatus(Long boardId, String username) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!board.getAccount().getUsername().equals(username)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        board.setPostStatus("ON".equals(board.getPostStatus()) ? "OFF" : "ON");
        boardRepository.save(board);
    }

    // 인기 해시태그 조회
    public List<String> getPopularHashtags() {
        List<String> allHashtagStrings = boardRepository.findAllHashtagStrings();

        Map<String, Long> tagCounts = allHashtagStrings.stream()
                .flatMap(s -> Arrays.stream(s.trim().split("\\s+")))
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        return tagCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private BoardDTO toDTO(Board board) {
        Long writerId = null;
        String writerNickname = "알수없음";
        String writerUsername = "알수없음";
        String writerProfileImageUrl = null;

        if (board.getAccount() != null) {
            writerId = board.getAccount().getAccountId();
            writerNickname = board.getAccount().getNickname();
            writerUsername = board.getAccount().getUsername();
            writerProfileImageUrl = board.getAccount().getProfileImageUrl();
        }

        List<BoardFile> boardFiles = boardFileRepository.findByBoard_BoardIdOrderByFileIdAsc(board.getBoardId());

        List<BoardFileDTO> fileDTOs = boardFiles.stream()
                .map(f -> BoardFileDTO.builder()
                        .fileId(f.getFileId())
                        .originalName(f.getOriginalName())
                        .filePath(f.getFilePath())
                        .contentType(f.getContentType())
                        .build())
                .collect(Collectors.toList());

        String thumbnail = boardFiles.stream()
                .filter(f -> f.getContentType() != null && f.getContentType().startsWith("image"))
                .map(BoardFile::getFilePath)
                .findFirst()
                .orElse(null);

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
                .writerId(writerId)
                .writerNickname(writerNickname)
                .writerUsername(writerUsername)
                .writerProfileImageUrl(writerProfileImageUrl)
                .files(fileDTOs)
                .thumbnailUrl(thumbnail)
                .build();
    }
}