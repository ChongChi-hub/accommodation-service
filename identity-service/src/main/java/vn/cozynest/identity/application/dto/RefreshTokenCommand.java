package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenCommand {
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;

    private String deviceInfo;
    private String ipAddress;
}

