package vn.cozynest.identity.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceDto {
    private String userId;

    @JsonProperty("isOnline")
    private boolean isOnline;

    private Instant lastActiveAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("isOnline")
    public boolean isOnline() {
        return isOnline;
    }

    @JsonProperty("isOnline")
    public void setOnline(boolean online) {
        this.isOnline = online;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
