package vn.cozynest.identity.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.LinkGoogleIdentityCommand;
import vn.cozynest.identity.application.dto.UserIdentityDto;
import vn.cozynest.identity.application.usecase.ManageUserIdentitiesUseCase;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/me/identities", "/api/v1/users/me/identities"})
public class UserIdentityController {

    private final ManageUserIdentitiesUseCase manageUserIdentitiesUseCase;

    public UserIdentityController(ManageUserIdentitiesUseCase manageUserIdentitiesUseCase) {
        this.manageUserIdentitiesUseCase = manageUserIdentitiesUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserIdentityDto>>> getIdentities(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<UserIdentityDto> list = manageUserIdentitiesUseCase.getIdentities(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<UserIdentityDto>> linkGoogle(
            @Valid @RequestBody LinkGoogleIdentityCommand command,
            Authentication authentication,
            HttpServletRequest request) {
        String userId = (String) authentication.getPrincipal();
        command.setIpAddress(extractClientIp(request));
        command.setUserAgent(request.getHeader("User-Agent"));

        UserIdentityDto linked = manageUserIdentitiesUseCase.linkGoogle(userId, command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Liên kết tài khoản Google thành công", linked));
    }

    @DeleteMapping("/google")
    public ResponseEntity<ApiResponse<Void>> unlinkGoogle(
            Authentication authentication,
            HttpServletRequest request) {
        String userId = (String) authentication.getPrincipal();
        manageUserIdentitiesUseCase.unlinkGoogle(userId, extractClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("Hủy liên kết tài khoản Google thành công", null));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}

