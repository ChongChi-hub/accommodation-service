package vn.toam.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public AuthController(RegisterUseCase registerUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          GetProfileUseCase getProfileUseCase,
                          ChangePasswordUseCase changePasswordUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(@Valid @RequestBody RegisterCommand command) {
        AuthResult result = registerUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult>> login(@Valid @RequestBody LoginCommand command,
                                                         HttpServletRequest request) {
        if (command.getIpAddress() == null) {
            command.setIpAddress(request.getRemoteAddr());
        }
        if (command.getDeviceInfo() == null) {
            command.setDeviceInfo(request.getHeader("User-Agent"));
        }
        AuthResult result = loginUseCase.execute(command);
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
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.message("Đăng xuất thành công"));
    }
}

