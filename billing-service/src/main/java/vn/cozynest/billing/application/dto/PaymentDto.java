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

            @Min(value = 0, message = "Số tiền thanh toán không hợp lệ")
            Long amount
    ) {
        public CreateIntentRequest {
            if (amount == null) {
                amount = 0L;
            }
        }
    }

    public record SubmitIntentRequest(
            String note,
            String receiptUrl,
            String payerName
    ) {
        public SubmitIntentRequest(String note, String receiptUrl) {
            this(note, receiptUrl, null);
        }
    }

    public record ConfirmPaymentRequest(
            @Min(value = 1, message = "Số tiền thực nhận phải lớn hơn 0")
            long actualAmount,

            @NotBlank(message = "Mã giao dịch ngân hàng không được để trống")
            String bankTxCode,

            String note
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

    public record AdminIntentResponse(
            String id,
            String paymentCode,
            String payerId,
            String orderType,
            String orderId,
            long amount,
            long paidAmount,
            String status,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record AdminApprovalItem(
            String id,
            String paymentCode,
            String payerId,
            String tenantName,
            String orderType,
            String orderId,
            long amount,
            long paidAmount,
            String status,
            String submittedBankTxCode,
            boolean hasProofImage,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record WebhookPayload(
            String paymentCode,
            long amount,
            String status,
            long timestamp
    ) {}
}
