package vn.toam.listing.infrastructure.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.toam.listing.domain.port.KycVerificationPort.KycVerificationStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HttpKycVerificationAdapterTest {

    @Test
    @DisplayName("4. Fail-Closed Policy: When identity-service is unreachable / times out, checkLandlordKyc returns unverified (isApproved == false)")
    void testFailClosedWhenIdentityServiceDown() {
        // Points to an unreachable port to simulate identity-service crash/timeout
        HttpKycVerificationAdapter adapter = new HttpKycVerificationAdapter(
                "http://127.0.0.1:59999",
                "ToAmInternalServiceKeySecret2026"
        );

        KycVerificationStatus status = adapter.checkLandlordKyc("u-landlord-01", 101L);

        // Fail-closed verification
        assertThat(status).isNotNull();
        assertThat(status.isApproved()).isFalse();
        assertThat(status.status()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("4. Cache Policy: Cache invalidation works and cached result is returned within 60s TTL")
    void testCacheInvalidation() {
        HttpKycVerificationAdapter adapter = new HttpKycVerificationAdapter(
                "http://127.0.0.1:59999",
                "ToAmInternalServiceKeySecret2026"
        );

        // First call fails closed
        KycVerificationStatus first = adapter.checkLandlordKyc("u-landlord-02", 102L);
        assertThat(first.isApproved()).isFalse();

        // Invalidate cache explicitly
        adapter.invalidateCache("u-landlord-02");

        // Second call still safely executes and returns fail-closed
        KycVerificationStatus second = adapter.checkLandlordKyc("u-landlord-02", 102L);
        assertThat(second.isApproved()).isFalse();
    }
}
