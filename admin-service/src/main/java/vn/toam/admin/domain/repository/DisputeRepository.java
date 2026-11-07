package vn.toam.admin.domain.repository;

import vn.toam.admin.domain.model.Dispute;
import java.util.Optional;

public interface DisputeRepository {
    Optional<Dispute> findById(String id);
    Optional<Dispute> findByDisputeCode(String disputeCode);
    Dispute save(Dispute dispute);
}
