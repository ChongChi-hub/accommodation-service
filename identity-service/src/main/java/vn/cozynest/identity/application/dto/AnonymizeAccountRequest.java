package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymizeAccountRequest {

    @AssertTrue(message = "Bạn cần xác nhận đồng ý xóa và ẩn danh tài khoản")
    private boolean confirm;

    private String reason;
}
