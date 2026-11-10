package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.BannerDto;
import vn.cozynest.listing.application.dto.CmsArticleDto;
import vn.cozynest.listing.application.service.HomeDataService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cms")
@RequiredArgsConstructor
@Tag(name = "CMS & Marketing", description = "Banner và cẩm nang thuê nhà")
public class CmsController {

    private final HomeDataService homeDataService;

    @GetMapping("/banners")
    @Operation(summary = "Lấy danh sách các banner quảng bá đang kích hoạt")
    public ResponseEntity<ApiResponse<List<BannerDto>>> getActiveBanners() {
        return ResponseEntity.ok(ApiResponse.success(homeDataService.getBanners()));
    }

    @GetMapping("/articles")
    @Operation(summary = "Lấy danh sách các bài viết cẩm nang thuê nhà mới nhất")
    public ResponseEntity<ApiResponse<List<CmsArticleDto>>> getArticles() {
        return ResponseEntity.ok(ApiResponse.success(homeDataService.getArticles()));
    }

    @GetMapping("/articles/{slug}")
    @Operation(summary = "Lấy chi tiết một bài viết cẩm nang theo slug")
    public ResponseEntity<ApiResponse<CmsArticleDto>> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(homeDataService.getArticleBySlug(slug)));
    }
}
