package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;
import vn.cozynest.billing.domain.repository.PaymentWebhookLogRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class PaymentService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final InvoiceRepository invoiceRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final PaymentGatewayPort gatewayPort;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final BillingEventOutboxRepository outboxRepository;
    private final String webhookSecret;
    private final String previousWebhookSecret;
    private final boolean strictIpAllowlist;
    private final Set<String> allowedIps;

    public PaymentService(
            PaymentIntentRepository paymentIntentRepository,
            InvoiceRepository invoiceRepository,
            LedgerService ledgerService,
            ObjectMapper objectMapper,
            PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository,
            String webhookSecret,
            String previousWebhookSecret,
            boolean strictIpAllowlist,
            String allowedIpsRaw) {
        this(paymentIntentRepository, invoiceRepository, ledgerService, objectMapper, gatewayPort,
                webhookLogRepository, Optional.empty(), webhookSecret, previousWebhookSecret,
                strictIpAllowlist, allowedIpsRaw);
    }

    @Autowired
    public PaymentService(
            PaymentIntentRepository paymentIntentRepository,
            InvoiceRepository invoiceRepository,
            LedgerService ledgerService,
            ObjectMapper objectMapper,
            PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository,
            Optional<BillingEventOutboxRepository> outboxRepository,
            @Value("${fintech.gateway.webhook-secret:${cozynest.fintech.webhook-secret:toam_fintech_webhook_secret_2026}}") String webhookSecret,
            @Value("${fintech.gateway.webhook-secret-previous:}") String previousWebhookSecret,
            @Value("${fintech.gateway.webhook-strict-ip-allowlist:false}") boolean strictIpAllowlist,
            @Value("${fintech.gateway.webhook-allowed-ips:127.0.0.1,::1}") String allowedIpsRaw) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.invoiceRepository = invoiceRepository;
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
        this.gatewayPort = gatewayPort;
        this.webhookLogRepository = webhookLogRepository;
        this.outboxRepository = outboxRepository != null ? outboxRepository.orElse(null) : null;
        this.webhookSecret = webhookSecret;
        this.previousWebhookSecret = previousWebhookSecret;
        this.strictIpAllowlist = strictIpAllowlist;
        this.allowedIps = parseIpList(allowedIpsRaw);
    }

    @Transactional
    public PaymentDto.IntentResponse createIntent(String payerId, PaymentDto.CreateIntentRequest request) {
        String paymentCode = "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String qrUrl = gatewayPort.buildPaymentInstructionUrl(paymentCode, request.amount(), request.orderType());
        PaymentIntent saved = paymentIntentRepository.save(PaymentIntent.create(
                paymentCode, payerId, request.orderType(), request.orderId(), request.amount(), qrUrl, 15));
        return toResponse(saved);
    }

    @Transactional
    public PaymentDto.IntentResponse cancelIntent(String paymentCode, String actorId, boolean isAdmin) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã thanh toán: " + paymentCode));
        if (!isAdmin && !intent.getPayerId().equals(actorId)) {
            throw new SecurityException("Không có quyền hủy yêu cầu thanh toán của người khác");
        }
        intent.cancel();
        return toResponse(paymentIntentRepository.save(intent));
    }

    @Transactional(readOnly = true)
    public PaymentDto.IntentResponse getIntent(String paymentCode, String actorId, boolean isAdmin) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ma thanh toan: " + paymentCode));
        if (!isAdmin && !intent.getPayerId().equals(actorId)) {
            throw new WebhookSecurityException(403, "Khong co quyen xem giao dich thanh toan cua nguoi khac");
        }
        return toResponse(intent);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.IntentResponse> getManualInterventionQueue() {
        return paymentIntentRepository.findAll().stream()
                .filter(i -> "OVERPAID".equals(i.getStatus())
                        || "MISMATCH".equals(i.getStatus())
                        || ("EXPIRED".equals(i.getStatus()) && i.getPaidAmount() > 0)
                        || "PARTIAL_PAID".equals(i.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentDto.IntentResponse simulateSandboxWebhook(String paymentCode, long amount, String actorId, boolean isAdmin) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ma thanh toan: " + paymentCode));
        if (!isAdmin && !intent.getPayerId().equals(actorId)) {
            throw new WebhookSecurityException(403, "Khong co quyen gia lap thanh toan cho ma nay");
        }
        long payAmount = amount > 0 ? amount : intent.getAmount();
        long ts = Instant.now().getEpochSecond();
        String rawBody = String.format(
                "{\"paymentCode\":\"%s\",\"amount\":%d,\"status\":\"SUCCESS\",\"timestamp\":%d}",
                paymentCode, payAmount, ts
        );
        String sig = computeHmacSha256(rawBody + "." + ts, webhookSecret);
        processWebhook(rawBody, sig, ts, "127.0.0.1");
        return paymentIntentRepository.findByPaymentCode(paymentCode).map(this::toResponse).orElseThrow();
    }

    @Scheduled(fixedDelayString = "${fintech.gateway.expire-poll-ms:60000}")
    @Transactional
    public int expireOverdueIntents() {
        Instant now = Instant.now();
        int expiredCount = 0;
        for (PaymentIntent intent : paymentIntentRepository.findPendingExpiredBefore(now)) {
            if (intent.markExpiredIfPast(now)) {
                paymentIntentRepository.save(intent);
                expiredCount++;
            }
        }
        return expiredCount;
    }

    @Transactional
    public boolean processWebhook(String rawBody, String signature, long timestamp) {
        return processWebhook(rawBody, signature, timestamp, "127.0.0.1");
    }

    @Transactional
    public boolean processWebhook(String rawBody, String signature, long timestamp, String sourceIp) {
        if (gatewayPort.isKillSwitchActive()) {
            logWebhook(null, sourceIp, false, "KILL_SWITCH_BLOCKED", "Payment gateway kill switch active", rawBody);
            throw new IllegalStateException("Cong thanh toan dang tam khoa");
        }
        if (strictIpAllowlist && (sourceIp == null || !allowedIps.contains(sourceIp))) {
            logWebhook(null, sourceIp, false, "IP_REJECTED", "IP khong thuoc whitelist", rawBody);
            throw new WebhookSecurityException(403, "Webhook IP khong thuoc danh sach tin cay");
        }
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > 300) {
            logWebhook(null, sourceIp, false, "REPLAY_REJECTED", "Timestamp expired", rawBody);
            throw new IllegalArgumentException("Webhook timestamp lệch quá 5 phút");
        }
        if (!isValidSignature(rawBody + "." + timestamp, signature)) {
            logWebhook(null, sourceIp, false, "INVALID_SIGNATURE", "HMAC mismatch", rawBody);
            throw new WebhookSecurityException(401, "Chữ ký Webhook HMAC không hợp lệ");
        }

        try {
            PaymentDto.WebhookPayload payload = objectMapper.readValue(rawBody, PaymentDto.WebhookPayload.class);
            PaymentIntent intent = paymentIntentRepository.findByPaymentCode(payload.paymentCode())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã thanh toán: " + payload.paymentCode()));

            PaymentIntent.PaymentOutcome outcome = intent.recordGatewayPayment(payload.amount(), Instant.now());
            paymentIntentRepository.save(intent);

            if (outcome == PaymentIntent.PaymentOutcome.IDEMPOTENT_ALREADY_PAID) {
                logWebhook(payload.paymentCode(), sourceIp, true, "IDEMPOTENT_IGNORED", null, rawBody);
                return true;
            }
            if (outcome == PaymentIntent.PaymentOutcome.LATE_EXPIRED || outcome == PaymentIntent.PaymentOutcome.LATE_AFTER_CANCEL) {
                logWebhook(payload.paymentCode(), sourceIp, true, outcome.name(), "Late or cancelled intent", rawBody);
                return false;
            }

            postLedgerForPayment(intent, payload.amount());
            publishPaymentEvent(intent, outcome);
            logWebhook(payload.paymentCode(), sourceIp, true, outcome.name(), null, rawBody);
            return true;
        } catch (IllegalArgumentException | SecurityException e) {
            logWebhook(null, sourceIp, true, "REJECTED", e.getMessage(), rawBody);
            throw e;
        } catch (Exception e) {
            logWebhook(null, sourceIp, true, "ERROR", e.getMessage(), rawBody);
            throw new RuntimeException("Xử lý webhook thanh toán thất bại", e);
        }
    }

    private void postLedgerForPayment(PaymentIntent intent, long paidChunk) {
        ledgerService.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Tài khoản tiền về cổng thanh toán", AccountType.ASSET, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Tài khoản cọc ký quỹ Escrow", AccountType.LIABILITY, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_REVENUE, "Doanh thu phí dịch vụ sàn Tổ Ấm", AccountType.REVENUE, null);

        String idemKey = "PAYWH-" + intent.getPaymentCode() + "-" + intent.getPaidAmount();
        if ("DEPOSIT".equalsIgnoreCase(intent.getOrderType())) {
            ledgerService.postEntryIdempotent(idemKey, "Thu tiền cọc hợp đồng " + intent.getOrderId(),
                    "DEPOSIT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                    List.of(
                            new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                            new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, paidChunk)));
        } else if ("INVOICE".equalsIgnoreCase(intent.getOrderType())) {
            Invoice invoice = invoiceRepository.findById(intent.getOrderId()).orElse(null);
            String landlordId = invoice != null ? invoice.getLandlordId() : "landlord-system";
            ledgerService.getOrCreateWallet(landlordId, "Chủ trọ");
            long baseRent = invoice != null ? invoice.getBaseRent() : 0L;
            long platformFee = (baseRent > 0 && paidChunk >= baseRent) ? (baseRent * 5 / 100) : (paidChunk * 5 / 100);
            long landlordAmount = paidChunk - platformFee;
            ledgerService.postEntryIdempotent(idemKey, "Thanh toán hóa đơn phòng " + intent.getOrderId(),
                    "INVOICE_PAYMENT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                    List.of(
                            new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                            new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(landlordId), PostingDirection.CREDIT, landlordAmount),
                            new LedgerDto.PostingItem(LedgerService.ACC_REVENUE, PostingDirection.CREDIT, platformFee)));
            if (invoice != null) {
                invoice.recordPayment(paidChunk);
                invoiceRepository.save(invoice);
            }
        }
    }

    private boolean isValidSignature(String signedData, String providedSig) {
        if (providedSig == null || providedSig.isBlank()) return false;
        if (constantTimeMatches(computeHmacSha256(signedData, webhookSecret), providedSig)) return true;
        return previousWebhookSecret != null && !previousWebhookSecret.isBlank()
                && constantTimeMatches(computeHmacSha256(signedData, previousWebhookSecret), providedSig);
    }

    private boolean constantTimeMatches(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi tính HMAC-SHA256", e);
        }
    }

    private void publishPaymentEvent(PaymentIntent intent, PaymentIntent.PaymentOutcome outcome) {
        if (outboxRepository == null) {
            return;
        }
        String eventType = "PAYMENT_" + outcome.name();
        String payload = String.format(
                "{\"paymentCode\":\"%s\",\"orderType\":\"%s\",\"orderId\":\"%s\",\"tenantId\":\"%s\",\"paidAmount\":%d,\"status\":\"%s\",\"message\":\"Thanh toan %s trang thai %s\"}",
                intent.getPaymentCode(), intent.getOrderType(), intent.getOrderId(),
                intent.getPayerId(), intent.getPaidAmount(), intent.getStatus(),
                intent.getPaymentCode(), intent.getStatus()
        );
        outboxRepository.save(BillingEventOutbox.create(eventType, "PAYMENT", intent.getId(), payload));
    }

    private void logWebhook(String paymentCode, String ip, boolean sigValid, String status, String err, String raw) {
        if (webhookLogRepository == null) return;
        webhookLogRepository.save(new PaymentWebhookLog(null, paymentCode, ip, sigValid, status, err, raw, Instant.now()));
    }

    private Set<String> parseIpList(String raw) {
        Set<String> set = new HashSet<>();
        if (raw != null) {
            for (String s : raw.split(",")) {
                if (!s.isBlank()) set.add(s.trim());
            }
        }
        return set;
    }

    private PaymentDto.IntentResponse toResponse(PaymentIntent i) {
        return new PaymentDto.IntentResponse(i.getId(), i.getPaymentCode(), i.getOrderType(),
                i.getOrderId(), i.getAmount(), i.getPaidAmount(), i.getQrUrl(), i.getStatus(), i.getExpiresAt());
    }
}
