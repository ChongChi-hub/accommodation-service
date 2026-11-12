package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.ContractSignatureAudit;

import java.util.List;

public interface ContractSignatureAuditRepository {
    ContractSignatureAudit save(ContractSignatureAudit audit);
    List<ContractSignatureAudit> findByContractId(String contractId);
}

