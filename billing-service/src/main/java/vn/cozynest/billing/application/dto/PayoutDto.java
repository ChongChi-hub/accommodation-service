package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class PayoutDto {

    public record RequestPayoutCommand(
            @Min(value = 1000, message = "Số tiền rút tối thiểu là 1,000 VND")
            long amount,

            @NotBlank(message = "Mã ngân hàng không được để trống")
            String bankCode,

            @NotBlank(message = "Số tài khoản không được để trống")
            String accountNumber,

            @NotBlank(message = "Tên chủ tài khoản không được để trống")
            String accountHolder,

            @NotBlank(message = "Mã OTP xác thực không được để trống")
            String otp
    ) {}

    public record PayoutResponse(
            String id,
            String payoutCode,
            long amount,
            String bankCode,
            String accountNumber,
            String accountHolder,
            String status,
            Instant createdAt,
            String landlordId,
            String failureReason
    ) {
        public PayoutResponse(String id, String payoutCode, long amount, String bankCode,
                              String accountNumber, String accountHolder, String status, Instant createdAt) {
            this(id, payoutCode, amount, bankCode, accountNumber, accountHolder, status, createdAt, null, null);
        }
    }
}

