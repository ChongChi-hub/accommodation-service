package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "entry_number", nullable = false, unique = true, length = 64)
    private String entryNumber;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "POSTED";

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<LedgerPostingJpaEntity> postings = new ArrayList<>();
}

