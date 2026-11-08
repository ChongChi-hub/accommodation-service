package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import vn.cozynest.listing.application.dto.BookmarkStatusDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.service.BookmarkService;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.domain.model.Bookmark;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.BookmarkRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomQueryService roomQueryService;

    @InjectMocks
    private BookmarkService bookmarkService;

    @Test
    @DisplayName("1. Thêm bookmark thành công và tăng bookmarkCount")
    void shouldAddBookmarkSuccessfully() {
        String userId = "u-tenant-01";
        Long roomId = 101L;

        Room room = Room.builder().id(roomId).title("Phòng studio").bookmarkCount(0).price(new BigDecimal("3000000")).build();
        Room updated = Room.builder().id(roomId).title("Phòng studio").bookmarkCount(1).price(new BigDecimal("3000000")).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room)).thenReturn(Optional.of(updated));
        when(bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)).thenReturn(false);

        BookmarkStatusDto status = bookmarkService.addBookmark(userId, roomId);

        assertThat(status.isBookmarked()).isTrue();
        assertThat(status.getBookmarkCount()).isEqualTo(1);
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
        verify(roomRepository, times(1)).incrementBookmarkCount(roomId);
    }

    @Test
    @DisplayName("2. Thêm bookmark idempotent: gọi lần 2 không tăng thêm và không throw lỗi")
    void shouldBeIdempotentWhenAddingBookmarkAgain() {
        String userId = "u-tenant-01";
        Long roomId = 101L;

        Room room = Room.builder().id(roomId).bookmarkCount(1).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)).thenReturn(true);

        BookmarkStatusDto status = bookmarkService.addBookmark(userId, roomId);

        assertThat(status.isBookmarked()).isTrue();
        assertThat(status.getBookmarkCount()).isEqualTo(1);
        verify(bookmarkRepository, never()).save(any());
        verify(roomRepository, never()).incrementBookmarkCount(any());
    }

    @Test
    @DisplayName("3. Bỏ bookmark thành công và giảm bookmarkCount")
    void shouldRemoveBookmarkSuccessfully() {
        String userId = "u-tenant-01";
        Long roomId = 101L;

        Room room = Room.builder().id(roomId).bookmarkCount(1).build();
        Room updated = Room.builder().id(roomId).bookmarkCount(0).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room)).thenReturn(Optional.of(updated));
        when(bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)).thenReturn(true);

        BookmarkStatusDto status = bookmarkService.removeBookmark(userId, roomId);

        assertThat(status.isBookmarked()).isFalse();
        assertThat(status.getBookmarkCount()).isEqualTo(0);
        verify(bookmarkRepository, times(1)).deleteByUserIdAndRoomId(userId, roomId);
        verify(roomRepository, times(1)).decrementBookmarkCount(roomId);
    }

    @Test
    @DisplayName("4. Bỏ bookmark idempotent: gọi lần 2 khi chưa bookmark không giảm và không throw lỗi")
    void shouldBeIdempotentWhenRemovingNonExistentBookmark() {
        String userId = "u-tenant-01";
        Long roomId = 101L;

        Room room = Room.builder().id(roomId).bookmarkCount(0).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)).thenReturn(false);

        BookmarkStatusDto status = bookmarkService.removeBookmark(userId, roomId);

        assertThat(status.isBookmarked()).isFalse();
        assertThat(status.getBookmarkCount()).isEqualTo(0);
        verify(bookmarkRepository, never()).deleteByUserIdAndRoomId(any(), any());
        verify(roomRepository, never()).decrementBookmarkCount(any());
    }

    @Test
    @DisplayName("5. Ném lỗi 404 NOT_FOUND khi phòng không tồn tại")
    void shouldThrow404WhenRoomNotFound() {
        String userId = "u-tenant-01";
        Long nonExistentRoomId = 9999L;

        when(roomRepository.findById(nonExistentRoomId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> bookmarkService.addBookmark(userId, nonExistentRoomId));
        assertThrows(ResponseStatusException.class, () -> bookmarkService.removeBookmark(userId, nonExistentRoomId));
    }

    @Test
    @DisplayName("6. Lấy danh sách bookmark của người dùng đúng thứ tự")
    void shouldGetMyBookmarks() {
        String userId = "u-tenant-01";
        when(bookmarkRepository.findRoomIdsByUserId(userId)).thenReturn(List.of(101L, 102L));

        RoomDto dto1 = RoomDto.builder().id(101L).title("Phòng 101").build();
        RoomDto dto2 = RoomDto.builder().id(102L).title("Phòng 102").build();

        when(roomQueryService.getRoomById(101L)).thenReturn(dto1);
        when(roomQueryService.getRoomById(102L)).thenReturn(dto2);

        List<RoomDto> list = bookmarkService.getMyBookmarks(userId);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(101L);
        assertThat(list.get(0).getIsBookmarked()).isTrue();
        assertThat(list.get(1).getId()).isEqualTo(102L);
        assertThat(list.get(1).getIsBookmarked()).isTrue();
    }
}
