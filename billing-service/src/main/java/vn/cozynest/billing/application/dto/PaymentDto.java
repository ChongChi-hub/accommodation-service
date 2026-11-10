package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class PaymentDto {

    public record CreateIntentRequest(
            @NotBlank(message = "Loại giao dịch không được để trống")
            String orderType,

            @NotBlank(message = "Mã đơn hàng không được để trống")
            String orderId,

            @Min(value = 1000, message = "Số tiền thanh toán tối thiểu là 1,000 VND")
            long amount
    ) {}

    public record IntentResponse(
            String id,
            String paymentCode,
            String orderType,
            String orderId,
            long amount,
            long paidAmount,
            String qrUrl,
            String status,
            Instant expiresAt
    ) {}

    public record WebhookPayload(
            String paymentCode,
            long amount,
            String status,
            long timestamp
    ) {}
}

