package vn.cozynest.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.FaceDecisionRequest;
import vn.cozynest.identity.application.dto.FaceModerationDetailDto;
import vn.cozynest.identity.application.usecase.FaceModerationUseCase;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

@RestController
@RequestMapping({"/api/v1/admin/kyc/{id}/face", "/api/v1/moderation/kyc/{id}/face"})
public class FaceModerationController {

    private final FaceModerationUseCase moderationUseCase;

    public FaceModerationController(FaceModerationUseCase moderationUseCase) {
        this.moderationUseCase = moderationUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or hasAuthority('kyc:review')")
    public ResponseEntity<ApiResponse<FaceModerationDetailDto>> getDetail(
            @PathVariable("id") String id,
            Authentication auth,
            HttpServletRequest request) {
        String role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("MODERATOR");
        FaceModerationDetailDto dto = moderationUseCase.getFaceDetail(auth.getName(), role, id, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/frames/{frameId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or hasAuthority('kyc:review')")
    public ResponseEntity<byte[]> streamFrame(
            @PathVariable("id") String id,
            @PathVariable("frameId") String frameId,
            Authentication auth,
            HttpServletRequest request) {
        String role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("MODERATOR");
        byte[] imageBytes = moderationUseCase.streamFrame(
                auth.getName(), role, id, frameId, getClientIp(request), request.getHeader("User-Agent")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or hasAuthority('kyc:review')")
    public ResponseEntity<ApiResponse<Void>> decide(
            @PathVariable("id") String id,
            @RequestBody FaceDecisionRequest decisionRequest,
            @RequestParam(value = "version", required = false) Long expectedVersion,
            Authentication auth,
            HttpServletRequest request) {
        String role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("MODERATOR");
        moderationUseCase.decide(
                auth.getName(), role, id, decisionRequest, expectedVersion, getClientIp(request)
        );
        return ResponseEntity.ok(ApiResponse.message("Cập nhật quyết định kiểm duyệt khuôn mặt thành công"));
    }

    @ExceptionHandler(IdentityDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(IdentityDomainException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("ERR_FACE_ALREADY_DECIDED".equals(ex.getErrorCode()) || "ERR_FACE_VERSION_CONFLICT".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        } else if ("ERR_SELF_REVIEW_FORBIDDEN".equals(ex.getErrorCode())) {
            status = HttpStatus.FORBIDDEN;
        } else if ("ERR_FACE_NOT_FOUND".equals(ex.getErrorCode()) || "ERR_FACE_FRAME_NOT_FOUND".equals(ex.getErrorCode())) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
