package vn.cozynest.admin.application.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.admin.application.port.AdminBillingEscrowPort;
import vn.cozynest.admin.application.port.DisputePersistencePort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DisputeService {

    private final DisputePersistencePort persistencePort;
    private final Optional<AdminBillingEscrowPort> billingEscrowPort;
    private final Optional<AdminAuditService> auditService;

    public DisputeService(
            DisputePersistencePort persistencePort,
            Optional<AdminBillingEscrowPort> billingEscrowPort,
            Optional<AdminAuditService> auditService
    ) {
        this.persistencePort = persistencePort;
        this.billingEscrowPort = billingEscrowPort != null ? billingEscrowPort : Optional.empty();
        this.auditService = auditService != null ? auditService : Optional.empty();
    }

    public record CreateDisputeCommand(
            String contractId,
            String roomTitle,
            String defendantId,
            String defendantName,
            String defendantRole,
            String complainantName,
            String complainantRole,
            String disputeType,
            long amount,
            String reason,
            List<String> evidenceUrls
    ) {}

    public record ResolveDisputeCommand(
            String resolutionAction,
            String verdict
    ) {}

    @Transactional(readOnly = true)
    public List<DisputePersistencePort.DisputeDetailView> listDisputes(String actorId, boolean isStaff, String status, String search) {
        if (isStaff) {
            return persistencePort.findAllViews(status, search);
        }
        return persistencePort.findByParticipantViews(actorId);
    }

    @Transactional(readOnly = true)
    public DisputePersistencePort.DisputeDetailView getDetail(String id, String actorId, boolean isStaff) {
        DisputePersistencePort.DisputeDetailView view = persistencePort.findViewById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so tranh chap: " + id));
        if (!isStaff && !actorId.equals(view.complainantId()) && !actorId.equals(view.defendantId())) {
            throw new AccessDeniedException("Khong co quyen truy cap ho so tranh chap nay");
        }
        return view;
    }

    @Transactional
    public DisputePersistencePort.DisputeDetailView createDispute(String actorId, String actorRole, CreateDisputeCommand cmd, String ip, String ua) {
        if (cmd.contractId() == null || cmd.contractId().isBlank()) {
            throw new IllegalArgumentException("Ma hop dong khong duoc de trong");
        }
        if (cmd.reason() == null || cmd.reason().isBlank()) {
            throw new IllegalArgumentException("Ly do khieu nai tranh chap khong duoc de trong");
        }
        String id = UUID.randomUUID().toString();
        String code = "DISP-" + (System.currentTimeMillis() % 1_000_000L);
        DisputePersistencePort.DisputeDetailView view = new DisputePersistencePort.DisputeDetailView(
                id,
                code,
                cmd.contractId().trim(),
                cmd.roomTitle() != null && !cmd.roomTitle().isBlank() ? cmd.roomTitle().trim() : "Hợp đồng " + cmd.contractId(),
                actorId,
                cmd.complainantName() != null && !cmd.complainantName().isBlank() ? cmd.complainantName().trim() : actorId,
                cmd.complainantRole() != null && !cmd.complainantRole().isBlank() ? cmd.complainantRole().trim() : actorRole,
                cmd.defendantId() != null && !cmd.defendantId().isBlank() ? cmd.defendantId().trim() : "UNKNOWN",
                cmd.defendantName() != null && !cmd.defendantName().isBlank() ? cmd.defendantName().trim() : cmd.defendantId(),
                cmd.defendantRole() != null && !cmd.defendantRole().isBlank() ? cmd.defendantRole().trim() : "LANDLORD",
                cmd.disputeType() != null && !cmd.disputeType().isBlank() ? cmd.disputeType().trim() : "DEPOSIT_RETURN",
                Math.max(0L, cmd.amount()),
                cmd.reason().trim(),
                cmd.evidenceUrls() != null ? cmd.evidenceUrls() : List.of(),
                "PENDING",
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        persistencePort.insertDispute(view);
        persistencePort.addEvent(id, actorId, "CREATED", "Khởi tạo hồ sơ tranh chấp " + code + ": " + cmd.reason().trim());
        auditService.ifPresent(a -> a.record(actorId, actorRole, "DISPUTE_CREATE", "DISPUTE", code, ip, ua,
                "{\"contractId\":\"" + cmd.contractId() + "\",\"amount\":" + cmd.amount() + "}"));
        return persistencePort.findViewById(id).orElse(view);
    }

    @Transactional
    public DisputePersistencePort.DisputeDetailView addComment(String id, String actorId, String actorRole, boolean isStaff, String message, String ip, String ua) {
        DisputePersistencePort.DisputeDetailView view = getDetail(id, actorId, isStaff);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Noi dung phan hoi khong duoc de trong");
        }
        persistencePort.addEvent(view.id(), actorId, "COMMENT", message.trim());
        auditService.ifPresent(a -> a.record(actorId, actorRole, "DISPUTE_COMMENT", "DISPUTE", view.disputeCode(), ip, ua, null));
        return persistencePort.findViewById(view.id()).orElse(view);
    }

    @Transactional
    public DisputePersistencePort.DisputeDetailView mediate(String id, String mediatorId, String note, boolean freezeEscrow, String ip, String ua) {
        DisputePersistencePort.DisputeDetailView view = persistencePort.findViewById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so tranh chap: " + id));
        if ("RESOLVED".equals(view.status()) || "REJECTED".equals(view.status())) {
            throw new IllegalStateException("Ho so tranh chap da dong, khong the tiep nhan hoa giai");
        }
        if (freezeEscrow) {
            billingEscrowPort.ifPresent(port -> port.freezeEscrow(
                    view.contractId(), mediatorId,
                    "Đóng băng ký quỹ theo tranh chấp " + view.disputeCode()));
        }
        persistencePort.updateDisputeState(view.id(), "MEDIATING", mediatorId, null, false);
        String msg = (note != null && !note.isBlank() ? note.trim() : "Quản trị viên đã tiếp nhận hồ sơ hòa giải")
                + (freezeEscrow ? " (Đã kích hoạt đóng băng tiền cọc Escrow)" : "");
        persistencePort.addEvent(view.id(), mediatorId, "STATUS_CHANGED", msg);
        auditService.ifPresent(a -> a.record(mediatorId, "ADMIN", "DISPUTE_MEDIATE", "DISPUTE", view.disputeCode(), ip, ua,
                "{\"freezeEscrow\":" + freezeEscrow + ",\"contractId\":\"" + view.contractId() + "\"}"));
        return persistencePort.findViewById(view.id()).orElse(view);
    }

    @Transactional
    public DisputePersistencePort.DisputeDetailView resolve(String id, String adminId, ResolveDisputeCommand cmd, String ip, String ua) {
        DisputePersistencePort.DisputeDetailView view = persistencePort.findViewById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so tranh chap: " + id));
        if ("RESOLVED".equals(view.status()) || "REJECTED".equals(view.status())) {
            throw new IllegalStateException("Ho so tranh chap da duoc giai quyet truoc do");
        }
        if (cmd.verdict() == null || cmd.verdict().isBlank()) {
            throw new IllegalArgumentException("Vui long nhap phan quyet cuoi cung cho tranh chap");
        }
        String action = cmd.resolutionAction() != null ? cmd.resolutionAction().trim().toUpperCase() : "NO_ESCROW_ACTION";
        if ("REFUND_TENANT".equals(action)) {
            billingEscrowPort.ifPresent(port -> port.refundEscrow(
                    view.contractId(), adminId,
                    "Hoàn cọc theo phán quyết tranh chấp " + view.disputeCode() + ": " + cmd.verdict().trim()));
        } else if ("RELEASE_LANDLORD".equals(action)) {
            billingEscrowPort.ifPresent(port -> port.releaseEscrow(view.contractId(), adminId));
        }

        String finalStatus = "REJECT".equals(action) ? "REJECTED" : "RESOLVED";
        persistencePort.updateDisputeState(view.id(), finalStatus, adminId, cmd.verdict().trim(), true);
        persistencePort.addEvent(view.id(), adminId, finalStatus,
                "Phán quyết (" + action + "): " + cmd.verdict().trim());
        auditService.ifPresent(a -> a.record(adminId, "ADMIN", "DISPUTE_" + finalStatus, "DISPUTE", view.disputeCode(), ip, ua,
                "{\"resolutionAction\":\"" + action + "\",\"contractId\":\"" + view.contractId() + "\"}"));
        return persistencePort.findViewById(view.id()).orElse(view);
    }
}
