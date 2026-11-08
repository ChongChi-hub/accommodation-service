package vn.cozynest.identity.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.application.dto.UserDto;
import vn.cozynest.identity.application.usecase.GetProfileUseCase;
import vn.cozynest.identity.application.usecase.LandlordApplicationUseCase;

@RestController
@RequestMapping({"/api/v1/me", "/api/v1/users/me"})
public class MeController {

    private final GetProfileUseCase getProfileUseCase;
    private final LandlordApplicationUseCase landlordApplicationUseCase;

    public MeController(GetProfileUseCase getProfileUseCase, LandlordApplicationUseCase landlordApplicationUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.landlordApplicationUseCase = landlordApplicationUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserDto>> getMe(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UserDto profile = getProfileUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
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

