package vn.cozynest.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.identity.application.dto.ConsentHistoryDto;
import vn.cozynest.identity.application.dto.ConsentRequestDto;
import vn.cozynest.identity.application.usecase.ConsentUseCase;
import vn.cozynest.identity.domain.model.ConsentHistory;
import vn.cozynest.identity.domain.repository.ConsentHistoryRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentUseCaseTest {

    @Mock
    private ConsentHistoryRepository consentHistoryRepository;

    @InjectMocks
    private ConsentUseCase consentUseCase;

    @Test
    @DisplayName("recordConsent saves and returns DTO with correct fields")
    void testRecordConsent() {
        when(consentHistoryRepository.save(any(ConsentHistory.class))).thenAnswer(i -> i.getArgument(0));

        ConsentRequestDto req = ConsentRequestDto.builder()
                .consentType("COOKIE_CONSENT")
                .version("v1.0")
                .agreed(true)
                .metadataJson("{\"analytics\":true,\"marketing\":false}")
                .build();

        ConsentHistoryDto dto = consentUseCase.recordConsent("usr-1", req, "192.168.1.1", "Chrome");

        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo("usr-1");
        assertThat(dto.getConsentType()).isEqualTo("COOKIE_CONSENT");
        assertThat(dto.getVersion()).isEqualTo("v1.0");
        assertThat(dto.isAgreed()).isTrue();
        assertThat(dto.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("getConsentHistory returns descending list of user consents")
    void testGetConsentHistory() {
        when(consentHistoryRepository.findByUserIdOrderByCreatedAtDesc("usr-1")).thenReturn(List.of(
                ConsentHistory.of("usr-1", "PRIVACY_POLICY", "v1.1", true, "127.0.0.1", "Safari", null),
                ConsentHistory.of("usr-1", "TERMS_OF_SERVICE", "v1.0", true, "127.0.0.1", "Safari", null)
        ));

        List<ConsentHistoryDto> history = consentUseCase.getConsentHistory("usr-1");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getConsentType()).isEqualTo("PRIVACY_POLICY");
        assertThat(history.get(1).getConsentType()).isEqualTo("TERMS_OF_SERVICE");
    }
}
