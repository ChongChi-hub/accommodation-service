package vn.cozynest.booking.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.booking.application.dto.CreateRentalRequestDto;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RentalRequestServiceTest {

    private final RentalRequestRepository repo = mock(RentalRequestRepository.class);
    private final IdentityVerificationPort identityPort = mock(IdentityVerificationPort.class);
    private final AppointmentEventOutboxRepository outbox = mock(AppointmentEventOutboxRepository.class);
    private final RentalRequestService service = new RentalRequestService(repo, identityPort, outbox);

    @Test
    void shouldRejectCreateWhenPhoneNotVerified() {
        when(identityPort.isPhoneVerified("t1")).thenReturn(false);
        CreateRentalRequestDto dto = new CreateRentalRequestDto("r1", "l1", LocalDate.now().plusDays(2), 1, "hi", "k1");

        assertThatThrownBy(() -> service.createRequest("t1", dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("so dien thoai");
    }

    @Test
    void shouldEnforceDailyRateLimitAndDuplicateGuard() {
        when(identityPort.isPhoneVerified("t1")).thenReturn(true);
        when(repo.countByTenantIdCreatedAfter(eq("t1"), any())).thenReturn(5L);
        CreateRentalRequestDto dto = new CreateRentalRequestDto("r1", "l1", LocalDate.now().plusDays(2), 1, "hi", null);

        assertThatThrownBy(() -> service.createRequest("t1", dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gioi han");

        when(repo.countByTenantIdCreatedAfter(eq("t1"), any())).thenReturn(1L);
        when(repo.existsActiveRequest("r1", "t1")).thenReturn(true);
        assertThatThrownBy(() -> service.createRequest("t1", dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dang cho xu ly");
    }

    @Test
    void shouldDenyCrossLandlordApprovalAndParticipantReadIdor() {
        RentalRequest req = RentalRequest.create("r1", "t1", "l1", LocalDate.now().plusDays(3), 1, "msg", "k2");
        when(repo.findById(req.getId())).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.approveRequest("l2", req.getId(), "c1"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getRequestById(req.getId(), "stranger", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.getRequestById(req.getId(), "t1", false).id()).isEqualTo(req.getId());
    }

    @Test
    void shouldReleaseExpiredApprovedHoldings() {
        RentalRequest expired = RentalRequest.builder()
                .id("req-exp").roomId("r1").tenantId("t1").landlordId("l1")
                .proposedMoveInDate(LocalDate.now()).numOccupants(1)
                .status("APPROVED").holdingExpiresAt(Instant.now().minusSeconds(60))
                .build();
        when(repo.findExpiredApprovedHoldings(any())).thenReturn(List.of(expired));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        int count = service.releaseExpiredHoldings();

        assertThat(count).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo("CANCELLED");
        verify(outbox).save(any(AppointmentEventOutbox.class));
    }
}

