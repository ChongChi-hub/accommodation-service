package vn.cozynest.booking.application.service;

import org.junit.jupiter.api.Test;
import vn.cozynest.booking.application.exception.ContractNotFoundException;
import vn.cozynest.booking.application.exception.RentalRequestInvalidStateException;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.CancelReasonCode;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ConcurrentRoomHoldAndIdorTest {

    @Test
    void preventsSecondTenantFromHoldingRoomAlreadyHeldByFirstTenant() {
        RentalRequestRepository reqRepo = mock(RentalRequestRepository.class);
        IdentityVerificationPort idPort = mock(IdentityVerificationPort.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);
        ContractRepository contractRepo = mock(ContractRepository.class);

        RentalRequest first = RentalRequest.create(
                "206", "Phong 206", "u-tenant-01", "Tenant 1", "u-landlord-01",
                LocalDate.now().plusDays(3), 1, "Xin thue", "idem-1"
        );
        first.approve(24, "ct-1");

        RentalRequest second = RentalRequest.create(
                "206", "Phong 206", "u-tenant-02", "Tenant 2", "u-landlord-01",
                LocalDate.now().plusDays(4), 1, "Xin thue", "idem-2"
        );

        when(reqRepo.findById(second.getId())).thenReturn(Optional.of(second));
        when(reqRepo.findByRoomId("206")).thenReturn(List.of(first, second));
        when(contractRepo.findActiveByRoomId("206")).thenReturn(Optional.empty());

        RentalRequestService service = new RentalRequestService(
                reqRepo, idPort, outboxRepo, Optional.empty(), contractRepo, null, null
        );

        assertThatThrownBy(() -> service.approveRequest("u-landlord-01", second.getId(), null))
                .isInstanceOf(RentalRequestInvalidStateException.class)
                .hasMessageContaining("giữ chỗ");
    }

    @Test
    void returns404NotFoundForCrossTenantContractReadAndSupportsChangePlansReason() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        ContractService service = new ContractService(
                contractRepo, mock(ContractAmendmentRepository.class),
                mock(ContractOccupantRepository.class), mock(ContractSigningCoordinator.class),
                mock(AppointmentEventOutboxRepository.class)
        );

        Contract c = Contract.create("HD-IDOR", "206", "u-tenant-01", "u-landlord-01", null,
                LocalDate.now(), LocalDate.now().plusMonths(6), 4_000_000L, 4_000_000L, "Terms");
        when(contractRepo.findById(c.getId())).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.getContractById(c.getId(), "u-tenant-02", false))
                .isInstanceOf(ContractNotFoundException.class);
        assertThat(CancelReasonCode.valueOf("CHANGE_PLANS")).isEqualTo(CancelReasonCode.CHANGE_PLANS);
    }
}
