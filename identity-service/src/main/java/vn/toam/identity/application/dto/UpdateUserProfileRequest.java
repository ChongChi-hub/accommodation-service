package vn.toam.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @NotBlank(message = "Ho va ten khong duoc de trong")
    @Size(min = 2, max = 150, message = "Ho va ten phai tu 2 den 150 ky tu")
    private String fullName;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    @Pattern(
            regexp = "^(\\+84|0)(3|5|7|8|9)[0-9]{8}$",
            message = "So dien thoai khong dung dinh dang Viet Nam (vi du: 0912345678)"
    )
    private String phone;

    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Dia chi chi tiet khong qua 255 ky tu")
    private String addressDetail;

    private String provinceCode;

    private String wardId;

    @Size(max = 1000, message = "Tieu su gioi thieu khong qua 1000 ky tu")
    private String bio;

    private String locale;

    @NotNull(message = "Phien ban version la bat buoc de kiem tra dong thoi")
    private Long version;
}

