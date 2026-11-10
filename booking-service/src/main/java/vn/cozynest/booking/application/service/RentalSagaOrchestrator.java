package vn.cozynest.booking.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.model.SagaInstance;
import vn.cozynest.booking.domain.repository.ContractRepository;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;
import vn.cozynest.booking.domain.repository.SagaInstanceRepository;

import java.util.UUID;

@Service
@Transactional
public class RentalSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RentalSagaOrchestrator.class);

    private final SagaInstanceRepository sagaRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final ContractRepository contractRepository;

    public RentalSagaOrchestrator(SagaInstanceRepository sagaRepository,
                                  RentalRequestRepository rentalRequestRepository,
                                  ContractRepository contractRepository) {
        this.sagaRepository = sagaRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.contractRepository = contractRepository;
    }

    public SagaInstance startRentalSaga(String rentalRequestId) {
        RentalRequest request = rentalRequestRepository.findById(rentalRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau thue"));

        String sagaId = "SAGA-RENTAL-" + UUID.randomUUID().toString().substring(0, 8);
        String payload = String.format("{\"requestId\":\"%s\",\"roomId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\"}",
                request.getId(), request.getRoomId(), request.getTenantId(), request.getLandlordId());

        SagaInstance saga = SagaInstance.start(sagaId, "CLOSED_RENTAL_LIFECYCLE", "REQUEST_SUBMITTED", payload);
        log.info("Started rental lifecycle saga: {}", sagaId);
        return sagaRepository.save(saga);
    }

    public SagaInstance onDepositPaid(String sagaId, String contractId) {
        SagaInstance saga = getSaga(sagaId);
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong"));

        saga.advance("HOLDING_DEPOSIT_PAID", String.format("{\"contractId\":\"%s\",\"depositStatus\":\"PAID\"}", contract.getId()));
        log.info("Saga {} advanced to HOLDING_DEPOSIT_PAID for contract {}", sagaId, contractId);
        return sagaRepository.save(saga);
    }

    public SagaInstance onContractFullySigned(String sagaId, String contractId) {
        SagaInstance saga = getSaga(sagaId);
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong"));

        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalStateException("Hop dong chua duoc ky boi ca 2 ben");
        }

        // Advance to ROOM_RENTED & complete
        saga.advance("ROOM_RENTED", String.format("{\"contractId\":\"%s\",\"roomStatus\":\"RENTED\"}", contractId));
        saga.complete();
        log.info("Saga {} successfully completed. Room marked as RENTED.", sagaId);
        return sagaRepository.save(saga);
    }

    public SagaInstance compensate(String sagaId, String reason) {
        SagaInstance saga = getSaga(sagaId);
        log.warn("Triggering compensation for saga {}: {}", sagaId, reason);

        // Compensation logic: release room hold, mark saga failed
        saga.fail(reason);
        return sagaRepository.save(saga);
    }

    private SagaInstance getSaga(String sagaId) {
        return sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay saga: " + sagaId));
    }
}

