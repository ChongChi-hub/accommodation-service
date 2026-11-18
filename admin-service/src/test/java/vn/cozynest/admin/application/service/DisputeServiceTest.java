package vn.cozynest.admin.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.admin.application.port.AdminAuditPort;
import vn.cozynest.admin.application.port.AdminBillingEscrowPort;
import vn.cozynest.admin.application.port.DisputePersistencePort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DisputeServiceTest {

    @Test
    void mediatesWithEscrowFreezeResolvesRefundAndEnforcesParticipantGuard() {
        DisputePersistencePort port = mock(DisputePersistencePort.class);
        AdminBillingEscrowPort escrowPort = mock(AdminBillingEscrowPort.class);
        AdminAuditService auditService = new AdminAuditService(mock(AdminAuditPort.class));
        DisputeService service = new DisputeService(port, Optional.of(escrowPort), Optional.of(auditService));

        DisputePersistencePort.DisputeDetailView sample = new DisputePersistencePort.DisputeDetailView(
                "d-1", "DISP-501", "ct-1", "Room A",
                "u-t1", "Tenant 1", "TENANT",
                "u-l1", "Landlord 1", "LANDLORD",
                "DEPOSIT_RETURN", 2_000_000L, "Slow deposit refund",
                List.of(), "PENDING", null, null, null, Instant.now(), Instant.now(), List.of()
        );
        when(port.findViewById("d-1")).thenReturn(Optional.of(sample));

        assertThatThrownBy(() -> service.getDetail("d-1", "stranger", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.getDetail("d-1", "u-t1", false).disputeCode()).isEqualTo("DISP-501");

        service.mediate("d-1", "u-admin-01", "Reviewing", true, "127.0.0.1", "JUnit");
        verify(escrowPort).freezeEscrow(eq("ct-1"), eq("u-admin-01"), anyString());
        verify(port).updateDisputeState("d-1", "MEDIATING", "u-admin-01", null, false);

        service.resolve("d-1", "u-admin-01",
                new DisputeService.ResolveDisputeCommand("REFUND_TENANT", "Hoan coc 100%"), "127.0.0.1", "JUnit");
        verify(escrowPort).refundEscrow(eq("ct-1"), eq("u-admin-01"), anyString());
        verify(port).updateDisputeState("d-1", "RESOLVED", "u-admin-01", "Hoan coc 100%", true);
    }
}
