package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordCommand {

    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    @ToString.Exclude
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
    @ToString.Exclude
    private String newPassword;

    private String currentTokenHash;

    private String ipAddress;
}
