package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.domain.model.AccountType;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final InvoiceRepository invoiceRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public PaymentService(PaymentIntentRepository paymentIntentRepository,
                          InvoiceRepository invoiceRepository,
                          LedgerService ledgerService,
                          ObjectMapper objectMapper,
                          @Value("${cozynest.fintech.webhook-secret:toam_fintech_webhook_secret_2026}") String webhookSecret) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.invoiceRepository = invoiceRepository;
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public PaymentDto.IntentResponse createIntent(String payerId, PaymentDto.CreateIntentRequest request) {
        String paymentCode = "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String encodedAddInfo = URLEncoder.encode(paymentCode, StandardCharsets.UTF_8);
        String qrUrl = "https://img.vietqr.io/image/970422-0912345678-compact2.png?amount="
                + request.amount() + "&addInfo=" + encodedAddInfo + "&accountName=TO%20AM%20ESCROW";

        PaymentIntent intent = PaymentIntent.create(
                paymentCode, payerId, request.orderType(), request.orderId(), request.amount(), qrUrl, 15
        );
        PaymentIntent saved = paymentIntentRepository.save(intent);

        return new PaymentDto.IntentResponse(
                saved.getId(), saved.getPaymentCode(), saved.getOrderType(), saved.getOrderId(),
                saved.getAmount(), saved.getPaidAmount(), saved.getQrUrl(), saved.getStatus(), saved.getExpiresAt()
        );
    }

    @Transactional
    public boolean processWebhook(String rawBody, String signature, long timestamp) {
        // 1. Replay attack protection (5 min window)
        long currentSec = Instant.now().getEpochSecond();
        if (Math.abs(currentSec - timestamp) > 300) {
            throw new IllegalArgumentException("Webhook timestamp lệch quá 5 phút (Replay attack detection)");
        }

        // 2. Validate HMAC-SHA256 signature
        String expectedSig = computeHmacSha256(rawBody + "." + timestamp, webhookSecret);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Chữ ký Webhook HMAC không hợp lệ");
        }

        try {
            PaymentDto.WebhookPayload payload = objectMapper.readValue(rawBody, PaymentDto.WebhookPayload.class);
            PaymentIntent intent = paymentIntentRepository.findByPaymentCode(payload.paymentCode())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã thanh toán: " + payload.paymentCode()));

            // Idempotent: If already processed, return true
            if ("PAID".equals(intent.getStatus())) {
                return true;
            }

            intent.markPaid(payload.amount());
            paymentIntentRepository.save(intent);

            // Double-entry ledger hạch toán
            ledgerService.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Tài khoản tiền về cổng thanh toán", AccountType.ASSET, null);
            ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Tài khoản cọc ký quỹ Escrow", AccountType.LIABILITY, null);
            ledgerService.getOrCreateAccount(LedgerService.ACC_REVENUE, "Doanh thu phí dịch vụ sàn Tổ Ấm", AccountType.REVENUE, null);

            if ("DEPOSIT".equalsIgnoreCase(intent.getOrderType())) {
                // Tiền cọc: DEBIT 1010-GATEWAY, CREDIT 2010-ESCROW
                ledgerService.postEntry(
                        "Thu tiền cọc hợp đồng " + intent.getOrderId(),
                        "DEPOSIT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                        List.of(
                                new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, payload.amount()),
                                new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, payload.amount())
                        )
                );
            } else if ("INVOICE".equalsIgnoreCase(intent.getOrderType())) {
                // Thu tiền phòng: 95% ví chủ trọ, 5% phí sàn
                Invoice invoice = invoiceRepository.findById(intent.getOrderId())
                        .orElse(null);
                String landlordId = invoice != null ? invoice.getLandlordId() : "landlord-system";
                ledgerService.getOrCreateWallet(landlordId, "Chủ trọ");

                long platformFee = payload.amount() * 5 / 100;
                long landlordAmount = payload.amount() - platformFee;

                ledgerService.postEntry(
                        "Thanh toán hóa đơn phòng " + intent.getOrderId(),
                        "INVOICE_PAYMENT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                        List.of(
                                new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, payload.amount()),
                                new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(landlordId), PostingDirection.CREDIT, landlordAmount),
                                new LedgerDto.PostingItem(LedgerService.ACC_REVENUE, PostingDirection.CREDIT, platformFee)
                        )
                );

                if (invoice != null) {
                    invoice.recordPayment(payload.amount());
                    invoiceRepository.save(invoice);
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Xử lý webhook thanh toán thất bại", e);
        }
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính mã băm HMAC-SHA256", e);
        }
    }
}

