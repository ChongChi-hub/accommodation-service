package vn.cozynest.booking.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.SagaInstance;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;
import vn.cozynest.booking.domain.repository.SagaInstanceRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class RentalSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RentalSagaOrchestrator.class);
    private static final Pattern CONTRACT_ID_PATTERN = Pattern.compile("\"contractId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\"requestId\"\\s*:\\s*\"([^\"]+)\"");

    private final SagaInstanceRepository sagaRepository;
    private final ContractRepository contractRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final AppointmentEventOutboxRepository outboxRepository;

    public RentalSagaOrchestrator(
            SagaInstanceRepository sagaRepository,
            ContractRepository contractRepository,
            RentalRequestRepository rentalRequestRepository,
            AppointmentEventOutboxRepository outboxRepository
    ) {
        this.sagaRepository = sagaRepository;
        this.contractRepository = contractRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.outboxRepository = outboxRepository;
    }

    public SagaInstance startRentalSaga(String sagaId, String payloadJson) {
        return sagaRepository.findBySagaId(sagaId).orElseGet(() -> {
            SagaInstance saved = sagaRepository.save(SagaInstance.start(sagaId, "RENTAL_FLOW", "LOCK_HOLDING", payloadJson));
            emitSagaEvent("SAGA_STARTED", saved);
            return saved;
        });
    }

    public SagaInstance advanceToDepositPaid(String sagaId, String updatedPayload) {
        SagaInstance saga = sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga khong ton tai: " + sagaId));
        if (saga.isTerminal() || "DEPOSIT_PAID".equals(saga.getCurrentStep())) {
            return saga;
        }
        saga.advance("DEPOSIT_PAID", updatedPayload);
        SagaInstance saved = sagaRepository.save(saga);
        emitSagaEvent("SAGA_DEPOSIT_CONFIRMED", saved);
        return saved;
    }

    public SagaInstance completeSaga(String sagaId) {
        SagaInstance saga = sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga khong ton tai: " + sagaId));
        if ("COMPLETED".equals(saga.getStatus())) {
            return saga;
        }
        if ("COMPENSATED".equals(saga.getStatus())) {
            throw new IllegalStateException("Saga da duoc bu tru, khong the hoan thanh");
        }
        saga.complete();
        SagaInstance saved = sagaRepository.save(saga);
        emitSagaEvent("SAGA_COMPLETED", saved);
        return saved;
    }

    public SagaInstance failSaga(String sagaId, String reason) {
        SagaInstance saga = sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga khong ton tai: " + sagaId));
        if (saga.isTerminal()) {
            return saga;
        }
        saga.fail(reason);
        SagaInstance saved = sagaRepository.save(saga);
        emitSagaEvent("SAGA_FAILED", saved);
        return saved;
    }

    public SagaInstance compensateSaga(String sagaId, String reason) {
        SagaInstance saga = sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga khong ton tai: " + sagaId));
        if ("COMPENSATED".equals(saga.getStatus())) {
            return saga;
        }
        if ("COMPLETED".equals(saga.getStatus())) {
            throw new IllegalStateException("Khong the bu tru saga da hoan thanh");
        }
        compensateLinkedResources(saga.getPayloadJson());
        saga.compensate("RELEASE_HOLD_AND_TERMINATE_DRAFT", reason);
        SagaInstance saved = sagaRepository.save(saga);
        emitSagaEvent("SAGA_COMPENSATED", saved);
        return saved;
    }

    @Scheduled(fixedDelay = 300000)
    public int compensateStuckSagas() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<SagaInstance> stuck = sagaRepository.findStuckInProgressBefore(cutoff);
        int compensated = 0;
        for (SagaInstance saga : stuck) {
            compensateSaga(saga.getSagaId(), "Auto-compensated stuck IN_PROGRESS saga after 30m");
            compensated++;
            log.warn("Auto-compensated stuck saga {}", saga.getSagaId());
        }
        return compensated;
    }

    @Transactional(readOnly = true)
    public SagaInstance getSaga(String sagaId) {
        return sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga khong ton tai: " + sagaId));
    }

    private void compensateLinkedResources(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return;
        }
        Matcher contractMatcher = CONTRACT_ID_PATTERN.matcher(payloadJson);
        if (contractMatcher.find()) {
            contractRepository.findById(contractMatcher.group(1)).ifPresent(contract -> {
                if (!"ACTIVE".equals(contract.getStatus())) {
                    contract.terminate();
                    contractRepository.save(contract);
                }
            });
        }
        Matcher requestMatcher = REQUEST_ID_PATTERN.matcher(payloadJson);
        if (requestMatcher.find()) {
            rentalRequestRepository.findById(requestMatcher.group(1)).ifPresent(req -> {
                if (!"CANCELLED".equals(req.getStatus())) {
                    req.cancel();
                    rentalRequestRepository.save(req);
                }
            });
        }
    }

    private void emitSagaEvent(String eventType, SagaInstance saga) {
        String payload = String.format(
                "{\"sagaId\":\"%s\",\"step\":\"%s\",\"status\":\"%s\"}",
                saga.getSagaId(), saga.getCurrentStep(), saga.getStatus()
        );
        outboxRepository.save(new AppointmentEventOutbox(eventType, saga.getSagaId(), payload));
    }
}
