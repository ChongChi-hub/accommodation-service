package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.UserVerification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserVerificationRepository {

    Optional<UserVerification> findById(String id);

    Optional<UserVerification> findByUserId(String userId);

    boolean existsByActiveIdHash(String idNumberHash);

    boolean existsByActiveIdHashAndUserIdNot(String idNumberHash, String userId);

    List<UserVerification> findAllFiltered(KycStatus status, String search, int offset, int limit);

    long countAllFiltered(KycStatus status, String search);

    List<UserVerification> findRejectedBefore(Instant cutoff);

    UserVerification save(UserVerification verification);
}

