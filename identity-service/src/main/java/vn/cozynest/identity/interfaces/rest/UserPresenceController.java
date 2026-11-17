package vn.cozynest.identity.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.UserPresenceDto;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPresenceController {

    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @GetMapping("/{userId}/presence")
    public ResponseEntity<ApiResponse<UserPresenceDto>> getUserPresence(@PathVariable String userId) {
        String resolvedId = resolveUserId(userId);
        Instant now = Instant.now();

        boolean isOnline = refreshTokenRepository.existsActiveTokenByUserId(resolvedId, now);
        Instant lastActiveAt;
        if (isOnline) {
            lastActiveAt = now;
        } else {
            Optional<Instant> revokedAt = refreshTokenRepository.findLatestRevocationByUserId(resolvedId);
            if (revokedAt.isPresent()) {
                lastActiveAt = revokedAt.get();
            } else {
                lastActiveAt = userRepository.findById(resolvedId)
                        .map(u -> u.getLastLoginAt() != null ? u.getLastLoginAt() : u.getUpdatedAt())
                        .orElse(now);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                UserPresenceDto.builder()
                        .userId(userId)
                        .isOnline(isOnline)
                        .lastActiveAt(lastActiveAt)
                        .build()
        ));
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) return userId;
        String trimmed = userId.trim();
        if ("2122472122".equals(trimmed) || "u-landlord-2122472122".equals(trimmed)
                || "1".equals(trimmed) || "u-landlord-01".equals(trimmed) || "u-landlord-1".equals(trimmed)) {
            return "u-huy-01";
        }
        if ("2".equals(trimmed) || "u-landlord-02".equals(trimmed) || "u-landlord-2".equals(trimmed)) {
            return "u-hao-01";
        }
        return trimmed;
    }
}
