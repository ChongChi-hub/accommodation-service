package vn.toam.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.toam.common.dto.ApiResponse;
import vn.toam.identity.application.dto.KycModerationDetailDto;
import vn.toam.identity.application.dto.KycModerationListItemDto;
import vn.toam.identity.application.dto.RejectKycRequest;
import vn.toam.identity.application.usecase.KycModerationUseCase;
import vn.toam.identity.domain.model.KycStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/moderation/kyc")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class KycModerationController {

    private final KycModerationUseCase moderationUseCase;

    public KycModerationController(KycModerationUseCase moderationUseCase) {
        this.moderationUseCase = moderationUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KycModerationListItemDto>>> getKycList(
            @RequestParam(value = "status", required = false) KycStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<KycModerationListItemDto> list = moderationUseCase.getKycList(status, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KycModerationDetailDto>> getKycDetail(
            Authentication authentication,
            @PathVariable("id") String id,
            HttpServletRequest request) {
        String reviewerId = (String) authentication.getPrincipal();
        String reviewerRole = authentication.getAuthorities().iterator().next().getAuthority();
        String clientIp = request.getRemoteAddr();

        KycModerationDetailDto detail = moderationUseCase.getKycDetail(reviewerId, reviewerRole, id, clientIp);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/{id}/images/{side}")
    public ResponseEntity<byte[]> streamKycImage(
            Authentication authentication,
            @PathVariable("id") String id,
            @PathVariable("side") String side,
            HttpServletRequest request) {
        String reviewerId = (String) authentication.getPrincipal();
        String reviewerRole = authentication.getAuthorities().iterator().next().getAuthority();
        String clientIp = request.getRemoteAddr();

        byte[] imageBytes = moderationUseCase.streamKycImage(reviewerId, reviewerRole, id, side, clientIp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/webp"));
        headers.set(HttpHeaders.CACHE_CONTROL, "no-store, private, max-age=0");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveKyc(
            Authentication authentication,
            @PathVariable("id") String id,
            @RequestParam(value = "version", required = false) Long version,
            HttpServletRequest request) {
        String reviewerId = (String) authentication.getPrincipal();
        String reviewerRole = authentication.getAuthorities().iterator().next().getAuthority();
        String clientIp = request.getRemoteAddr();

        moderationUseCase.approveKyc(reviewerId, reviewerRole, id, version, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Phe duyet ho so KYC thanh cong"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            Authentication authentication,
            @PathVariable("id") String id,
            @RequestParam(value = "version", required = false) Long version,
            @Valid @RequestBody RejectKycRequest body,
            HttpServletRequest request) {
        String reviewerId = (String) authentication.getPrincipal();
        String reviewerRole = authentication.getAuthorities().iterator().next().getAuthority();
        String clientIp = request.getRemoteAddr();

        moderationUseCase.rejectKyc(reviewerId, reviewerRole, id, body, version, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Tu choi ho so KYC thanh cong"));
    }
}

