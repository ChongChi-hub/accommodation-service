package vn.cozynest.admin.domain.repository;

import vn.cozynest.admin.domain.model.DeadLetterMessage;

import java.util.List;
import java.util.Optional;

public interface DeadLetterRepository {
    DeadLetterMessage save(DeadLetterMessage msg);
    Optional<DeadLetterMessage> findById(String id);
    List<DeadLetterMessage> findAll();
}

