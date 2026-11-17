package vn.cozynest.identity.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.UserPresenceDto;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPresenceControllerTest {

    @Test
    @DisplayName("getUserPresence returns online=true when active token exists")
    void shouldReturnOnlineWhenActiveTokenExists() {
        SpringDataRefreshTokenRepository tokenRepo = mock(SpringDataRefreshTokenRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(tokenRepo.existsActiveTokenByUserId(eq("u-huy-01"), any(Instant.class))).thenReturn(true);

        UserPresenceController controller = new UserPresenceController(tokenRepo, userRepo);
        ResponseEntity<ApiResponse<UserPresenceDto>> response = controller.getUserPresence("2122472122");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isOnline()).isTrue();
        assertThat(response.getBody().getData().getUserId()).isEqualTo("2122472122");
    }

    @Test
    @DisplayName("getUserPresence returns online=false and lastActiveAt when logged out")
    void shouldReturnOfflineWithLastActiveAt() {
        SpringDataRefreshTokenRepository tokenRepo = mock(SpringDataRefreshTokenRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        Instant logoutTime = Instant.parse("2026-11-18T02:00:00Z");

        when(tokenRepo.existsActiveTokenByUserId(eq("u-hao-01"), any(Instant.class))).thenReturn(false);
        when(tokenRepo.findLatestRevocationByUserId("u-hao-01")).thenReturn(Optional.of(logoutTime));

        UserPresenceController controller = new UserPresenceController(tokenRepo, userRepo);
        ResponseEntity<ApiResponse<UserPresenceDto>> response = controller.getUserPresence("u-hao-01");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isOnline()).isFalse();
        assertThat(response.getBody().getData().getLastActiveAt()).isEqualTo(logoutTime);
    }
}
