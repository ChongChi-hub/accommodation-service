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
public class GoogleAuthCommand {
    @NotBlank(message = "Google ID token không được để trống")
    private String credential;

    @NotBlank(message = "Nonce không được để trống")
    private String nonce;

    private String intendedRole;

    private String ipAddress;
    private String deviceInfo;
}

