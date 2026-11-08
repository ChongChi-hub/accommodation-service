package vn.cozynest.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.FaceChallengeResponse;
import vn.cozynest.identity.application.dto.FaceStatusResponse;
import vn.cozynest.identity.application.usecase.LandlordFaceVerificationUseCase;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/me/kyc/face", "/api/v1/landlord/kyc/face"})
public class LandlordFaceVerificationController {

    private final LandlordFaceVerificationUseCase faceUseCase;

    public LandlordFaceVerificationController(LandlordFaceVerificationUseCase faceUseCase) {
        this.faceUseCase = faceUseCase;
    }

    @PostMapping("/challenges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FaceChallengeResponse>> createChallenge(
            @RequestBody(required = false) Map<String, Object> body,
            Authentication auth) {
        boolean consent = body != null && Boolean.TRUE.equals(body.get("consent"));
        FaceChallengeResponse response = faceUseCase.createChallenge(auth.getName(), consent);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiên thử thách khuôn mặt thành công", response));
    }

    @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FaceStatusResponse>> submitFrames(
            @RequestParam("nonce") String nonce,
            @RequestParam(value = "livenessClientFlag", defaultValue = "false") boolean livenessClientFlag,
            @RequestParam("neutralFrame") MultipartFile neutralFrame,
            @RequestParam("actionFrames") List<MultipartFile> actionFrames,
            Authentication auth) {
        FaceStatusResponse response = faceUseCase.submitFrames(
                auth.getName(), nonce, livenessClientFlag, neutralFrame, actionFrames
        );
        return ResponseEntity.ok(ApiResponse.success("Nộp khung hình xác thực khuôn mặt thành công", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FaceStatusResponse>> getStatus(Authentication auth) {
        FaceStatusResponse response = faceUseCase.getFaceStatus(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/consent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeConsent(Authentication auth) {
        faceUseCase.revokeConsentAndPurge(auth.getName());
        return ResponseEntity.ok(ApiResponse.message("Đã rút lại đồng ý và xóa toàn bộ dữ liệu khuôn mặt theo yêu cầu"));
    }

    @ExceptionHandler(IdentityDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(IdentityDomainException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("ERR_FACE_RATE_LIMIT_EXCEEDED".equals(ex.getErrorCode())) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if ("ERR_FACE_ALREADY_PASSED".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }
}
