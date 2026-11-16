package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.PresignedUploadRequestDto;
import vn.cozynest.listing.application.dto.PresignedUploadResponseDto;
import vn.cozynest.listing.application.service.StoragePresignedService;
import vn.cozynest.listing.domain.model.MediaUpload;
import vn.cozynest.listing.infrastructure.security.LandlordContextResolver;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Cloud Object Storage & Presigned URLs", description = "STORAGE-01: Direct Presigned Upload & Download URLs")
public class StoragePresignedController {

    private final StoragePresignedService presignedService;
    private final LandlordContextResolver contextResolver;

    @PostMapping("/presigned-upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Yêu cầu cấp Presigned Upload URL trực tiếp lên Object Storage (S3 / R2)")
    public ResponseEntity<ApiResponse<PresignedUploadResponseDto>> requestPresignedUpload(
            @Valid @RequestBody PresignedUploadRequestDto dto,
            Authentication auth
    ) {
        String uploaderId = contextResolver.getCurrentActorId();
        if (uploaderId == null || uploaderId.isBlank()) {
            uploaderId = auth != null ? auth.getName() : "anonymous";
        }
        PresignedUploadResponseDto response = presignedService.requestPresignedUpload(dto, uploaderId);
        return ResponseEntity.ok(ApiResponse.success("Cấp presigned upload URL thành công", response));
    }

    @PostMapping("/confirm-upload/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xác nhận tệp đã tải lên Object Storage thành công")
    public ResponseEntity<ApiResponse<MediaUpload>> confirmUpload(
            @PathVariable String uploadId,
            Authentication auth
    ) {
        String actorId = contextResolver.getCurrentActorId();
        MediaUpload entity = presignedService.confirmUpload(uploadId, actorId, null);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận tải tệp thành công", entity));
    }

    @PutMapping(value = "/upload/direct", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Endpoint tải lên trực tiếp khi chạy local fallback")
    public ResponseEntity<ApiResponse<MediaUpload>> directUpload(
            @RequestParam("uploadId") String uploadId,
            @RequestBody byte[] fileBytes,
            Authentication auth
    ) {
        String actorId = auth != null ? auth.getName() : "anonymous";
        MediaUpload entity = presignedService.confirmUpload(uploadId, actorId, fileBytes);
        return ResponseEntity.ok(ApiResponse.success("Tải tệp trực tiếp thành công", entity));
    }

    @GetMapping("/presigned-download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cấp URL có chữ ký ngắn hạn để tải tài liệu bảo mật")
    public ResponseEntity<ApiResponse<Map<String, String>>> presignedDownload(
            @RequestParam("objectKey") String objectKey,
            @RequestParam(value = "ttlSeconds", defaultValue = "600") long ttlSeconds
    ) {
        String downloadUrl = presignedService.generatePresignedDownloadUrl(objectKey, ttlSeconds);
        return ResponseEntity.ok(ApiResponse.success(Map.of("downloadUrl", downloadUrl)));
    }
}
