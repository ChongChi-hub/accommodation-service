package vn.cozynest.booking.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.cozynest.booking.application.dto.ContractDto;
import vn.cozynest.booking.application.dto.CreateContractDto;
import vn.cozynest.booking.application.dto.CreateRentalRequestDto;
import vn.cozynest.booking.application.dto.RentalRequestDto;
import vn.cozynest.booking.application.exception.ContractAlreadyExistsException;
import vn.cozynest.booking.application.exception.RentalRequestInvalidStateException;
import vn.cozynest.booking.application.exception.RentalRequestNotFoundException;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RentalRequestContractLifecycleTest {

    private ContractRepository contractRepository;
    private ContractAmendmentRepository amendmentRepository;
    private ContractOccupantRepository occupantRepository;
    private ContractSigningCoordinator signingCoordinator;
    private AppointmentEventOutboxRepository outboxRepository;
    private RentalRequestRepository rentalRequestRepository;
    private BookingListingPort listingPort;
    private IdentityVerificationPort identityPort;
    private ContractService contractService;
    private RentalRequestService rentalRequestService;

    @BeforeEach
    void setUp() {
        contractRepository = mock(ContractRepository.class);
        amendmentRepository = mock(ContractAmendmentRepository.class);
        occupantRepository = mock(ContractOccupantRepository.class);
        signingCoordinator = mock(ContractSigningCoordinator.class);
        outboxRepository = mock(AppointmentEventOutboxRepository.class);
        rentalRequestRepository = mock(RentalRequestRepository.class);
        listingPort = mock(BookingListingPort.class);
        identityPort = mock(IdentityVerificationPort.class);

        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rentalRequestRepository.save(any(RentalRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(identityPort.getVerificationStatus(anyString()))
                .thenReturn(new IdentityVerificationPort.UserVerificationStatus(true, true, true, "LANDLORD"));
        when(identityPort.isPhoneVerified(anyString())).thenReturn(true);

        contractService = new ContractService(
                contractRepository, amendmentRepository, occupantRepository,
                signingCoordinator, outboxRepository,
                Optional.of(listingPort), Optional.empty(), identityPort,
                Optional.of(rentalRequestRepository)
        );
        rentalRequestService = new RentalRequestService(
                rentalRequestRepository, identityPort, outboxRepository,
                Optional.of(listingPort), null, null, null
        );
    }

    private CreateContractDto contractDto(String requestId, String roomId, String landlordId) {
        return new CreateContractDto(
                roomId, "u-tenant-01", landlordId, requestId,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(6),
                4500000L, 4500000L, "Standard terms"
        );
    }

    private RentalRequest buildRequest(String id, String status, String landlordId, String contractId) {
        return RentalRequest.builder()
                .id(id).roomId("101").roomTitle("Studio Cau Giay")
                .tenantId("u-tenant-01").tenantName("Nguyen Van A")
                .landlordId(landlordId).proposedMoveInDate(LocalDate.now().plusDays(5))
                .numOccupants(1).status(status).contractId(contractId)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    void createContractRejectsNonApprovedStatuses() {
        for (String badStatus : new String[]{"REJECTED", "PENDING", "CANCELLED", "EXPIRED"}) {
            String reqId = "req-" + badStatus.toLowerCase();
            when(rentalRequestRepository.findById(reqId))
                    .thenReturn(Optional.of(buildRequest(reqId, badStatus, "u-landlord-01", null)));

            assertThatThrownBy(() -> contractService.createContract(contractDto(reqId, "101", "u-landlord-01"), "u-landlord-01", false))
                    .isInstanceOf(RentalRequestInvalidStateException.class);
        }
    }

    @Test
    void createContractRejectsDuplicateForApprovedRequest() {
        RentalRequest approved = buildRequest("req-dup", "APPROVED", "u-landlord-01", "ct-existing");
        when(rentalRequestRepository.findById("req-dup")).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> contractService.createContract(contractDto("req-dup", "101", "u-landlord-01"), "u-landlord-01", false))
                .isInstanceOf(ContractAlreadyExistsException.class);
    }

    @Test
    void concurrentCreateContractAllowsOnlyOneWinner() throws Exception {
        RentalRequest approved = buildRequest("req-race", "APPROVED", "u-landlord-01", null);
        when(rentalRequestRepository.findById("req-race")).thenReturn(Optional.of(approved));
        when(contractRepository.findByRequestId("req-race")).thenReturn(Optional.empty());

        Set<String> uniqueDbConstraint = ConcurrentHashMap.newKeySet();
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            if (c.getRequestId() != null && !uniqueDbConstraint.add(c.getRequestId())) {
                throw new DataIntegrityViolationException("Duplicate entry for uq_contracts_request_id");
            }
            return c;
        });

        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);

        Callable<Void> task = () -> {
            gate.await(5, TimeUnit.SECONDS);
            try {
                contractService.createContract(contractDto("req-race", "101", "u-landlord-01"), "u-landlord-01", false);
                successes.incrementAndGet();
            } catch (ContractAlreadyExistsException ex) {
                conflicts.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = pool.submit(task);
        Future<Void> f2 = pool.submit(task);
        gate.countDown();
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
    }

    @Test
    void createContractBlocksIdorOtherLandlordRequestAndDirectUnownedRoom() {
        RentalRequest othersReq = buildRequest("req-other", "APPROVED", "u-landlord-02", null);
        when(rentalRequestRepository.findById("req-other")).thenReturn(Optional.of(othersReq));

        assertThatThrownBy(() -> contractService.createContract(contractDto("req-other", "101", "u-landlord-01"), "u-landlord-01", false))
                .isInstanceOf(RentalRequestNotFoundException.class);

        when(listingPort.getRoomOwnerId("999")).thenReturn("u-landlord-02");
        assertThatThrownBy(() -> contractService.createContract(contractDto(null, "999", "u-landlord-01"), "u-landlord-01", false))
                .isInstanceOf(RentalRequestNotFoundException.class);

        when(listingPort.getRoomOwnerId("101")).thenReturn("u-landlord-01");
        ContractDto direct = contractService.createContract(contractDto(null, "101", "u-landlord-01"), "u-landlord-01", false);
        assertThat(direct.roomId()).isEqualTo("101");
    }

    @Test
    void rejectRequestStoresReasonReleasesHoldAndEmitsOutbox() {
        RentalRequest pending = buildRequest("req-rej", "PENDING", "u-landlord-01", null);
        when(rentalRequestRepository.findById("req-rej")).thenReturn(Optional.of(pending));

        RentalRequestDto res = rentalRequestService.rejectRequest("u-landlord-01", "req-rej", "Phong da co nguoi dat coc");
        assertThat(res.status()).isEqualTo("REJECTED");
        assertThat(res.rejectionReason()).isEqualTo("Phong da co nguoi dat coc");
        verify(listingPort).releaseRoomHold("101");
        verify(outboxRepository).save(argThat((AppointmentEventOutbox e) ->
                "RENTAL_REQUEST_REJECTED".equals(e.getEventType())
                        && e.getPayload().contains("Phong da co nguoi dat coc")));
    }

    @Test
    void createRequestSnapshotsRoomTitleAndTenantNameOnce() {
        when(listingPort.checkRoomAvailability("101", 1))
                .thenReturn(new BookingListingPort.RoomAvailabilityCheck(true, 2, "AVAILABLE", null));
        when(listingPort.getRoomContractSnapshot("101")).thenReturn(new BookingListingPort.RoomContractSnapshot(
                4500000L, 4500000L, BigDecimal.valueOf(3500), BigDecimal.valueOf(25000),
                BigDecimal.ZERO, BigDecimal.ZERO, "Studio Cau Giay VIP", "Cau Giay, Ha Noi"
        ));
        when(identityPort.resolveUserFullName("u-tenant-01")).thenReturn("Nguyen Van A");

        RentalRequestDto dto = rentalRequestService.createRequest("u-tenant-01",
                new CreateRentalRequestDto("101", "u-landlord-01", LocalDate.now().plusDays(3), 1, "Xin thue", "idem-1"));

        assertThat(dto.roomTitle()).isEqualTo("Studio Cau Giay VIP");
        assertThat(dto.tenantName()).isEqualTo("Nguyen Van A");
    }
}

