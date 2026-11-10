package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.PhoneVerification;

import java.util.Optional;

public interface PhoneVerificationRepository {
    PhoneVerification save(PhoneVerification verification);
    Optional<PhoneVerification> findLatestByPhone(String phoneNumber);
    long countRecentByPhone(String phoneNumber, long sinceSeconds);
}

