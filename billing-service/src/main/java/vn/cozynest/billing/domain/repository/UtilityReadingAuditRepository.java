package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.UtilityReadingAudit;

import java.util.List;

public interface UtilityReadingAuditRepository {
    UtilityReadingAudit save(UtilityReadingAudit audit);
    List<UtilityReadingAudit> findByReadingId(String readingId);
}

