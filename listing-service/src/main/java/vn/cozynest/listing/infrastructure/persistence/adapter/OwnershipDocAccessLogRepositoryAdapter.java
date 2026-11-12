package vn.cozynest.listing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.OwnershipDocAccessLog;
import vn.cozynest.listing.domain.repository.OwnershipDocAccessLogRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.OwnershipDocAccessLogJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataOwnershipDocAccessLogRepository;

import java.util.List;

@Component
public class OwnershipDocAccessLogRepositoryAdapter implements OwnershipDocAccessLogRepository {

    private final SpringDataOwnershipDocAccessLogRepository repository;

    public OwnershipDocAccessLogRepositoryAdapter(SpringDataOwnershipDocAccessLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public OwnershipDocAccessLog save(OwnershipDocAccessLog entry) {
        OwnershipDocAccessLogJpaEntity saved = repository.save(OwnershipDocAccessLogJpaEntity.builder()
                .id(entry.getId())
                .docId(entry.getDocId())
                .roomId(entry.getRoomId())
                .actorId(entry.getActorId())
                .actorRole(entry.getActorRole())
                .action(entry.getAction())
                .accessedAt(entry.getAccessedAt())
                .build());
        return OwnershipDocAccessLog.builder()
                .id(saved.getId())
                .docId(saved.getDocId())
                .roomId(saved.getRoomId())
                .actorId(saved.getActorId())
                .actorRole(saved.getActorRole())
                .action(saved.getAction())
                .accessedAt(saved.getAccessedAt())
                .build();
    }

    @Override
    public List<OwnershipDocAccessLog> findByRoomId(Long roomId) {
        return repository.findByRoomIdOrderByAccessedAtDesc(roomId).stream()
                .map(e -> OwnershipDocAccessLog.builder()
                        .id(e.getId())
                        .docId(e.getDocId())
                        .roomId(e.getRoomId())
                        .actorId(e.getActorId())
                        .actorRole(e.getActorRole())
                        .action(e.getAction())
                        .accessedAt(e.getAccessedAt())
                        .build())
                .toList();
    }
}

