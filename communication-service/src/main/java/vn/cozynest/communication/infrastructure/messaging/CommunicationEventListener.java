package vn.cozynest.communication.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.cozynest.common.events.DeadLetterEnvelope;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventInboxProcessor;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.communication.application.service.CommunicationEmailService;
import vn.cozynest.communication.application.service.CommunicationLifecycleEmailHelper;
import vn.cozynest.communication.application.service.NotificationApplicationService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CommunicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(CommunicationEventListener.class);
    private static final int MAX_RETRIES = 3;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CommunicationInboxStore inboxStore;
    private final NotificationApplicationService notificationService;
    private final Optional<RabbitTemplate> rabbitTemplate;
    private final Optional<CommunicationEmailService> emailService;
    private final Optional<CommunicationLifecycleEmailHelper> lifecycleEmailHelper;

    public CommunicationEventListener(
            CommunicationInboxStore inboxStore,
            NotificationApplicationService notificationService,
            Optional<RabbitTemplate> rabbitTemplate
    ) {
        this(inboxStore, notificationService, rabbitTemplate, Optional.empty(), Optional.empty());
    }

    public CommunicationEventListener(
            CommunicationInboxStore inboxStore,
            NotificationApplicationService notificationService,
            Optional<RabbitTemplate> rabbitTemplate,
            Optional<CommunicationEmailService> emailService
    ) {
        this(inboxStore, notificationService, rabbitTemplate, emailService, Optional.empty());
    }

    @Autowired
    public CommunicationEventListener(
            CommunicationInboxStore inboxStore,
            NotificationApplicationService notificationService,
            Optional<RabbitTemplate> rabbitTemplate,
            Optional<CommunicationEmailService> emailService,
            Optional<CommunicationLifecycleEmailHelper> lifecycleEmailHelper
    ) {
        this.inboxStore = inboxStore;
        this.notificationService = notificationService;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
        this.emailService = emailService != null ? emailService : Optional.empty();
        this.lifecycleEmailHelper = lifecycleEmailHelper != null ? lifecycleEmailHelper : Optional.empty();
    }

    @RabbitListener(queues = EventTopology.COMMUNICATION_QUEUE)
    @Transactional
    public void onMessage(String rawJson) {
        handleRawMessage(rawJson);
    }

    @Transactional
    public EventInboxProcessor.ProcessOutcome handleRawMessage(String rawJson) {
        DomainEventEnvelope envelope;
        try {
            envelope = EventEnvelopeCodec.decode(rawJson);
        } catch (Exception ex) {
            log.warn("Poison event payload could not be parsed; quarantining to DLQ: {}", ex.getMessage());
            publishPoisonToDlq(rawJson, "MALFORMED_ENVELOPE: " + ex.getMessage());
            return EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ;
        }

        if (envelope.schemaVersion() > EventTopology.SUPPORTED_MAX_SCHEMA_VERSION) {
            inboxStore.recordDeadLetter(envelope, "UNSUPPORTED_SCHEMA_VERSION_" + envelope.schemaVersion());
            return EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ;
        }

        return processWithBoundedRetry(envelope);
    }

    private EventInboxProcessor.ProcessOutcome processWithBoundedRetry(DomainEventEnvelope envelope) {
        EventInboxProcessor.ProcessOutcome lastOutcome = EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ;
        long backoffMs = 500L;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            lastOutcome = EventInboxProcessor.processIdempotently(inboxStore, envelope, () -> {
                dispatchNotificationFromEnvelope(envelope);
                return null;
            });
            if (lastOutcome != EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ) {
                return lastOutcome;
            }
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoffMs *= 2;
            }
        }
        return lastOutcome;
    }

    private void dispatchNotificationFromEnvelope(DomainEventEnvelope envelope) {
        JsonNode payload = MAPPER.readTree(envelope.payloadJson() != null ? envelope.payloadJson() : "{}");
        String recipientId = payload.path("recipientId").asString("");
        String userId = payload.path("userId").asString("");
        String tenantId = payload.path("tenantId").asString("");
        String landlordId = payload.path("landlordId").asString("");

        String title = resolveTitle(envelope.eventType());
        String content = resolveContent(envelope, payload);
        String linkUrl = resolveLinkUrl(envelope, payload);

        if (!recipientId.isBlank()) {
            notificationService.createInternalNotification(recipientId, envelope.aggregateType(), title, content, linkUrl);
            triggerEmailNotificationIfApplicable(envelope, payload);
            return;
        }
        if (!userId.isBlank()) {
            notificationService.createInternalNotification(userId, envelope.aggregateType(), title, content, linkUrl);
            triggerEmailNotificationIfApplicable(envelope, payload);
            return;
        }

        boolean notified = false;
        if (!tenantId.isBlank()) {
            notificationService.createInternalNotification(tenantId, envelope.aggregateType(), title, content, linkUrl);
            notified = true;
        }
        if (!landlordId.isBlank() && !landlordId.equals(tenantId)) {
            notificationService.createInternalNotification(landlordId, envelope.aggregateType(), title, content, linkUrl);
            notified = true;
        }

        if (!notified) {
            throw new IllegalArgumentException("Missing notification recipient userId in event " + envelope.eventType());
        }

        triggerEmailNotificationIfApplicable(envelope, payload);
    }

    private void triggerEmailNotificationIfApplicable(DomainEventEnvelope envelope, JsonNode payload) {
        if (emailService.isEmpty()) {
            return;
        }
        CommunicationEmailService svc = emailService.get();
        String evt = envelope.eventType();
        if (evt == null) return;

        try {
            switch (evt) {
                case "APPOINTMENT_CREATED" -> {
                    CommunicationEmailService.AppointmentEmailPayload apptPayload = new CommunicationEmailService.AppointmentEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("roomTitle").asString(""),
                            payload.path("roomAddress").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("contactName").asString(""),
                            payload.path("contactPhone").asString(""),
                            payload.path("contactEmail").asString(""),
                            payload.path("tenantNote").asString(""),
                            null,
                            payload.path("scheduledStart").asString(""),
                            payload.path("scheduledEnd").asString(""),
                            null
                    );
                    svc.sendAppointmentCreatedNotification(apptPayload);
                }
                case "APPOINTMENT_CONFIRMED" -> {
                    CommunicationEmailService.AppointmentEmailPayload apptPayload = new CommunicationEmailService.AppointmentEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("roomTitle").asString(""),
                            payload.path("roomAddress").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("contactName").asString(""),
                            payload.path("contactPhone").asString(""),
                            payload.path("contactEmail").asString(""),
                            payload.path("tenantNote").asString(""),
                            payload.path("landlordNote").asString(""),
                            payload.path("scheduledStart").asString(""),
                            payload.path("scheduledEnd").asString(""),
                            null
                    );
                    svc.sendAppointmentConfirmedNotification(apptPayload);
                }
                case "APPOINTMENT_DECLINED" -> {
                    CommunicationEmailService.AppointmentEmailPayload apptPayload = new CommunicationEmailService.AppointmentEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("roomTitle").asString(""),
                            payload.path("roomAddress").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("contactName").asString(""),
                            payload.path("contactPhone").asString(""),
                            payload.path("contactEmail").asString(""),
                            null,
                            null,
                            payload.path("scheduledStart").asString(""),
                            null,
                            payload.path("reasonNote").asString(payload.path("reasonCode").asString(""))
                    );
                    svc.sendAppointmentDeclinedNotification(apptPayload);
                }
                case "APPOINTMENT_CANCELLED" -> {
                    String cancelledBy = payload.path("cancelledBy").asString("");
                    CommunicationEmailService.AppointmentEmailPayload apptPayload = new CommunicationEmailService.AppointmentEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("roomTitle").asString(""),
                            payload.path("roomAddress").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("contactName").asString(""),
                            payload.path("contactPhone").asString(""),
                            payload.path("contactEmail").asString(""),
                            null,
                            null,
                            payload.path("scheduledStart").asString(""),
                            null,
                            payload.path("reasonNote").asString(payload.path("reasonCode").asString(""))
                    );
                    if ("TENANT".equalsIgnoreCase(cancelledBy)) {
                        svc.sendAppointmentCancelledByTenantNotification(apptPayload);
                    } else {
                        svc.sendAppointmentDeclinedNotification(apptPayload);
                    }
                }
                case "RENTAL_REQUEST_CREATED" -> {
                    CommunicationEmailService.RentalRequestEmailPayload rentalPayload = new CommunicationEmailService.RentalRequestEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("proposedMoveInDate").asString(""),
                            payload.path("numOccupants").asInt(1),
                            payload.path("message").asString(""),
                            null,
                            payload.path("roomTitle").asString("")
                    );
                    svc.sendRentalRequestCreatedNotification(rentalPayload);
                }
                case "RENTAL_REQUEST_APPROVED" -> {
                    CommunicationEmailService.RentalRequestEmailPayload rentalPayload = new CommunicationEmailService.RentalRequestEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("proposedMoveInDate").asString(""),
                            payload.path("numOccupants").asInt(1),
                            payload.path("message").asString(""),
                            payload.path("contractId").asString(""),
                            payload.path("roomTitle").asString("")
                    );
                    svc.sendRentalRequestApprovedNotification(rentalPayload);
                }
                case "RENTAL_REQUEST_REJECTED" -> {
                    CommunicationEmailService.RentalRequestEmailPayload rentalPayload = new CommunicationEmailService.RentalRequestEmailPayload(
                            envelope.aggregateId(),
                            payload.path("roomId").asString(""),
                            payload.path("tenantId").asString(""),
                            payload.path("landlordId").asString(""),
                            payload.path("proposedMoveInDate").asString(""),
                            payload.path("numOccupants").asInt(1),
                            payload.path("message").asString(""),
                            null,
                            payload.path("roomTitle").asString("")
                    );
                    svc.sendRentalRequestRejectedNotification(rentalPayload);
                }
                case "APPOINTMENT_COMPLETED" -> lifecycleEmailHelper.ifPresent(h ->
                        h.sendAppointmentOutcomeEmail(payload.path("tenantId").asString(""), payload.path("roomTitle").asString(""), true));
                case "APPOINTMENT_NO_SHOW" -> lifecycleEmailHelper.ifPresent(h ->
                        h.sendAppointmentOutcomeEmail(payload.path("tenantId").asString(""), payload.path("roomTitle").asString(""), false));
                case "CONTRACT_CREATED" -> lifecycleEmailHelper.ifPresent(h ->
                        h.sendContractNeedsSignatureEmail(payload.path("tenantId").asString(""), payload.path("contractCode").asString("")));
                case "CONTRACT_ACTIVATED" -> lifecycleEmailHelper.ifPresent(h ->
                        h.sendContractActivatedEmail(
                                payload.path("tenantId").asString(""),
                                payload.path("landlordId").asString(""),
                                payload.path("contractCode").asString(""),
                                payload.path("orderCode").asString("")
                        ));
                case "PAYMENT_PAID" -> lifecycleEmailHelper.ifPresent(h ->
                        h.sendPaymentPaidEmail(
                                payload.path("tenantId").asString(""),
                                payload.path("paymentCode").asString(""),
                                payload.path("paidAmount").asLong(0L),
                                true
                        ));
                case "INVOICE_GENERATED" -> lifecycleEmailHelper.ifPresent(h ->
                        h.sendInvoiceGeneratedEmail(
                                payload.path("tenantId").asString(""),
                                payload.path("invoiceCode").asString(""),
                                payload.path("totalAmount").asLong(0L)
                        ));
                default -> {}
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch email for event {}: {}", evt, ex.getMessage());
        }
    }

    private void publishPoisonToDlq(String rawPayload, String reason) {
        if (rabbitTemplate.isEmpty()) {
            return;
        }
        DomainEventEnvelope synthetic = new DomainEventEnvelope(
                UUID.randomUUID().toString(),
                "UNKNOWN_POISON_EVENT",
                1,
                "UNKNOWN",
                "UNKNOWN",
                "POISON",
                Instant.now(),
                EventTopology.sanitizePiiForDlq(rawPayload)
        );
        DeadLetterEnvelope dlq = new DeadLetterEnvelope(synthetic, "communication-service", reason, MAX_RETRIES);
        rabbitTemplate.get().convertAndSend(
                EventTopology.DLX_EXCHANGE,
                EventTopology.DLQ_ROUTING_KEY,
                EventEnvelopeCodec.encodeDeadLetter(dlq)
        );
    }

    private String resolveTitle(String eventType) {
        if (eventType == null) return "Thông báo hệ thống Tổ Ấm";
        return switch (eventType) {
            case "APPOINTMENT_CREATED" -> "Có khách hẹn xem phòng mới";
            case "APPOINTMENT_CONFIRMED" -> "Lịch hẹn xem phòng đã được xác nhận";
            case "APPOINTMENT_COMPLETED" -> "Lịch hẹn xem phòng đã hoàn thành";
            case "APPOINTMENT_NO_SHOW" -> "Khách không đến theo lịch hẹn xem phòng";
            case "APPOINTMENT_DECLINED" -> "Lịch hẹn xem phòng bị từ chối";
            case "APPOINTMENT_CANCELLED" -> "Lịch hẹn xem phòng đã bị hủy";
            case "APPOINTMENT_EXPIRED" -> "Lịch hẹn xem phòng đã hết hạn";
            case "RENTAL_REQUEST_CREATED" -> "Yêu cầu thuê phòng mới";
            case "RENTAL_REQUEST_APPROVED" -> "Yêu cầu thuê phòng đã được chấp nhận";
            case "RENTAL_REQUEST_REJECTED" -> "Yêu cầu thuê phòng bị từ chối";
            case "CONTRACT_CREATED" -> "Hợp đồng thuê phòng mới đã được tạo";
            case "CONTRACT_SIGNED" -> "Hợp đồng thuê phòng đã được ký kết";
            case "CONTRACT_ACTIVATED" -> "Hợp đồng thuê phòng đã có hiệu lực";
            case "CONTRACT_DRAFT_UPDATED" -> "Dự thảo hợp đồng thuê phòng đã được cập nhật";
            case "CONTRACT_EXPIRY_REMINDER" -> "Nhắc nhở hợp đồng thuê phòng sắp hết hạn";
            case "CONTRACT_PHYSICAL_SCAN_UPLOADED" -> "Bản quét hợp đồng thuê phòng đã được tải lên";
            case "CONTRACT_PHYSICAL_SCAN_REJECTED" -> "Bản quét hợp đồng thuê phòng bị từ chối";
            case "CONTRACT_AMENDMENT_APPLIED" -> "Phụ lục hợp đồng thuê phòng đã được áp dụng";
            case "PAYMENT_CONFIRMED", "PAYMENT_PAID" -> "Xác nhận thanh toán thành công";
            case "ESCROW_RELEASED" -> "Tiền đặt cọc đã được giải ngân";
            case "INVOICE_GENERATED", "INVOICE_ISSUED" -> "Hóa đơn thuê phòng kỳ mới đã phát hành";
            case "INVOICE_PAYMENT_RECORDED" -> "Thanh toán hóa đơn phòng đã được ghi nhận";
            case "INVOICE_LATE_FEE_APPLIED" -> "Áp dụng phí phạt chậm thanh toán hóa đơn";
            case "INVOICE_OVERDUE_REMINDER" -> "Nhắc nhở thanh toán hóa đơn quá hạn";
            case "KYC_SUBMITTED", "KycSubmitted" -> "Hồ sơ xác thực eKYC đã được gửi";
            case "KYC_APPROVED", "TenantKycApproved", "TENANT_KYC_APPROVED" -> "Hồ sơ xác thực eKYC đã được phê duyệt";
            case "LandlordApproved", "LANDLORD_APPROVED" -> "Chúc mừng! Bạn đã trở thành Chủ trọ";
            case "KYC_REJECTED", "KycRejected" -> "Hồ sơ xác thực eKYC cần bổ sung hoặc bị từ chối";
            case "FACE_VERIFIED", "FaceVerificationPassed", "FACE_VERIFICATION_PASSED" -> "Xác thực sinh trắc học khuôn mặt thành công";
            case "FACE_FAILED", "FaceVerificationFailed", "FACE_VERIFICATION_FAILED" -> "Xác thực sinh trắc học khuôn mặt thất bại";
            case "LISTING_PUBLISHED" -> "Tin đăng phòng trọ đã được phát hành";
            case "LISTING_UPDATED" -> "Tin đăng phòng trọ đã được cập nhật";
            case "LISTING_CLOSED" -> "Tin đăng phòng trọ đã được đóng";
            default -> "Thông báo từ hệ thống Tổ Ấm";
        };
    }

    private String resolveContent(DomainEventEnvelope envelope, JsonNode payload) {
        String rejectionReason = payload.path("rejectionReason").asString("");
        if ("RENTAL_REQUEST_REJECTED".equals(envelope.eventType()) && !rejectionReason.isBlank()) {
            String roomTitle = payload.path("roomTitle").asString("");
            return (!roomTitle.isBlank() ? roomTitle + " - " : "") + "Lý do: " + rejectionReason;
        }
        if ("APPOINTMENT_COMPLETED".equals(envelope.eventType())) {
            String roomTitle = payload.path("roomTitle").asString("");
            return !roomTitle.isBlank()
                    ? "Buổi xem phòng " + roomTitle + " đã hoàn thành. Bạn có thể gửi yêu cầu thuê phòng."
                    : "Buổi xem phòng đã hoàn thành. Bạn có thể gửi yêu cầu thuê phòng.";
        }
        if ("APPOINTMENT_NO_SHOW".equals(envelope.eventType())) {
            String roomTitle = payload.path("roomTitle").asString("");
            return !roomTitle.isBlank()
                    ? "Lịch hẹn xem phòng " + roomTitle + " được ghi nhận khách không đến."
                    : "Lịch hẹn xem phòng được ghi nhận khách không đến.";
        }
        String msg = payload.path("message").asString("");
        if (!msg.isBlank()) {
            return msg;
        }
        String contractCode = payload.path("contractCode").asString("");
        if (!contractCode.isBlank()) {
            return "Mã hợp đồng: " + contractCode;
        }
        String invoiceCode = payload.path("invoiceCode").asString("");
        if (!invoiceCode.isBlank()) {
            return "Mã hóa đơn: " + invoiceCode;
        }
        String roomTitle = payload.path("roomTitle").asString("");
        if (!roomTitle.isBlank()) {
            return "Phòng: " + roomTitle;
        }
        String agg = envelope.aggregateType() != null ? envelope.aggregateType().toUpperCase() : "";
        return switch (agg) {
            case "APPOINTMENT" -> "Lịch hẹn xem phòng của bạn đã có cập nhật trạng thái mới.";
            case "RENTAL_REQUEST" -> "Yêu cầu thuê phòng của bạn đã có phản hồi mới từ hệ thống.";
            case "CONTRACT" -> "Hợp đồng thuê phòng của bạn đã có cập nhật mới.";
            case "INVOICE" -> "Hóa đơn thanh toán của bạn đã có cập nhật mới.";
            case "KYC", "KYC_VERIFICATION" -> "Hồ sơ định danh eKYC của bạn đã có kết quả kiểm duyệt.";
            case "FACE_VERIFICATION" -> "Kết quả xác thực sinh trắc học khuôn mặt đã được cập nhật.";
            default -> "Bạn có thông báo mới từ hệ thống Tổ Ấm.";
        };
    }

    private String resolveLinkUrl(DomainEventEnvelope envelope, JsonNode payload) {
        String link = payload.path("linkUrl").asString("");
        if (!link.isBlank()) {
            return link;
        }
        String agg = envelope.aggregateType() != null ? envelope.aggregateType().toUpperCase() : "";
        return switch (agg) {
            case "CONTRACT" -> "/dashboard/contracts";
            case "INVOICE" -> "/dashboard/invoices";
            case "APPOINTMENT" -> "/dashboard/appointments";
            case "RENTAL_REQUEST" -> "/dashboard/rental-requests";
            default -> "/dashboard";
        };
    }

    private String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return "";
    }
}

