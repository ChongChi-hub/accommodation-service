package vn.cozynest.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class FaceChallengeResponse {
    private String nonce;
    private Instant expiresAt;
    private List<ActionItem> actions;
    private int attemptNo;
    private int remainingAttempts;

    @Getter
    @Builder
    public static class ActionItem {
        private String code;
        private String description;
    }
}

