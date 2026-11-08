package vn.cozynest.admin.application.port;

import vn.cozynest.admin.domain.model.Dispute;
import java.util.Optional;

public interface DisputeQueryPort {
    Optional<Dispute> getDisputeById(String id);
}
