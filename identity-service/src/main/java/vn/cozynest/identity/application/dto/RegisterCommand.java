package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class RegisterCommand {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự")
    private String email;

    @Pattern(
        regexp = "^$|^((\\+84)|0)(3[2-9]|5[689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$",
        message = "Số điện thoại di động Việt Nam không hợp lệ"
    )
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 60, message = "Họ và tên phải từ 2 đến 60 ký tự")
    @Pattern(
        regexp = "^[\\p{L}\\s'.-]{2,60}$",
        message = "Họ tên chỉ được chứa chữ cái, khoảng trắng và dấu gạch nối"
    )
    private String fullName;

    private String role; // TENANT, LANDLORD

    private String ipAddress;
}
