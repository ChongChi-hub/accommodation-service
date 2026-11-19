package vn.cozynest.booking.application.service;

import org.junit.jupiter.api.Test;
import vn.cozynest.booking.application.dto.ContractCheckinInfoDto;
import vn.cozynest.booking.application.dto.ContractDto;
import vn.cozynest.booking.application.exception.ContractNotFoundException;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContractDepositActivationFlowTest {

    @Test
    void activatesContractGeneratesOrderCodeMarksRoomRentedAndRevealsCheckinPii() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);
        BookingListingPort listingPort = mock(BookingListingPort.class);

        Contract contract = Contract.create(
                "HD-2026-FLOW1", "206", "u-tenant-01", "u-landlord-01", "req-206",
                LocalDate.now(), LocalDate.now().plusMonths(12),
                4_200_000L, 4_200_000L, "Dieu khoan chuan"
        );
        contract.signByTenant();
        contract.signByLandlord();
        assertThat(contract.getStatus()).isEqualTo("AWAITING_DEPOSIT");

        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingPort.getRoomCheckinDetails("206")).thenReturn(
                new BookingListingPort.RoomCheckinDetails(
                        "Phong studio Cau Giay",
                        "So 18 Ngo 68 Cau Giay, Ha Noi",
                        "Cau Giay, Ha Noi",
                        "Nguyen Van Chu",
                        "0901234567",
                        "Gio giac tu do"
                )
        );
        when(listingPort.markRoomRented("206", contract.getId())).thenReturn(true);

        ContractActivationService service = new ContractActivationService(
                contractRepo, outboxRepo, Optional.of(listingPort), Optional.empty()
        );

        // Before activation: PII is hidden for tenant, 404 for outsider
        ContractCheckinInfoDto before = service.getCheckinInfo(contract.getId(), "u-tenant-01", false);
        assertThat(before.revealed()).isFalse();
        assertThat(before.fullAddress()).isNull();
        assertThat(before.landlordPhone()).isNull();
        assertThat(before.orderCode()).isNull();
        assertThat(before.areaHint()).isEqualTo("Cau Giay, Ha Noi");

        assertThatThrownBy(() -> service.getCheckinInfo(contract.getId(), "u-tenant-02", false))
                .isInstanceOf(ContractNotFoundException.class);

        // Activate after deposit paid
        ContractDto activated = service.activateAfterDeposit(contract.getId(), "PAY-12345", 4_200_000L);
        assertThat(activated.status()).isEqualTo("ACTIVE");
        assertThat(activated.orderCode()).matches("^TOAM-\\d{6}-[0-9A-HJKMNP-TV-Z]{6}$");
        assertThat(activated.listingSyncStatus()).isEqualTo("SYNCED");
        verify(listingPort, times(1)).markRoomRented("206", contract.getId());
        verify(outboxRepo, times(1)).save(argThat(e -> "CONTRACT_ACTIVATED".equals(e.getEventType())));

        // Idempotent second call does not re-emit or re-call listing
        ContractDto idempotent = service.activateAfterDeposit(contract.getId(), "PAY-12345", 4_200_000L);
        assertThat(idempotent.orderCode()).isEqualTo(activated.orderCode());
        verify(listingPort, times(1)).markRoomRented("206", contract.getId());

        // After activation: PII revealed to tenant, still 404 to outsider
        ContractCheckinInfoDto after = service.getCheckinInfo(contract.getId(), "u-tenant-01", false);
        assertThat(after.revealed()).isTrue();
        assertThat(after.orderCode()).isEqualTo(activated.orderCode());
        assertThat(after.fullAddress()).isEqualTo("So 18 Ngo 68 Cau Giay, Ha Noi");
        assertThat(after.landlordPhone()).isEqualTo("0901234567");
        assertThat(after.checkinCode()).hasSize(6);
        assertThat(after.checkinInstructions()).contains(activated.orderCode());
    }

    @Test
    void queuesRetryOutboxWhenListingSyncFailsWithoutLosingPaidDeposit() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);
        BookingListingPort listingPort = mock(BookingListingPort.class);

        Contract contract = Contract.create(
                "HD-2026-FLOW2", "207", "u-tenant-01", "u-landlord-01", "req-207",
                LocalDate.now(), LocalDate.now().plusMonths(6),
                3_500_000L, 3_500_000L, "Terms"
        );
        contract.signByTenant();
        contract.signByLandlord();

        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingPort.getRoomCheckinDetails("207")).thenReturn(
                new BookingListingPort.RoomCheckinDetails("Phong 207", "Dia chi", "Khu vuc", "Chu", "0900", "")
        );
        when(listingPort.markRoomRented("207", contract.getId())).thenReturn(false);

        ContractActivationService service = new ContractActivationService(
                contractRepo, outboxRepo, Optional.of(listingPort), Optional.empty()
        );

        ContractDto res = service.activateAfterDeposit(contract.getId(), "PAY-999", 3_500_000L);
        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.listingSyncStatus()).isEqualTo("FAILED_RETRY");
        verify(outboxRepo).save(argThat((AppointmentEventOutbox e) -> "CONTRACT_LISTING_SYNC_RETRY".equals(e.getEventType())));
    }
}

