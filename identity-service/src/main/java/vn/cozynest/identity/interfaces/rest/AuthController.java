package vn.cozynest.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.application.usecase.*;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final GoogleAuthUseCase googleAuthUseCase;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProviderPort tokenProvider;
    private final boolean isProduction;

    public AuthController(RegisterUseCase registerUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          GetProfileUseCase getProfileUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          ForgotPasswordUseCase forgotPasswordUseCase,
                          ResetPasswordUseCase resetPasswordUseCase,
                          GoogleAuthUseCase googleAuthUseCase,
                          RefreshTokenRepository refreshTokenRepository,
                          TokenProviderPort tokenProvider,
                          @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.googleAuthUseCase = googleAuthUseCase;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.isProduction = activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(@Valid @RequestBody RegisterCommand command,
                                                             HttpServletRequest request) {
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        AuthResult result = registerUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult>> login(@Valid @RequestBody LoginCommand command,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        if (command.getDeviceInfo() == null) {
            command.setDeviceInfo(request.getHeader("User-Agent"));
        }
        AuthResult result = loginUseCase.execute(command);

        ResponseCookie cookie = ResponseCookie.from("cozynest_token", result.getToken())
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .sameSite("Lax")
                .maxAge(86400)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", result));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResult>> refreshToken(@Valid @RequestBody RefreshTokenCommand command,
                                                               HttpServletRequest request) {
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        if (command.getDeviceInfo() == null) {
            command.setDeviceInfo(request.getHeader("User-Agent"));
        }
        AuthResult result = refreshTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", result));
    }

    @GetMapping({"/profile", "/me"})
    public ResponseEntity<ApiResponse<UserDto>> getProfile(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UserDto profile = getProfileUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication authentication,
                                                           @Valid @RequestBody ChangePasswordCommand command,
                                                           HttpServletRequest request) {
        String userId = (String) authentication.getPrincipal();
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        changePasswordUseCase.execute(userId, command);
        return ResponseEntity.ok(ApiResponse.message("Đổi mật khẩu thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenCommand command,
            Authentication authentication,
            HttpServletResponse response
    ) {
        if (command != null && command.getRefreshToken() != null && !command.getRefreshToken().isBlank()) {
            String tokenHash = tokenProvider.hashToken(command.getRefreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
                t.revoke("LOGOUT");
                refreshTokenRepository.save(t);
            });
        } else if (authentication != null && authentication.getPrincipal() instanceof String userId) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }

        ResponseCookie cookie = ResponseCookie.from("cozynest_token", "")
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.message("Đăng xuất thành công"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordCommand command,
                                                            HttpServletRequest request) {
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        forgotPasswordUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.message("Nếu email tồn tại trên hệ thống, mã xác thực OTP đã được gửi đến hộp thư của bạn."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordCommand command) {
        resetPasswordUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.message("Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới."));
    }

    @GetMapping("/google/status")
    public ResponseEntity<ApiResponse<GoogleStatusResponse>> getGoogleStatus() {
        GoogleStatusResponse status = googleAuthUseCase.getStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/google/nonce")
    public ResponseEntity<ApiResponse<GoogleNonceResponse>> getGoogleNonce() {
        GoogleNonceResponse nonce = googleAuthUseCase.generateNonce();
        return ResponseEntity.ok(ApiResponse.success(nonce));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResult>> googleAuth(
            @Valid @RequestBody GoogleAuthCommand command,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        if (command.getDeviceInfo() == null) {
            command.setDeviceInfo(request.getHeader("User-Agent"));
        }

        AuthResult result = googleAuthUseCase.authenticate(command);

        ResponseCookie cookie = ResponseCookie.from("cozynest_token", result.getToken())
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .sameSite("Lax")
                .maxAge(86400)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Xác thực Google thành công", result));
    }
}
