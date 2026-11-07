package vn.toam.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.toam.common.dto.ApiResponse;
import vn.toam.identity.application.dto.*;
import vn.toam.identity.application.usecase.*;

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

    public AuthController(RegisterUseCase registerUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          GetProfileUseCase getProfileUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          ForgotPasswordUseCase forgotPasswordUseCase,
                          ResetPasswordUseCase resetPasswordUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(@Valid @RequestBody RegisterCommand command) {
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

        ResponseCookie cookie = ResponseCookie.from("toam_token", result.getToken())
                .httpOnly(true)
                .secure(false)
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
                                                           @Valid @RequestBody ChangePasswordCommand command) {
        String userId = (String) authentication.getPrincipal();
        changePasswordUseCase.execute(userId, command);
        return ResponseEntity.ok(ApiResponse.message("Đổi mật khẩu thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("toam_token", "")
                .httpOnly(true)
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
}

