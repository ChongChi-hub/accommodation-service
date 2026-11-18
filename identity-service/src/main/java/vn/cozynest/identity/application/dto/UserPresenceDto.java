package vn.cozynest.identity.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceDto {
    private String userId;

    @JsonProperty("isOnline")
    private boolean isOnline;

    private Instant lastActiveAt;

    @JsonProperty("isOnline")
    public boolean isOnline() {
        return isOnline;
    }

    @JsonProperty("online")
    public boolean getOnline() {
        return isOnline;
    }
}
