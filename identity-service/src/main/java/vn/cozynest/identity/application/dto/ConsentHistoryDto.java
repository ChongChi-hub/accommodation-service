package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentHistoryDto {
    private String id;
    private String userId;
    private String consentType;
    private String version;
    private boolean agreed;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
