package vn.cozynest.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserVerificationDomainTest {

    @Test
    @DisplayName("Initial KYC submission should start in PENDING state")
    void shouldCreateInitialPending() {
        UserVerification v = UserVerification.createInitialSubmission(
                "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "HN",
                "key_front", "key_back", "v1.0"
        );

        assertThat(v.getStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(v.getAttemptCount()).isEqualTo(1);
        assertThat(v.getIdNumberLast4()).isEqualTo("1234");
    }

    @Test
    @DisplayName("Approve KYC successfully changes status to APPROVED")
    void shouldApproveSuccessfully() {
        UserVerification v = UserVerification.createInitialSubmission(
                "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "HN",
                "key_front", "key_back", "v1.0"
        );

        v.approve("u-mod-01");

        assertThat(v.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(v.getReviewedBy()).isEqualTo("u-mod-01");
        assertThat(v.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("Reviewer cannot be the owner of the KYC verification")
    void shouldPreventSelfReview() {
        UserVerification v = UserVerification.createInitialSubmission(
                "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "HN",
                "key_front", "key_back", "v1.0"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> v.approve("u-landlord-01"));
        assertThat(ex.getMessage()).contains("khong duoc phep la chu so huu");
    }

    @Test
    @DisplayName("Reject KYC with OTHER reason code requires at least 10 chars note")
    void shouldValidateRejectReasonNoteForOther() {
        UserVerification v = UserVerification.createInitialSubmission(
                "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "HN",
                "key_front", "key_back", "v1.0"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                v.reject("u-mod-01", KycRejectReasonCode.OTHER, "Ngan")
        );
        assertThat(ex.getMessage()).contains("tu 10 ky tu tro len");
    }

    @Test
    @DisplayName("Resubmitting rejected verification resets status to PENDING and increments attempts")
    void shouldResubmitRejected() {
        UserVerification v = UserVerification.createInitialSubmission(
                "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "HN",
                "key_front", "key_back", "v1.0"
        );
        v.reject("u-mod-01", KycRejectReasonCode.BLURRY, "Mo anh");

        v.resubmit("Nguyen Van A", LocalDate.of(1990, 1, 1), "new_enc", "new_hash", "1234",
                LocalDate.of(2020, 1, 1), "HN", "new_front", "new_back", "v1.0");

        assertThat(v.getStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(v.getAttemptCount()).isEqualTo(2);
        assertThat(v.getReviewedBy()).isNull();
    }
}

