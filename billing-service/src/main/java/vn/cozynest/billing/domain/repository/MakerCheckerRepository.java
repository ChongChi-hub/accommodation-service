package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.MakerCheckerRequest;

import java.util.List;
import java.util.Optional;

public interface MakerCheckerRepository {
    MakerCheckerRequest save(MakerCheckerRequest request);
    Optional<MakerCheckerRequest> findById(String id);
    List<MakerCheckerRequest> findByStatus(String status);
    List<MakerCheckerRequest> findAll();
}


