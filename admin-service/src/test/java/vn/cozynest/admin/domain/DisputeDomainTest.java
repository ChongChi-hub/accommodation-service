package vn.cozynest.admin.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.admin.domain.model.Dispute;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DisputeDomainTest {

    @Test
    @DisplayName("Verify Dispute creation with contract, complainant, and disputed amount")
    void testDisputeCreation() {
        LocalDateTime now = LocalDateTime.now();
        Dispute dispute = Dispute.builder()
                .id("disp-001")
                .disputeCode("DSP-202611-001")
                .contractId("ct-001")
                .roomTitle("Phòng trọ ban công")
                .complainantId("u-tenant-01")
                .defendantId("u-landlord-01")
                .disputeType("DEPOSIT_REFUND")
                .amount(2000000L)
                .reason("Chủ nhà không hoàn cọc sau khi kết thúc hợp đồng đúng hạn")
                .status("OPEN")
                .createdAt(now)
                .build();

        assertThat(dispute.getId()).isEqualTo("disp-001");
        assertThat(dispute.getDisputeCode()).isEqualTo("DSP-202611-001");
        assertThat(dispute.getAmount()).isEqualTo(2000000L);
        assertThat(dispute.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Verify Dispute status progression and resolution values")
    void testDisputeStatusValues() {
        Dispute dispute = Dispute.builder()
                .id("disp-002")
                .status("RESOLVED")
                .build();

        assertThat(dispute.getStatus()).isEqualTo("RESOLVED");
    }
}

