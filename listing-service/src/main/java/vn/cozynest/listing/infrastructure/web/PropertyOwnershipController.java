package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.ReviewOwnershipDocRequest;
import vn.cozynest.listing.application.dto.UploadOwnershipDocRequest;
import vn.cozynest.listing.application.service.PropertyOwnershipService;
import vn.cozynest.listing.domain.model.PropertyOwnershipDocument;
import vn.cozynest.listing.infrastructure.security.LandlordContextResolver;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@Tag(name = "Property Ownership KYC", description = "Xac minh quyen cho thue nha (KYC-03)")
public class PropertyOwnershipController {

    private final PropertyOwnershipService ownershipService;
    private final LandlordContextResolver contextResolver;

    @PostMapping("/api/v1/landlord/rooms/{roomId}/ownership-docs")
    @PreAuthorize("hasRole('LANDLORD')")
    @Operation(summary = "Chu tro tai len giay to chung minh quyen cho thue")
    public ResponseEntity<ApiResponse<PropertyOwnershipDocument>> uploadDoc(
            @PathVariable Long roomId,
            @Valid @RequestBody UploadOwnershipDocRequest request
    ) {
        Long landlordId = contextResolver.getCurrentLandlordId();
        if (landlordId == null) {
            throw new NoSuchElementException("Khong the xac dinh danh tinh chu tro tu phien dang nhap.");
        }
        PropertyOwnershipDocument doc = ownershipService.uploadDocument(
                landlordId, roomId, request.getDocType(), request.getFileUrl(), request.getRawNumber(), request.getExpiresAt()
        );
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    @GetMapping("/api/v1/landlord/rooms/{roomId}/ownership-docs")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Xem danh sach giay to so huu cua phong")
    public ResponseEntity<ApiResponse<List<PropertyOwnershipDocument>>> getDocs(@PathVariable Long roomId) {
        Long landlordId = contextResolver.getCurrentLandlordId();
        boolean privileged = contextResolver.isPrivilegedModeratorOrAdmin();
        return ResponseEntity.ok(ApiResponse.success(ownershipService.getDocumentsByRoom(roomId, landlordId, privileged)));
    }

    @PostMapping("/api/v1/admin/rooms/ownership-docs/{docId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(summary = "Moderator kiem duyet giay to so huu")
    public ResponseEntity<ApiResponse<PropertyOwnershipDocument>> reviewDoc(
            @PathVariable String docId,
            @Valid @RequestBody ReviewOwnershipDocRequest request
    ) {
        String reviewerId = contextResolver.getCurrentActorId();
        PropertyOwnershipDocument doc = ownershipService.reviewDocument(
                docId, reviewerId, request.isApprove(), request.getRejectReason()
        );
        return ResponseEntity.ok(ApiResponse.success(doc));
    }
}

