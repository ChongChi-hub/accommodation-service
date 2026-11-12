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

    private static final int MAX_BOOKMARKS_PER_USER = 200;

    private final BookmarkRepository bookmarkRepository;
    private final RoomRepository roomRepository;
    private final RoomQueryService roomQueryService;

    @Transactional
    public BookmarkStatusDto addBookmark(String userId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng không tồn tại: " + roomId));

        if (!bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)) {
            if (bookmarkRepository.countByUserId(userId) >= MAX_BOOKMARKS_PER_USER) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Bạn chỉ có thể lưu tối đa " + MAX_BOOKMARKS_PER_USER + " phòng trọ"
                );
            }
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

        List<Room> activeRooms = roomRepository.findAllByIdIn(roomIds).stream()
                .filter(r -> !r.isDeleted() && !r.isHidden()
                        && (r.isPublished() || "AVAILABLE".equalsIgnoreCase(r.getStatus())))
                .toList();

        Map<Long, RoomDto> roomMap = roomQueryService.toRoomDtos(activeRooms).stream()
                .peek(dto -> dto.setIsBookmarked(true))
                .collect(Collectors.toMap(RoomDto::getId, Function.identity(), (a, b) -> a));

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
