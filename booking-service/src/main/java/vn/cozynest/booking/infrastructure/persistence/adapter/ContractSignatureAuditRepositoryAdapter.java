package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.ContractSignatureAudit;
import vn.cozynest.booking.domain.repository.ContractSignatureAuditRepository;
import vn.cozynest.booking.infrastructure.persistence.entity.ContractSignatureAuditJpaEntity;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataContractSignatureAuditRepository;

import java.util.List;

@Component
public class ContractSignatureAuditRepositoryAdapter implements ContractSignatureAuditRepository {

    private final SpringDataContractSignatureAuditRepository repository;

    public ContractSignatureAuditRepositoryAdapter(SpringDataContractSignatureAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public ContractSignatureAudit save(ContractSignatureAudit audit) {
        ContractSignatureAuditJpaEntity entity = ContractSignatureAuditJpaEntity.builder()
                .id(audit.getId())
                .contractId(audit.getContractId())
                .signerId(audit.getSignerId())
                .signerRole(audit.getSignerRole())
                .otpVerified(audit.isOtpVerified())
                .documentHash(audit.getDocumentHash())
                .pdfObjectKey(audit.getPdfObjectKey())
                .ipAddress(audit.getIpAddress())
                .userAgent(audit.getUserAgent())
                .signedAt(audit.getSignedAt())
                .build();
        ContractSignatureAuditJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ContractSignatureAudit> findByContractId(String contractId) {
        return repository.findByContractIdOrderBySignedAtAsc(contractId).stream().map(this::toDomain).toList();
    }

    private ContractSignatureAudit toDomain(ContractSignatureAuditJpaEntity e) {
        return ContractSignatureAudit.builder()
                .id(e.getId())
                .contractId(e.getContractId())
                .signerId(e.getSignerId())
                .signerRole(e.getSignerRole())
                .otpVerified(e.isOtpVerified())
                .documentHash(e.getDocumentHash())
                .pdfObjectKey(e.getPdfObjectKey())
                .ipAddress(e.getIpAddress())
                .userAgent(e.getUserAgent())
                .signedAt(e.getSignedAt())
                .build();
    }
}

