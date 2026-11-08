package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.BannerDto;
import vn.cozynest.listing.application.service.HomeDataService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cms")
@RequiredArgsConstructor
@Tag(name = "CMS & Marketing", description = "Banner và thông báo trang chủ")
public class CmsController {

    private final HomeDataService homeDataService;

    @GetMapping("/banners")
    @Operation(summary = "Lấy danh sách các banner quảng bá đang kích hoạt")
    public ResponseEntity<ApiResponse<List<BannerDto>>> getActiveBanners() {
        return ResponseEntity.ok(ApiResponse.success(homeDataService.getBanners()));
    }
}
