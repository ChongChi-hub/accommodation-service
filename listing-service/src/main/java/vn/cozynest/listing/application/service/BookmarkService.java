package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.cozynest.listing.application.dto.BookmarkStatusDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.domain.model.Bookmark;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.BookmarkRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final RoomRepository roomRepository;
    private final RoomQueryService roomQueryService;

    @Transactional
    public BookmarkStatusDto addBookmark(String userId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng không tồn tại: " + roomId));

        if (!bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)) {
            Bookmark bookmark = Bookmark.builder()
                    .userId(userId)
                    .roomId(roomId)
                    .createdAt(LocalDateTime.now())
                    .build();
            bookmarkRepository.save(bookmark);
            roomRepository.incrementBookmarkCount(roomId);
            log.info("User {} bookmarked room {}", userId, roomId);
        }

        Room updated = roomRepository.findById(roomId).orElse(room);
        int count = updated.getBookmarkCount() != null ? updated.getBookmarkCount() : 0;
        return BookmarkStatusDto.builder()
                .roomId(roomId)
                .isBookmarked(true)
                .bookmarkCount(count)
                .build();
    }

    @Transactional
    public BookmarkStatusDto removeBookmark(String userId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng không tồn tại: " + roomId));

        if (bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)) {
            bookmarkRepository.deleteByUserIdAndRoomId(userId, roomId);
            roomRepository.decrementBookmarkCount(roomId);
            log.info("User {} removed bookmark on room {}", userId, roomId);
        }

        Room updated = roomRepository.findById(roomId).orElse(room);
        int count = updated.getBookmarkCount() != null ? updated.getBookmarkCount() : 0;
        return BookmarkStatusDto.builder()
                .roomId(roomId)
                .isBookmarked(false)
                .bookmarkCount(count)
                .build();
    }

    @Transactional(readOnly = true)
    public BookmarkStatusDto getStatus(String userId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng không tồn tại: " + roomId));

        boolean isBookmarked = (userId != null && !userId.isBlank())
                && bookmarkRepository.existsByUserIdAndRoomId(userId, roomId);
        int count = room.getBookmarkCount() != null ? room.getBookmarkCount() : 0;

        return BookmarkStatusDto.builder()
                .roomId(roomId)
                .isBookmarked(isBookmarked)
                .bookmarkCount(count)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getMyBookmarks(String userId) {
        List<Long> roomIds = bookmarkRepository.findRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, RoomDto> roomMap = new HashMap<>();
        for (Long id : roomIds) {
            try {
                RoomDto dto = roomQueryService.getRoomById(id);
                if (dto != null) {
                    dto.setIsBookmarked(true);
                    roomMap.put(id, dto);
                }
            } catch (Exception ignored) {
            }
        }

        List<RoomDto> result = new ArrayList<>();
        for (Long id : roomIds) {
            RoomDto dto = roomMap.get(id);
            if (dto != null) {
                result.add(dto);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Long> getMyBookmarkedRoomIds(String userId) {
        return bookmarkRepository.findRoomIdsByUserId(userId);
    }
}
