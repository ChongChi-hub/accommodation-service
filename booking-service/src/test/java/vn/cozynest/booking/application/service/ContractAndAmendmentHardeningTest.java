package vn.cozynest.booking.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.*;
import vn.cozynest.booking.domain.repository.*;
import vn.cozynest.common.storage.EncryptedLocalStorageService;
import vn.cozynest.common.storage.PrivateObjectStoragePort;
import vn.cozynest.common.storage.SignedDownloadTokenService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContractAndAmendmentHardeningTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateEncryptedPdfAuditAndSignedUrlWhenBothPartiesSignWithOtp() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        ContractAmendmentRepository amendRepo = mock(ContractAmendmentRepository.class);
        ContractOccupantRepository occRepo = mock(ContractOccupantRepository.class);
        IdentityVerificationPort idPort = mock(IdentityVerificationPort.class);
        ContractSignatureAuditRepository auditRepo = mock(ContractSignatureAuditRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);

        PrivateObjectStoragePort storage = new EncryptedLocalStorageService(tempDir.toString(), "", false);
        SignedDownloadTokenService tokenService = new SignedDownloadTokenService("test-secret-key-at-least-32-bytes-long");
        ContractSigningCoordinator coordinator = new ContractSigningCoordinator(
                idPort, new ContractPdfService(), storage, tokenService, auditRepo);
        ContractService service = new ContractService(contractRepo, amendRepo, occRepo, coordinator, outboxRepo);

        Contract contract = Contract.create("HD-2026-001", "room-1", "tenant-1", "landlord-1", "req-1",
                LocalDate.now(), LocalDate.now().plusMonths(12), 4_500_000L, 4_500_000L, "Dieu khoan hop dong thue nha");
        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(idPort.verifyContractOtp("tenant-1", "654321")).thenReturn(true);
        when(idPort.verifyContractOtp("landlord-1", "112233")).thenReturn(true);
        when(idPort.verifyContractOtp("tenant-1", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.signContract(contract.getId(), "tenant-1", "000000", false, "1.1.1.1", "UA"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.signContract(contract.getId(), "intruder", "654321", false, "1.1.1.1", "UA"))
                .isInstanceOf(AccessDeniedException.class);

        service.signContract(contract.getId(), "tenant-1", "654321", false, "10.0.0.2", "Chrome");
        ContractDto active = service.signContract(contract.getId(), "landlord-1", "112233", true, "10.0.0.3", "Firefox");

        assertThat(active.status()).isEqualTo("ACTIVE");
        assertThat(contract.getPdfObjectKey()).startsWith("contracts/" + contract.getId() + "/");
        verify(auditRepo, times(2)).save(any(ContractSignatureAudit.class));

        String url = service.getSignedPdfDownloadUrl(contract.getId(), "tenant-1", false);
        String token = url.substring(url.indexOf("token=") + 6);
        byte[] pdfBytes = service.downloadSignedPdf(contract.getId(), "tenant-1", false, token);
        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        assertThatThrownBy(() -> service.downloadSignedPdf(contract.getId(), "landlord-1", false, token))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldApplyAmendmentToParentContractAndDispatchExpiryRemindersOnce() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        ContractAmendmentRepository amendRepo = mock(ContractAmendmentRepository.class);
        ContractOccupantRepository occRepo = mock(ContractOccupantRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);
        ContractSigningCoordinator coordinator = mock(ContractSigningCoordinator.class);
        doThrow(new IllegalArgumentException("OTP invalid")).when(coordinator).verifyOtpForUser("landlord-1", "000000");
        ContractService service = new ContractService(contractRepo, amendRepo, occRepo, coordinator, outboxRepo);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Contract contract = Contract.create("HD-2026-002", "room-2", "tenant-1", "landlord-1", "req-2",
                today.minusMonths(11), today.plusDays(14), 5_000_000L, 5_000_000L, "Terms");
        contract.signByTenant();
        contract.signByLandlord();

        ContractAmendment amendment = ContractAmendment.create(
                contract.getId(), "PL-2026-01", "RENT_AND_TERM", 5_500_000L, today.plusMonths(6), today.plusDays(1), "Gia han");
        when(amendRepo.findById(amendment.getId())).thenReturn(Optional.of(amendment));
        when(amendRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.signAmendment(amendment.getId(), "landlord-1", "000000", true))
                .isInstanceOf(IllegalArgumentException.class);
        service.signAmendment(amendment.getId(), "landlord-1", "112233", true);
        ContractAmendmentDto done = service.signAmendment(amendment.getId(), "tenant-1", "654321", false);
        assertThat(done.status()).isEqualTo("ACTIVE");
        assertThat(contract.getMonthlyRent()).isEqualTo(5_500_000L);
        assertThat(contract.getEndDate()).isEqualTo(today.plusMonths(6));

        Contract expiringContract = Contract.create("HD-EXP", "room-3", "t1", "l1", null,
                today.minusMonths(10), today.plusDays(14), 3_000_000L, 3_000_000L, "Terms");
        expiringContract.signByTenant();
        expiringContract.signByLandlord();
        when(contractRepo.findExpiringBetween(any(), any())).thenReturn(List.of(expiringContract));

        ContractExpiryScheduler scheduler = new ContractExpiryScheduler(contractRepo, outboxRepo);
        assertThat(scheduler.dispatchExpiryReminders()).isEqualTo(1);
        assertThat(scheduler.dispatchExpiryReminders()).isEqualTo(1); // 15-day reminder next if <=15 -> actually 30 captured first, 15 second, then 0!
        assertThat(scheduler.dispatchExpiryReminders()).isEqualTo(0);
    }

    @Test
    void shouldRejectDuplicateOccupantIdCardAndOverLimit() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        ContractOccupantRepository occRepo = mock(ContractOccupantRepository.class);
        ContractService service = new ContractService(contractRepo, mock(ContractAmendmentRepository.class),
                occRepo, mock(ContractSigningCoordinator.class), mock(AppointmentEventOutboxRepository.class));

        Contract c = Contract.create("HD-1", "r1", "t1", "l1", null, LocalDate.now(), LocalDate.now().plusMonths(6), 1000, 1000, "T");
        when(contractRepo.findById(c.getId())).thenReturn(Optional.of(c));
        when(occRepo.findByContractId(c.getId())).thenReturn(List.of(
                ContractOccupant.create(c.getId(), "Nguyen Van A", "0901234567", "079099000111", "SELF", true)
        ));

        assertThatThrownBy(() -> service.addOccupant(c.getId(), "t1", false,
                new AddOccupantDto("B", "0901234568", "079099000111", "FRIEND", false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSoftRemoveOccupantEnforceListingMaxOccupantsAndLockDraftAfterActive() {
        ContractRepository contractRepo = mock(ContractRepository.class);
        ContractAmendmentRepository amendRepo = mock(ContractAmendmentRepository.class);
        ContractOccupantRepository occRepo = mock(ContractOccupantRepository.class);
        BookingListingPort listingPort = mock(BookingListingPort.class);
        ContractService service = new ContractService(
                contractRepo, amendRepo, occRepo, mock(ContractSigningCoordinator.class),
                mock(AppointmentEventOutboxRepository.class), Optional.of(listingPort));

        Contract draft = Contract.create("HD-DRAFT", "room-cap", "t1", "l1", null,
                LocalDate.now(), LocalDate.now().plusMonths(6), 3_000_000L, 3_000_000L, "Old terms");
        when(contractRepo.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(contractRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ContractDto updatedDraft = service.updateDraftContract(draft.getId(), "l1", false,
                new CreateContractDto("room-cap", "t1", "l1", null,
                        LocalDate.now(), LocalDate.now().plusMonths(12), 3_500_000L, 3_500_000L, "Updated terms"));
        assertThat(updatedDraft.monthlyRent()).isEqualTo(3_500_000L);

        ContractOccupant occ = ContractOccupant.create(draft.getId(), "A", "0901234567", "079099000111", "SELF", true);
        when(occRepo.findById(occ.getId())).thenReturn(Optional.of(occ));
        when(occRepo.findByContractId(draft.getId())).thenReturn(List.of(occ));
        when(listingPort.checkRoomAvailability("room-cap", 2))
                .thenReturn(new BookingListingPort.RoomAvailabilityCheck(false, 1, "AVAILABLE", "Vuot so nguoi toi da"));

        assertThatThrownBy(() -> service.addOccupant(draft.getId(), "t1", false,
                new AddOccupantDto("B", "0901234568", "079099000222", "FRIEND", false)))
                .isInstanceOf(IllegalStateException.class);

        service.removeOccupant(draft.getId(), occ.getId(), "t1", false);
        assertThat(occ.getRemovedAt()).isNotNull();
        assertThat(occ.getRemovedBy()).isEqualTo("t1");
        verify(occRepo, never()).deleteById(any());
        verify(occRepo).save(occ);

        draft.signByTenant();
        draft.signByLandlord();
        assertThatThrownBy(() -> service.updateDraftContract(draft.getId(), "l1", false,
                new CreateContractDto("room-cap", "t1", "l1", null,
                        LocalDate.now(), LocalDate.now().plusMonths(12), 4_000_000L, 4_000_000L, "Tamper")))
                .isInstanceOf(IllegalStateException.class);
    }
}

