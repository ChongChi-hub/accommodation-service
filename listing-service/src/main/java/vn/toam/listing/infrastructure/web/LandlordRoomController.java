package vn.toam.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.toam.common.dto.ApiResponse;
import vn.toam.common.dto.PaginationResponse;
import vn.toam.listing.application.dto.*;
import vn.toam.listing.application.service.LandlordImageService;
import vn.toam.listing.application.service.LandlordRoomManagementService;
import vn.toam.listing.application.service.LandlordRoomQueryService;
import vn.toam.listing.domain.model.Room;
import vn.toam.listing.infrastructure.security.LandlordContextResolver;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/landlord/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LANDLORD')")
@Tag(name = "Landlord Rooms", description = "Quản lý phòng trọ và hình ảnh dành cho Chủ trọ")
public class LandlordRoomController {

    private final LandlordRoomQueryService queryService;
    private final LandlordRoomManagementService managementService;
    private final LandlordImageService imageService;
    private final LandlordContextResolver contextResolver;

    private Long getRequiredLandlordId() {
        Long landlordId = contextResolver.getCurrentLandlordId();
        if (landlordId == null) {
            throw new NoSuchElementException("Không thể xác định danh tính chủ trọ từ phiên đăng nhập.");
        }
        return landlordId;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng của chủ trọ có tìm kiếm, lọc và phân trang")
    public ResponseEntity<ApiResponse<List<LandlordRoomListItemDto>>> getRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Long landlordId = getRequiredLandlordId();
        Page<LandlordRoomListItemDto> result = queryService.getLandlordRooms(landlordId, search, status, sort, page, size);

        PaginationResponse pagination = PaginationResponse.of(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );

        ApiResponse<List<LandlordRoomListItemDto>> apiResponse = ApiResponse.<List<LandlordRoomListItemDto>>builder()
                .data(result.getContent())
                .pagination(pagination)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/summary")
    @Operation(summary = "Lấy thống kê số lượng phòng theo từng trạng thái")
    public ResponseEntity<ApiResponse<RoomSummaryDto>> getSummary() {
        Long landlordId = getRequiredLandlordId();
        RoomSummaryDto summary = queryService.getRoomSummary(landlordId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết phòng của chủ trọ (bao gồm danh sách ảnh)")
    public ResponseEntity<ApiResponse<LandlordRoomDetailDto>> getDetail(@PathVariable Long id) {
        Long landlordId = getRequiredLandlordId();
        LandlordRoomDetailDto detail = queryService.getRoomDetail(landlordId, id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Bật/Tắt trạng thái ẩn/hiện phòng")
    public ResponseEntity<ApiResponse<String>> toggleVisibility(
            @PathVariable Long id,
            @Valid @RequestBody VisibilityRequest request
    ) {
        Long landlordId = getRequiredLandlordId();
        String actorId = contextResolver.getCurrentActorId();
        Room updated = managementService.toggleVisibility(landlordId, id, request.getVisible(), actorId);
        String msg = "HIDDEN".equalsIgnoreCase(updated.getStatus()) ? "Đã tạm ẩn bài đăng thành công" : "Đã mở lại bài đăng thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, msg));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm bài đăng phòng trọ")
    public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable Long id) {
        Long landlordId = getRequiredLandlordId();
        String actorId = contextResolver.getCurrentActorId();
        managementService.softDeleteRoom(landlordId, id, actorId);
        return ResponseEntity.ok(ApiResponse.message("Đã xóa bài đăng phòng trọ thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin chi tiết phòng trọ")
    public ResponseEntity<ApiResponse<String>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        Long landlordId = getRequiredLandlordId();
        String actorId = contextResolver.getCurrentActorId();
        managementService.updateRoom(landlordId, id, request, actorId);
        return ResponseEntity.ok(ApiResponse.message("Cập nhật thông tin phòng thành công"));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải lên hình ảnh cho phòng (tối đa 10 ảnh, tự động sinh 3 kích cỡ WebP)")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files
    ) {
        Long landlordId = getRequiredLandlordId();
        String actorId = contextResolver.getCurrentActorId();
        ImageUploadResponse response = imageService.uploadImages(landlordId, id, files, actorId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/images/{imageId}/cover")
    @Operation(summary = "Đặt ảnh làm ảnh bìa chính của phòng")
    public ResponseEntity<ApiResponse<String>> setCover(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        Long landlordId = getRequiredLandlordId();
        imageService.setCoverImage(landlordId, id, imageId);
        return ResponseEntity.ok(ApiResponse.message("Đã đặt ảnh bìa thành công"));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Xóa ảnh của phòng trọ")
    public ResponseEntity<ApiResponse<String>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        Long landlordId = getRequiredLandlordId();
        imageService.deleteImage(landlordId, id, imageId);
        return ResponseEntity.ok(ApiResponse.message("Đã xóa ảnh thành công"));
    }

    @PutMapping("/{id}/images/order")
    @Operation(summary = "Sắp xếp lại thứ tự hiển thị của các ảnh")
    public ResponseEntity<ApiResponse<String>> reorderImages(
            @PathVariable Long id,
            @Valid @RequestBody ReorderImagesRequest request
    ) {
        Long landlordId = getRequiredLandlordId();
        imageService.reorderImages(landlordId, id, request.getImageIds());
        return ResponseEntity.ok(ApiResponse.message("Đã cập nhật thứ tự ảnh thành công"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        // Return 404 for IDOR protection: accessing another landlord's room behaves as if it doesn't exist
        return ResponseEntity.status(404).body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(400).body(ApiResponse.error(ex.getMessage(), "BUSINESS_RULE_VIOLATION"));
    }
}
