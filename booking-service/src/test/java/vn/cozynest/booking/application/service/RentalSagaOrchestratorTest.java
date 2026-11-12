package vn.cozynest.booking.application.service;

import org.junit.jupiter.api.Test;
import vn.cozynest.booking.application.port.BookingBillingPort;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.domain.model.*;
import vn.cozynest.booking.domain.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RentalSagaOrchestratorTest {

    @Test
    void shouldCompensateStuckSagaAndReleaseLinkedDraftContractAndRequest() {
        SagaInstanceRepository sagaRepo = mock(SagaInstanceRepository.class);
        ContractRepository contractRepo = mock(ContractRepository.class);
        RentalRequestRepository reqRepo = mock(RentalRequestRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);

        RentalSagaOrchestrator orchestrator = new RentalSagaOrchestrator(sagaRepo, contractRepo, reqRepo, outboxRepo);

        Contract draft = Contract.create("HD-SAGA", "r1", "t1", "l1", "req-1",
                LocalDate.now(), LocalDate.now().plusMonths(6), 3_000_000L, 3_000_000L, "Terms");
        RentalRequest req = RentalRequest.create("r1", "t1", "l1", LocalDate.now().plusDays(1), 1, "hi", "ik");

        SagaInstance stuck = SagaInstance.builder()
                .id("s1").sagaId("saga-1").sagaType("RENTAL_FLOW").currentStep("WAIT_DEPOSIT")
                .payloadJson("{\"contractId\":\"" + draft.getId() + "\",\"requestId\":\"" + req.getId() + "\"}")
                .status("IN_PROGRESS").createdAt(Instant.now().minusSeconds(4000))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        when(sagaRepo.findStuckInProgressBefore(any())).thenReturn(List.of(stuck));
        when(sagaRepo.findBySagaId("saga-1")).thenReturn(Optional.of(stuck));
        when(contractRepo.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(reqRepo.findById(req.getId())).thenReturn(Optional.of(req));
        when(sagaRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        int compensated = orchestrator.compensateStuckSagas();

        assertThat(compensated).isEqualTo(1);
        assertThat(stuck.getStatus()).isEqualTo("COMPENSATED");
        assertThat(draft.getStatus()).isEqualTo("TERMINATED");
        assertThat(req.getStatus()).isEqualTo("CANCELLED");
        verify(outboxRepo).save(any(AppointmentEventOutbox.class));
    }

    @Test
    void shouldRouteToManualInterventionOnCompensationFailureAndRejectOutOfOrderTransition() {
        SagaInstanceRepository sagaRepo = mock(SagaInstanceRepository.class);
        ContractRepository contractRepo = mock(ContractRepository.class);
        RentalRequestRepository reqRepo = mock(RentalRequestRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);
        BookingListingPort listingPort = mock(BookingListingPort.class);
        BookingBillingPort billingPort = mock(BookingBillingPort.class);

        RentalSagaOrchestrator orchestrator = new RentalSagaOrchestrator(
                sagaRepo, contractRepo, reqRepo, outboxRepo, Optional.of(listingPort), Optional.of(billingPort));

        SagaInstance saga = SagaInstance.start("saga-err", "RENTAL_FLOW", "LOCK_HOLDING",
                "{\"contractId\":\"c-err\",\"roomId\":\"r-err\"}");
        when(sagaRepo.findBySagaId("saga-err")).thenReturn(Optional.of(saga));
        when(sagaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Billing gateway timeout"))
                .when(billingPort).refundOrReleaseOnCompensation("c-err", "Cancel");

        SagaInstance manual = orchestrator.compensateSaga("saga-err", "Cancel");
        assertThat(manual.getStatus()).isEqualTo("MANUAL_INTERVENTION_REQUIRED");
        assertThatThrownBy(() -> orchestrator.advanceToDepositPaid("saga-err", null))
                .isInstanceOf(IllegalStateException.class);

        SagaInstance resolved = orchestrator.resolveManualSaga("saga-err", "Manually refunded by ops");
        assertThat(resolved.getStatus()).isEqualTo("COMPENSATED");
    }
}

