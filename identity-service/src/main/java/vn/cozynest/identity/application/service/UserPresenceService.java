package vn.cozynest.identity.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.cozynest.identity.application.dto.UserPresenceDto;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceService {

    // Threshold in seconds to consider user online (heartbeat received within last 45s)
    private static final long ONLINE_THRESHOLD_SECONDS = 45;

    private final ConcurrentMap<String, Instant> userHeartbeats = new ConcurrentHashMap<>();
    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public void recordHeartbeat(String userId) {
        if (userId == null || userId.isBlank()) return;
        String canonical = resolveUserId(userId);
        userHeartbeats.put(canonical, Instant.now());
    }

    public void recordOffline(String userId) {
        if (userId == null || userId.isBlank()) return;
        String canonical = resolveUserId(userId);
        userHeartbeats.remove(canonical);
    }

    public UserPresenceDto getPresence(String userId) {
        String resolvedId = resolveUserId(userId);
        Instant now = Instant.now();
        Instant lastHeartbeat = userHeartbeats.get(resolvedId);

        boolean isOnline = false;
        Instant lastActiveAt = null;

        if (lastHeartbeat != null) {
            long secondsAgo = Duration.between(lastHeartbeat, now).toSeconds();
            if (secondsAgo <= ONLINE_THRESHOLD_SECONDS) {
                isOnline = true;
                lastActiveAt = lastHeartbeat;
            } else {
                isOnline = false;
                lastActiveAt = lastHeartbeat;
            }
        }

        if (lastActiveAt == null) {
            Optional<Instant> revokedAt = refreshTokenRepository.findLatestRevocationByUserId(resolvedId);
            if (revokedAt.isPresent()) {
                lastActiveAt = revokedAt.get();
            } else {
                lastActiveAt = userRepository.findById(resolvedId)
                        .map(u -> u.getLastLoginAt() != null ? u.getLastLoginAt() : u.getUpdatedAt())
                        .orElse(now);
            }
        }

        return UserPresenceDto.builder()
                .userId(userId)
                .isOnline(isOnline)
                .lastActiveAt(lastActiveAt)
                .build();
    }

    public String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) return userId;
        String trimmed = userId.trim();
        if ("1".equals(trimmed) || "u-landlord-1".equals(trimmed) || "2122472122".equals(trimmed) || "u-landlord-2122472122".equals(trimmed)) {
            return "u-landlord-01";
        }
        if ("2".equals(trimmed) || "u-landlord-2".equals(trimmed)) {
            return "u-landlord-02";
        }
        if ("u-tenant-1".equals(trimmed)) {
            return "u-tenant-01";
        }
        if ("u-tenant-2".equals(trimmed)) {
            return "u-tenant-02";
        }
        return trimmed;
    }
}
