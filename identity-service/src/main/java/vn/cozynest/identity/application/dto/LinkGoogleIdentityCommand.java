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
public class LinkGoogleIdentityCommand {
    @NotBlank(message = "Google ID token không được để trống")
    private String credential;

    @NotBlank(message = "Mã xác thực nonce không được để trống")
    private String nonce;

    private String ipAddress;
    private String userAgent;
}

