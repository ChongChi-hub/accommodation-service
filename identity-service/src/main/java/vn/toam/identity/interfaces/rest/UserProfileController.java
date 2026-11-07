package vn.toam.identity.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.toam.common.dto.ApiResponse;
import vn.toam.identity.application.dto.AvatarUploadResponse;
import vn.toam.identity.application.dto.UpdateUserProfileRequest;
import vn.toam.identity.application.dto.UserProfileResponse;
import vn.toam.identity.application.usecase.UserProfileUseCase;

@RestController
@RequestMapping("/api/v1/users/profile")
public class UserProfileController {

    private final UserProfileUseCase userProfileUseCase;

    public UserProfileController(UserProfileUseCase userProfileUseCase) {
        this.userProfileUseCase = userProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UserProfileResponse response = userProfileUseCase.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        String userId = (String) authentication.getPrincipal();
        UserProfileResponse response = userProfileUseCase.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat ho so ca nhan thanh cong", response));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<AvatarUploadResponse>> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        String userId = (String) authentication.getPrincipal();
        AvatarUploadResponse response = userProfileUseCase.uploadAvatar(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Tai len anh dai dien thanh cong", response));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        userProfileUseCase.deleteAvatar(userId);
        return ResponseEntity.ok(ApiResponse.message("Xoa anh dai dien thanh cong"));
    }
}

