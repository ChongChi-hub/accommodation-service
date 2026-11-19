package vn.cozynest.booking.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.ContractCheckinInfoDto;
import vn.cozynest.booking.application.dto.ContractDto;
import vn.cozynest.booking.application.exception.ContractNotFoundException;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.OrderCodeGenerator;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;

import java.util.Optional;

/**
 * Handles idempotent contract activation after deposit payment, listing room occupancy sync
 * (with retry outbox on failure), saga completion, and phased PII disclosure for check-in.
 */
@Service
@Transactional
public class ContractActivationService {

    private static final Logger log = LoggerFactory.getLogger(ContractActivationService.class);

    private final ContractRepository contractRepository;
    private final AppointmentEventOutboxRepository outboxRepository;
    private final Optional<BookingListingPort> listingPort;
    private final Optional<RentalSagaOrchestrator> sagaOrchestrator;

    @Autowired
    public ContractActivationService(
            ContractRepository contractRepository,
            AppointmentEventOutboxRepository outboxRepository,
            Optional<BookingListingPort> listingPort,
            Optional<RentalSagaOrchestrator> sagaOrchestrator
    ) {
        this.contractRepository = contractRepository;
        this.outboxRepository = outboxRepository;
        this.listingPort = listingPort != null ? listingPort : Optional.empty();
        this.sagaOrchestrator = sagaOrchestrator != null ? sagaOrchestrator : Optional.empty();
    }

