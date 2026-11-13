package vn.cozynest.identity.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.application.dto.UpdateUserProfileRequest;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.application.dto.UserProfileResponse;
import vn.cozynest.identity.application.usecase.GetProfileUseCase;
import vn.cozynest.identity.application.usecase.LandlordApplicationUseCase;
import vn.cozynest.identity.application.usecase.UserProfileUseCase;

@RestController
@RequestMapping({"/api/v1/me", "/api/v1/users/me"})
public class MeController {

    private final GetProfileUseCase getProfileUseCase;
    private final LandlordApplicationUseCase landlordApplicationUseCase;
    private final UserProfileUseCase userProfileUseCase;

    public MeController(
            GetProfileUseCase getProfileUseCase,
            LandlordApplicationUseCase landlordApplicationUseCase,
            UserProfileUseCase userProfileUseCase
    ) {
        this.getProfileUseCase = getProfileUseCase;
        this.landlordApplicationUseCase = landlordApplicationUseCase;
        this.userProfileUseCase = userProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserDto>> getMe(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UserDto profile = getProfileUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        UserProfileResponse response = userProfileUseCase.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat ho so ca nhan thanh cong", response));
    }

    @PostMapping("/landlord-application")
    public ResponseEntity<ApiResponse<LandlordApplicationSummaryDto>> applyLandlord(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        LandlordApplicationSummaryDto response = landlordApplicationUseCase.apply(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nộp hồ sơ xin làm chủ trọ thành công", response));
    }

    @GetMapping("/landlord-application")
    public ResponseEntity<ApiResponse<LandlordApplicationSummaryDto>> getLandlordApplication(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        LandlordApplicationSummaryDto response = landlordApplicationUseCase.getApplication(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

