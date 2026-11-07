package vn.toam.admin.application.port;

import vn.toam.admin.domain.model.Dispute;
import java.util.Optional;

public interface DisputeQueryPort {
    Optional<Dispute> getDisputeById(String id);
}
