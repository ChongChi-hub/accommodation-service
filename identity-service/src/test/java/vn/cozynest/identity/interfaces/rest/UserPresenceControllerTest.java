package vn.cozynest.identity.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.UserPresenceDto;
import vn.cozynest.identity.application.service.UserPresenceService;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPresenceControllerTest {

    @Test
    @DisplayName("getUserPresence returns online=true when recent heartbeat exists")
    void shouldReturnOnlineWhenHeartbeatReceived() {
        SpringDataRefreshTokenRepository tokenRepo = mock(SpringDataRefreshTokenRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        UserPresenceService presenceService = new UserPresenceService(tokenRepo, userRepo);
        presenceService.recordHeartbeat("u-landlord-01");

        UserPresenceController controller = new UserPresenceController(presenceService);
        ResponseEntity<ApiResponse<UserPresenceDto>> response = controller.getUserPresence("u-landlord-01");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isOnline()).isTrue();
        assertThat(response.getBody().getData().getUserId()).isEqualTo("u-landlord-01");
    }

    @Test
    @DisplayName("getUserPresence returns online=false and lastActiveAt when offline")
    void shouldReturnOfflineWithLastActiveAt() {
        SpringDataRefreshTokenRepository tokenRepo = mock(SpringDataRefreshTokenRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        Instant logoutTime = Instant.parse("2026-11-18T02:00:00Z");

        when(tokenRepo.findLatestRevocationByUserId(eq("u-landlord-02"))).thenReturn(Optional.of(logoutTime));

        UserPresenceService presenceService = new UserPresenceService(tokenRepo, userRepo);
        UserPresenceController controller = new UserPresenceController(presenceService);
        ResponseEntity<ApiResponse<UserPresenceDto>> response = controller.getUserPresence("u-landlord-02");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isOnline()).isFalse();
        assertThat(response.getBody().getData().getLastActiveAt()).isEqualTo(logoutTime);
    }
}