    public ContractDto activateAfterDeposit(String contractId, String paymentCode, long paidAmount) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));

        if ("ACTIVE".equals(contract.getStatus()) && "SYNCED".equals(contract.getListingSyncStatus())) {
            return ContractDto.from(contract);
        }

        BookingListingPort.RoomCheckinDetails roomDetails = listingPort
                .map(p -> p.getRoomCheckinDetails(contract.getRoomId()))
                .orElse(new BookingListingPort.RoomCheckinDetails("Phong tro CozyNest", "", "", "", "", ""));

        String checkinCode = (contract.getCheckinCode() != null && !contract.getCheckinCode().isBlank())
                ? contract.getCheckinCode()
                : OrderCodeGenerator.generateCheckinCode();

        boolean wasAlreadyActive = "ACTIVE".equals(contract.getStatus());
        contract.activateAfterDeposit(
                checkinCode,
                roomDetails.fullAddress(),
                roomDetails.landlordName(),
                roomDetails.landlordPhone(),
                null
        );
        String instructions = buildCheckinInstructions(contract, roomDetails, checkinCode);
        contract.activateAfterDeposit(
                checkinCode,
                roomDetails.fullAddress(),
                roomDetails.landlordName(),
                roomDetails.landlordPhone(),
                instructions
        );

        boolean listingSynced = listingPort
                .map(p -> p.markRoomRented(contract.getRoomId(), contract.getId()))
                .orElse(true);

        if (listingSynced) {
            contract.markListingSynced();
        } else {
            contract.markListingSyncFailed();
            emitEvent("CONTRACT_LISTING_SYNC_RETRY", contract, paymentCode);
            log.warn("Listing sync failed for contractId={}, roomId={} — queued CONTRACT_LISTING_SYNC_RETRY",
                    contract.getId(), contract.getRoomId());
        }

        Contract saved = contractRepository.save(contract);

        if (!wasAlreadyActive) {
            emitEvent("CONTRACT_ACTIVATED", saved, paymentCode);
        }

        advanceSagaSafely(saved, paymentCode, paidAmount);
        return ContractDto.from(saved);
    }

    @Transactional(readOnly = true)
    public ContractCheckinInfoDto getCheckinInfo(String contractId, String actorId, boolean isAdmin) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));

        boolean isParticipant = isAdmin
                || contract.getTenantId().equals(actorId)
                || LandlordIdentityResolver.matches(contract.getLandlordId(), actorId);
        if (!isParticipant) {
            throw new ContractNotFoundException(contractId);
        }

        BookingListingPort.RoomCheckinDetails live = listingPort
                .map(p -> p.getRoomCheckinDetails(contract.getRoomId()))
                .orElse(new BookingListingPort.RoomCheckinDetails("Phong tro CozyNest", "", "", "", "", ""));

        boolean active = "ACTIVE".equals(contract.getStatus());
        if (!active) {
            return new ContractCheckinInfoDto(
                    contract.getId(),
                    contract.getContractCode(),
                    null,
                    contract.getRoomId(),
                    contract.getStatus(),
                    false,
                    live.areaHint(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    contract.getStartDate(),
                    contract.getEndDate(),
                    contract.getMonthlyRent(),
                    contract.getDepositAmount(),
                    null,
                    contract.getPdfUrl()
            );
        }

        String fullAddress = firstNonBlank(contract.getRoomFullAddress(), live.fullAddress());
        String landlordName = firstNonBlank(contract.getLandlordName(), live.landlordName());
        String landlordPhone = firstNonBlank(contract.getLandlordPhone(), live.landlordPhone());
        String checkinCode = firstNonBlank(contract.getCheckinCode(), contract.getVerificationCode());
        String instructions = firstNonBlank(
                contract.getCheckinInstructions(),
                buildCheckinInstructions(contract, live, checkinCode)
        );

        return new ContractCheckinInfoDto(
                contract.getId(),
                contract.getContractCode(),
                contract.getOrderCode(),
                contract.getRoomId(),
                contract.getStatus(),
                true,
                live.areaHint(),
                fullAddress,
                landlordName,
                landlordPhone,
                checkinCode,
                instructions,
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getDepositAmount(),
                contract.getDepositPaidAt(),
                contract.getPdfUrl()
        );
    }

    private void advanceSagaSafely(Contract contract, String paymentCode, long paidAmount) {
        if (sagaOrchestrator.isEmpty() || contract.getRequestId() == null || contract.getRequestId().isBlank()) {
            return;
        }
        String sagaId = "saga-" + contract.getRequestId();
        try {
            String payload = String.format(
                    "{\"contractId\":\"%s\",\"requestId\":\"%s\",\"roomId\":\"%s\",\"paymentCode\":\"%s\",\"paidAmount\":%d}",
                    contract.getId(), contract.getRequestId(), contract.getRoomId(),
                    paymentCode != null ? paymentCode : "", paidAmount
            );
            sagaOrchestrator.get().advanceToDepositPaid(sagaId, payload);
            sagaOrchestrator.get().completeSaga(sagaId);
        } catch (Exception ex) {
            log.debug("Saga advance skipped for sagaId={}: {}", sagaId, ex.getMessage());
        }
    }

    private void emitEvent(String eventType, Contract contract, String paymentCode) {
        String payload = String.format(
                "{\"contractId\":\"%s\",\"contractCode\":\"%s\",\"orderCode\":\"%s\",\"roomId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"status\":\"%s\",\"paymentCode\":\"%s\"}",
                contract.getId(),
                safe(contract.getContractCode()),
                safe(contract.getOrderCode()),
                safe(contract.getRoomId()),
                safe(contract.getTenantId()),
                safe(contract.getLandlordId()),
                safe(contract.getStatus()),
                safe(paymentCode)
        );
        outboxRepository.save(new AppointmentEventOutbox(eventType, contract.getId(), payload));
    }

    private String buildCheckinInstructions(
            Contract contract,
            BookingListingPort.RoomCheckinDetails room,
            String checkinCode
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Xuat trinh ma don ").append(safe(contract.getOrderCode()))
                .append(" va ma nhan phong ").append(safe(checkinCode))
                .append(" khi gap chu tro.");
        if (contract.getStartDate() != null) {
            sb.append("\n2. Ngay nhan phong du kien: ").append(contract.getStartDate()).append(".");
        }
        if (room.landlordPhone() != null && !room.landlordPhone().isBlank()) {
            sb.append("\n3. Lien he chu tro ").append(safe(room.landlordName()))
                    .append(" qua so ").append(room.landlordPhone())
                    .append(" truoc khi den 30 phut de nhan chia khoa va chot chi so dien nuoc.");
        }
        if (room.houseRules() != null && !room.houseRules().isBlank()) {
            sb.append("\n4. Noi quy nha tro: ").append(room.houseRules());
        }
        return sb.toString();
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return (b != null && !b.isBlank()) ? b : "";
    }

    private String safe(String value) {
        return value != null ? value.replace("\"", "'") : "";
    }
}
