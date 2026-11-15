package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public class TwoFactorDto {

    public record SetupResponse(
            String secret,
            String qrUri,
            List<String> backupCodes
    ) {}

    public record VerifyRequest(
            @NotBlank(message = "Mã xác thực không được để trống")
            @Pattern(regexp = "^[0-9]{6,8}$", message = "Mã xác thực phải gồm 6 chữ số TOTP hoặc 8 chữ số dự phòng")
            String code
    ) {}

    public record StatusResponse(
            boolean isEnabled
    ) {}
}

