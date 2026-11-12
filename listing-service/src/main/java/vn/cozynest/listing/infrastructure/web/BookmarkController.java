package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.BookmarkStatusDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.service.BookmarkService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Quản lý phòng trọ đã lưu (yêu thích) của người dùng")
public class BookmarkController {

    private final BookmarkService bookmarkService;

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
