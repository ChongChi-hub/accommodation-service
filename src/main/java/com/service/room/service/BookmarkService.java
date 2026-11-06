package com.service.room.service;

import com.service.common.exception.AppException;
import com.service.room.entity.BookmarkEntity;
import com.service.room.repository.BookmarkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final JdbcTemplate jdbcTemplate;

    public BookmarkService(BookmarkRepository bookmarkRepository, JdbcTemplate jdbcTemplate) {
        this.bookmarkRepository = bookmarkRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void addBookmark(String userId, String roomId) {
        if (bookmarkRepository.existsByUserIdAndRoomId(userId, roomId)) {
            throw new AppException(HttpStatus.CONFLICT, "Phòng đã trong danh sách yêu thích");
        }

        BookmarkEntity bookmark = BookmarkEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .roomId(roomId)
                .build();
        bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(String userId, String roomId) {
        bookmarkRepository.deleteByUserIdAndRoomId(userId, roomId);
    }

    public boolean checkBookmark(String userId, String roomId) {
        return bookmarkRepository.existsByUserIdAndRoomId(userId, roomId);
    }

    public List<Map<String, Object>> listBookmarks(String userId) {
        String sql = """
            SELECT b.id, b.created_at, r.id as room_id, r.title, r.price, r.area, r.address,
                   r.status, rt.name as room_type_name,
                   w.name as ward_name, d.name as district_name, p.name as province_name,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as cover_image
            FROM bookmarks b
            JOIN rooms r ON b.room_id = r.id
            JOIN room_types rt ON r.room_type_id = rt.id
            JOIN wards w ON r.ward_id = w.id
            JOIN districts d ON w.district_id = d.id
            JOIN provinces p ON d.province_id = p.id
            WHERE b.user_id = ?
            ORDER BY b.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql, userId);
    }
}

