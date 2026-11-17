package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.BookmarkStatusDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.service.BookmarkService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Quản lý phòng trọ đã lưu (yêu thích) của người dùng")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/searches")
    @Operation(summary = "Lấy danh sách bộ lọc tìm kiếm đã lưu")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSavedSearches(Authentication authentication) {
        String userId = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal()))
                ? authentication.getName() : null;
        List<Map<String, Object>> list = new ArrayList<>();
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(list));
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, user_id, title, query_params_json, notify_new_matches, created_at FROM saved_searches WHERE user_id = ? ORDER BY created_at DESC",
                    userId
            );
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", r.get("id"));
                item.put("title", String.valueOf(r.getOrDefault("title", "")));
                item.put("queryParams", String.valueOf(r.getOrDefault("query_params_json", "{}")));
                item.put("notifications", Boolean.TRUE.equals(r.get("notify_new_matches")) || Integer.valueOf(1).equals(r.get("notify_new_matches")));
                item.put("createdAt", String.valueOf(r.getOrDefault("created_at", "")));
                list.add(item);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @DeleteMapping("/searches/{id}")
    @Operation(summary = "Xóa bộ lọc tìm kiếm đã lưu")
    public ResponseEntity<ApiResponse<Void>> deleteSavedSearch(@PathVariable Long id) {
        try {
            jdbcTemplate.update("DELETE FROM saved_searches WHERE id = ?", id);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(ApiResponse.message("Đã xóa bộ lọc tìm kiếm"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách các phòng trọ người dùng đã lưu")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getMyBookmarks(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getMyBookmarks(userId)));
    }

    @GetMapping("/ids")
    @Operation(summary = "Lấy danh sách ID phòng trọ người dùng đã lưu")
    public ResponseEntity<ApiResponse<List<Long>>> getMyBookmarkedRoomIds(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getMyBookmarkedRoomIds(userId)));
    }

    @GetMapping("/{roomId}/status")
    @Operation(summary = "Kiểm tra trạng thái đã lưu phòng và số lượt lưu")
    public ResponseEntity<ApiResponse<BookmarkStatusDto>> getStatus(@PathVariable Long roomId,
                                                                    Authentication authentication) {
        String userId = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal()))
                ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getStatus(userId, roomId)));
    }

    @RequestMapping(value = "/{roomId}", method = {RequestMethod.POST, RequestMethod.PUT})
    @Operation(summary = "Lưu phòng trọ (idempotent POST/PUT)")
    public ResponseEntity<ApiResponse<BookmarkStatusDto>> addBookmark(@PathVariable Long roomId,
                                                                      Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.addBookmark(userId, roomId)));
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Bỏ lưu phòng trọ (idempotent)")
    public ResponseEntity<ApiResponse<BookmarkStatusDto>> removeBookmark(@PathVariable Long roomId,
                                                                         Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.removeBookmark(userId, roomId)));
    }
}
