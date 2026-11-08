package vn.cozynest.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LandlordProfileStateMachineUnitTest {

    @Test
    @DisplayName("Initial state: Newly created profile is in AWAITING_KYC")
    void testInitialState() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.AWAITING_KYC);
        assertThat(profile.getUserId()).isEqualTo("user-001");
        assertThat(profile.getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("Transition AWAITING_KYC -> submitKyc() -> PENDING_REVIEW")
    void testSubmitKycFromAwaiting() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Transition PENDING_REVIEW -> approve() -> APPROVED")
    void testApproveFromPending() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.approve("reviewer-admin-01");

        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.APPROVED);
        assertThat(profile.getReviewedBy()).isEqualTo("reviewer-admin-01");
        assertThat(profile.getApprovedAt()).isNotNull();
        assertThat(profile.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("Transition PENDING_REVIEW -> reject() -> REJECTED")
    void testRejectFromPending() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.reject("reviewer-admin-01", "BLURRY_IMAGE", "Ảnh không rõ nét");

        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.REJECTED);
        assertThat(profile.getReviewedBy()).isEqualTo("reviewer-admin-01");
        assertThat(profile.getRejectReasonCode()).isEqualTo("BLURRY_IMAGE");
        assertThat(profile.getRejectReasonNote()).isEqualTo("Ảnh không rõ nét");
    }

    @Test
    @DisplayName("Transition REJECTED -> submitKyc() -> PENDING_REVIEW (Resubmission)")
    void testResubmitFromRejected() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.reject("reviewer-admin-01", "BLURRY_IMAGE", "Ảnh không rõ nét");
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.REJECTED);

        profile.submitKyc();
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.PENDING_REVIEW);
        assertThat(profile.getRejectReasonCode()).isNull();
        assertThat(profile.getRejectReasonNote()).isNull();
    }

    @Test
    @DisplayName("Invalid: AWAITING_KYC -> approve() throws 409 ERR_LANDLORD_INVALID_STATE_TRANSITION")
    void testCannotApproveDirectlyFromAwaitingKyc() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        assertThatThrownBy(() -> profile.approve("reviewer-admin-01"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Invalid: AWAITING_KYC -> reject() throws 409 ERR_LANDLORD_INVALID_STATE_TRANSITION")
    void testCannotRejectDirectlyFromAwaitingKyc() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        assertThatThrownBy(() -> profile.reject("reviewer-admin-01", "INVALID", "note"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Invalid: APPROVED -> submitKyc() throws 409 ERR_LANDLORD_INVALID_STATE_TRANSITION")
    void testCannotSubmitKycWhenAlreadyApproved() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.approve("reviewer-admin-01");

        assertThatThrownBy(profile::submitKyc)
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Invalid: APPROVED -> reject() throws 409 ERR_LANDLORD_INVALID_STATE_TRANSITION")
    void testCannotRejectWhenAlreadyApproved() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.approve("reviewer-admin-01");

        assertThatThrownBy(() -> profile.reject("reviewer-admin-01", "BLURRY", "note"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Invalid: REJECTED -> approve() directly throws 409 ERR_LANDLORD_INVALID_STATE_TRANSITION")
    void testCannotApproveDirectlyFromRejected() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.reject("reviewer-admin-01", "BLURRY", "note");

        assertThatThrownBy(() -> profile.approve("reviewer-admin-01"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Idempotent: APPROVED -> approve() is idempotent no-op")
    void testApproveIdempotent() {
        LandlordProfile profile = LandlordProfile.createNew("user-001");
        profile.submitKyc();
        profile.approve("reviewer-admin-01");
        profile.approve("reviewer-admin-01"); // Idempotent
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.APPROVED);
    }
}

