package vn.cozynest.identity.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.TwoFactorDto;
import vn.cozynest.identity.application.service.TwoFactorAuthService;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class TwoFactorAuthController {
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;

    public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService,
                                   UserRepository userRepository) {
        this.twoFactorAuthService = twoFactorAuthService;
        this.userRepository = userRepository;
    }

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFactorDto.SetupResponse>> setup(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        TwoFactorDto.SetupResponse response = twoFactorAuthService.setup(userId, user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo 2FA thành công", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TwoFactorDto.StatusResponse>> verify(
            @Valid @RequestBody TwoFactorDto.VerifyRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        boolean success = twoFactorAuthService.verifyAndEnable(userId, request.code());
        if (!success) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã xác thực 2FA không chính xác", "ERR_AUTH_INVALID_2FA_CODE"));
        }
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt bảo mật 2 lớp thành công", new TwoFactorDto.StatusResponse(true)));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<TwoFactorDto.StatusResponse>> disable(
            @Valid @RequestBody TwoFactorDto.VerifyRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        boolean success = twoFactorAuthService.disable(userId, request.code());
        if (!success) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã xác thực 2FA không chính xác", "ERR_AUTH_INVALID_2FA_CODE"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã hủy kích hoạt bảo mật 2 lớp", new TwoFactorDto.StatusResponse(false)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TwoFactorDto.StatusResponse>> getStatus(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        TwoFactorDto.StatusResponse status = twoFactorAuthService.getStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
