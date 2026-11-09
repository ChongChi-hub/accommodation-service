package vn.cozynest.identity.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.model.LandlordApprovalStatus;
import vn.cozynest.identity.domain.model.LandlordProfile;

import static org.assertj.core.api.Assertions.assertThat;

class LandlordProfileJpaEntityTest {

    @Test
    @DisplayName("Default @Version initializer must be 0L so Hibernate 7 treats new UUID entities as unsaved rather than detached")
    void defaultVersionMatchesHibernateUnsavedSentinel() {
        LandlordProfileJpaEntity empty = new LandlordProfileJpaEntity();
        assertThat(empty.getVersion()).isEqualTo(0L);

        LandlordProfile domain = LandlordProfile.createNew("user-123");
        LandlordProfileJpaEntity mapped = LandlordProfileJpaEntity.fromDomain(domain);
        assertThat(mapped.getVersion()).isEqualTo(0L);
        assertThat(mapped.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.AWAITING_KYC);

        LandlordProfile roundTrip = mapped.toDomain();
        assertThat(roundTrip.getId()).isEqualTo(domain.getId());
        assertThat(roundTrip.getUserId()).isEqualTo("user-123");
        assertThat(roundTrip.getVersion()).isEqualTo(0L);
    }
}

