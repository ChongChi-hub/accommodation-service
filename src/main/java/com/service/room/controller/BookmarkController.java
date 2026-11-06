package com.service.room.controller;

import com.service.common.dto.ApiResponse;
import com.service.room.dto.AddBookmarkRequest;
import com.service.room.service.BookmarkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            Authentication authentication,
            @Valid @RequestBody AddBookmarkRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        bookmarkService.addBookmark(userId, request.getRoomId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.message("Đã thêm vào yêu thích"));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(
            Authentication authentication,
            @PathVariable String roomId
    ) {
        String userId = (String) authentication.getPrincipal();
        bookmarkService.removeBookmark(userId, roomId);
        return ResponseEntity.ok(ApiResponse.message("Đã xóa khỏi yêu thích"));
    }

    @GetMapping("/check/{roomId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkBookmark(
            Authentication authentication,
            @PathVariable String roomId
    ) {
        String userId = (String) authentication.getPrincipal();
        boolean isBookmarked = bookmarkService.checkBookmark(userId, roomId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("isBookmarked", isBookmarked)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listBookmarks(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.listBookmarks(userId)));
    }
}

