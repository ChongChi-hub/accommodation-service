package vn.cozynest.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.ConsentHistoryDto;
import vn.cozynest.identity.application.dto.ConsentRequestDto;
import vn.cozynest.identity.domain.model.ConsentHistory;
import vn.cozynest.identity.domain.repository.ConsentHistoryRepository;

import java.util.List;

@Service
@Transactional
public class ConsentUseCase {

    private final ConsentHistoryRepository consentHistoryRepository;

    public ConsentUseCase(ConsentHistoryRepository consentHistoryRepository) {
        this.consentHistoryRepository = consentHistoryRepository;
    }

    public ConsentHistoryDto recordConsent(String userId, ConsentRequestDto req, String ipAddress, String userAgent) {
        ConsentHistory history = ConsentHistory.of(
                userId,
                req.getConsentType(),
                req.getVersion(),
                req.isAgreed(),
                ipAddress,
                userAgent,
                req.getMetadataJson()
        );
        ConsentHistory saved = consentHistoryRepository.save(history);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsentHistoryDto> getConsentHistory(String userId) {
        return consentHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private ConsentHistoryDto toDto(ConsentHistory domain) {
        return ConsentHistoryDto.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .consentType(domain.getConsentType())
                .version(domain.getVersion())
                .agreed(domain.isAgreed())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
