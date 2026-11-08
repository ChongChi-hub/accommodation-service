package vn.cozynest.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceVerificationDomainTest {

    @Test
    @DisplayName("State Machine: PENDING transitions to PASSED when approved by reviewer")
    void testPendingToPassed() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "nonce123", Instant.now().plusSeconds(120), 1, "v1.0"
        );
        fv.completeSubmission(true);
        fv.pass("mod-1");

        assertThat(fv.getStatus()).isEqualTo(FaceVerificationStatus.PASSED);
        assertThat(fv.getReviewedBy()).isEqualTo("mod-1");
        assertThat(fv.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("State Machine: PENDING transitions to FAILED with valid reason code")
    void testPendingToFailed() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "nonce123", Instant.now().plusSeconds(120), 1, "v1.0"
        );
        fv.completeSubmission(false);
        fv.fail("mod-1", "KHONG_KHOP_GIAY_TO", "Khuôn mặt không khớp ảnh CCCD");

        assertThat(fv.getStatus()).isEqualTo(FaceVerificationStatus.FAILED);
        assertThat(fv.getRejectReasonCode()).isEqualTo("KHONG_KHOP_GIAY_TO");
    }

    @Test
    @DisplayName("State Machine: Decided record cannot be transitioned again (throws ERR_FACE_ALREADY_DECIDED)")
    void testDecidedRecordCannotBeChanged() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "nonce123", Instant.now().plusSeconds(120), 1, "v1.0"
        );
        fv.completeSubmission(true);
        fv.pass("mod-1");

        assertThatThrownBy(() -> fv.fail("mod-2", "KHONG_KHOP_GIAY_TO", "note"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("đã có kết quả trước đó");
    }

    @Test
    @DisplayName("State Machine: Reason KHAC requires minimum 5 characters note")
    void testReasonKhacRequiresNote() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "nonce123", Instant.now().plusSeconds(120), 1, "v1.0"
        );
        fv.completeSubmission(true);

        assertThatThrownBy(() -> fv.fail("mod-1", "KHAC", "abc"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("tối thiểu 5 ký tự");
    }
}
