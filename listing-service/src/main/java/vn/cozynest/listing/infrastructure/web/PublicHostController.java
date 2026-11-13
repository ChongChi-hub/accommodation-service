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
import vn.cozynest.listing.application.dto.HostPublicProfileDto;
import vn.cozynest.listing.application.service.HostQueryService;

@RestController
@RequestMapping("/api/v1/hosts")
@RequiredArgsConstructor
@Tag(name = "Public Hosts", description = "API hồ sơ công khai chủ trọ")
public class PublicHostController {

    private final HostQueryService hostQueryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các chủ trọ uy tín được xác minh")
    public ResponseEntity<ApiResponse<java.util.List<HostPublicProfileDto>>> getTopHosts() {
        return ResponseEntity.ok(ApiResponse.success(hostQueryService.getTopHosts()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin công khai và danh sách phòng đang đăng của chủ trọ")
    public ResponseEntity<ApiResponse<HostPublicProfileDto>> getPublicHost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hostQueryService.getPublicHostProfile(id)));
    }
}

