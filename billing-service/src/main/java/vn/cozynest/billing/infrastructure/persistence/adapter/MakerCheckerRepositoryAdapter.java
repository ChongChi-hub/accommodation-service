package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.MakerCheckerJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataMakerCheckerRepository;

import java.util.List;
import java.util.Optional;

@Component
public class MakerCheckerRepositoryAdapter implements MakerCheckerRepository {
    private final SpringDataMakerCheckerRepository repository;

    public MakerCheckerRepositoryAdapter(SpringDataMakerCheckerRepository repository) {
        this.repository = repository;
    }

    @Override
    public MakerCheckerRequest save(MakerCheckerRequest domain) {
        MakerCheckerJpaEntity entity = toEntity(domain);
        MakerCheckerJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MakerCheckerRequest> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MakerCheckerRequest> findByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<MakerCheckerRequest> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    private MakerCheckerRequest toDomain(MakerCheckerJpaEntity entity) {
        return new MakerCheckerRequest(
                entity.getId(),
                entity.getMakerId(),
                entity.getCheckerId(),
                entity.getActionType(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getReason(),
                entity.getPayloadJson(),
                entity.getDecidedAt(),
                entity.getCreatedAt()
        );
    }

    private MakerCheckerJpaEntity toEntity(MakerCheckerRequest domain) {
        return MakerCheckerJpaEntity.builder()
                .id(domain.getId())
                .makerId(domain.getMakerId())
                .checkerId(domain.getCheckerId())
                .actionType(domain.getActionType())
                .amount(domain.getAmount())
                .status(domain.getStatus())
                .reason(domain.getReason())
                .payloadJson(domain.getPayloadJson())
                .decidedAt(domain.getDecidedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

