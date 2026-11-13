package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.UtilityReadingDto;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.model.UtilityReading;
import vn.cozynest.billing.domain.model.UtilityReadingAudit;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.domain.repository.UtilityReadingAuditRepository;
import vn.cozynest.billing.domain.repository.UtilityReadingRepository;
import vn.cozynest.billing.domain.service.UtilityTariffCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UtilityReadingService {
    private final UtilityReadingRepository repository;
    private final UtilityReadingAuditRepository auditRepository;
    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;
    private final Optional<BillingContractPort> billingContractPort;

    public UtilityReadingService(
            UtilityReadingRepository repository,
            UtilityReadingAuditRepository auditRepository,
            InvoiceRepository invoiceRepository,
            ObjectMapper objectMapper
    ) {
        this(repository, auditRepository, invoiceRepository, objectMapper, Optional.empty());
    }

    @Autowired
    public UtilityReadingService(
            UtilityReadingRepository repository,
            UtilityReadingAuditRepository auditRepository,
            InvoiceRepository invoiceRepository,
            ObjectMapper objectMapper,
            Optional<BillingContractPort> billingContractPort
    ) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.invoiceRepository = invoiceRepository;
        this.objectMapper = objectMapper;
        this.billingContractPort = billingContractPort != null ? billingContractPort : Optional.empty();
    }

    @Transactional
    public UtilityReadingDto.ReadingResponse recordReading(UtilityReadingDto.RecordReadingCommand cmd, String recordedBy) {
        UtilityTariffCalculator.PricingModel model = cmd.pricingModel() != null
                ? cmd.pricingModel() : UtilityTariffCalculator.PricingModel.EVN_TIERED;
        int occupants = cmd.occupantCount() != null ? cmd.occupantCount() : 1;

        long elecCost = UtilityTariffCalculator.calculateElectricityCost(
                cmd.electricCurr().subtract(cmd.electricPrev()), model,
                cmd.fixedElectricUnitPrice(), cmd.perPersonElectricRate(), occupants);
        long waterCost = UtilityTariffCalculator.calculateWaterCost(
                cmd.waterCurr().subtract(cmd.waterPrev()), model,
                cmd.fixedWaterUnitPrice(), cmd.perPersonWaterRate(), occupants);

        String imagesJson = writeJsonSafe(cmd.imageUrls(), "[]");
        String tariffJson = writeJsonSafe(Map.of(
                "model", model.name(),
                "occupants", occupants,
                "fixedElectric", cmd.fixedElectricUnitPrice() != null ? cmd.fixedElectricUnitPrice() : 0L,
                "fixedWater", cmd.fixedWaterUnitPrice() != null ? cmd.fixedWaterUnitPrice() : 0L
        ), "{}");

        UtilityReading reading = new UtilityReading(
                UUID.randomUUID().toString(), cmd.contractId(), cmd.roomId(), cmd.periodMonth(),
                cmd.electricPrev(), cmd.electricCurr(), cmd.waterPrev(), cmd.waterCurr(),
                imagesJson, recordedBy, Instant.now(),
                "ACTIVE", model.name(), tariffJson, elecCost, waterCost, 1, null
        );
        UtilityReading saved = repository.save(reading);
        auditRepository.save(UtilityReadingAudit.of(saved.getId(), "CREATED", writeSnapshot(saved), recordedBy, null));
        return toResponse(saved, saved.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10")));
    }

    @Transactional
    public UtilityReadingDto.ReadingResponse editReading(String readingId, UtilityReadingDto.EditReadingCommand cmd, String actorId) {
        UtilityReading reading = findLockedChecked(readingId);
        String prevSnapshot = writeSnapshot(reading);

        UtilityTariffCalculator.PricingModel model = cmd.pricingModel() != null
                ? cmd.pricingModel() : UtilityTariffCalculator.PricingModel.valueOf(reading.getPricingModel());
        int occupants = cmd.occupantCount() != null ? cmd.occupantCount() : 1;
        long elecCost = UtilityTariffCalculator.calculateElectricityCost(
                cmd.electricCurr().subtract(cmd.electricPrev()), model,
                cmd.fixedElectricUnitPrice(), cmd.perPersonElectricRate(), occupants);
        long waterCost = UtilityTariffCalculator.calculateWaterCost(
                cmd.waterCurr().subtract(cmd.waterPrev()), model,
                cmd.fixedWaterUnitPrice(), cmd.perPersonWaterRate(), occupants);

        reading.edit(cmd.electricPrev(), cmd.electricCurr(), cmd.waterPrev(), cmd.waterCurr(),
                model.name(), reading.getTariffConfigJson(), elecCost, waterCost, cmd.reason());
        UtilityReading saved = repository.save(reading);
        auditRepository.save(UtilityReadingAudit.of(saved.getId(), "EDITED", prevSnapshot, actorId, cmd.reason()));
        return toResponse(saved, saved.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10")));
    }

    @Transactional
    public UtilityReadingDto.ReadingResponse cancelReading(String readingId, String reason, String actorId) {
        UtilityReading reading = findLockedChecked(readingId);
        String prevSnapshot = writeSnapshot(reading);
        reading.cancel(reason);
        UtilityReading saved = repository.save(reading);
        auditRepository.save(UtilityReadingAudit.of(saved.getId(), "CANCELLED", prevSnapshot, actorId, reason));
        return toResponse(saved, false);
    }

    @Transactional
    public UtilityReadingDto.ReadingResponse disputeReading(String readingId, String reason, String actorId, boolean isAdmin) {
        UtilityReading reading = repository.findById(readingId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi so dien nuoc: " + readingId));
        assertContractParticipantOrAdmin(reading.getContractId(), actorId, isAdmin);
        auditRepository.save(UtilityReadingAudit.of(reading.getId(), "DISPUTED", writeSnapshot(reading), actorId, reason));
        reading.markDisputed(reason);
        UtilityReading saved = repository.save(reading);
        return toResponse(saved, saved.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10")));
    }

    @Transactional(readOnly = true)
    public UtilityReadingDto.ReadingResponse getLatestReading(String contractId) {
        return repository.findLatestByContractId(contractId)
                .map(r -> toResponse(r, r.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10"))))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UtilityReadingDto.ReadingResponse getLatestReading(String contractId, String actorId, boolean isAdmin) {
        assertContractParticipantOrAdmin(contractId, actorId, isAdmin);
        return getLatestReading(contractId);
    }

    @Transactional(readOnly = true)
    public List<UtilityReadingDto.ReadingResponse> getReadingsForContract(String contractId) {
        return repository.findByContractId(contractId).stream()
                .map(r -> toResponse(r, r.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10"))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UtilityReadingDto.ReadingResponse> getReadingsForContract(String contractId, String actorId, boolean isAdmin) {
        assertContractParticipantOrAdmin(contractId, actorId, isAdmin);
        return getReadingsForContract(contractId);
    }

    @Transactional(readOnly = true)
    public List<UtilityReadingAudit> getAuditHistory(String readingId) {
        return auditRepository.findByReadingId(readingId);
    }

    @Transactional(readOnly = true)
    public List<UtilityReadingAudit> getAuditHistory(String readingId, String actorId, boolean isAdmin) {
        UtilityReading reading = repository.findById(readingId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi so dien nuoc: " + readingId));
        assertContractParticipantOrAdmin(reading.getContractId(), actorId, isAdmin);
        return auditRepository.findByReadingId(readingId);
    }

    private void assertContractParticipantOrAdmin(String contractId, String actorId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        Optional<BillingContractPort.ContractSnapshot> contractOpt = billingContractPort
                .flatMap(port -> port.getContractSnapshot(contractId, LocalDate.now()));
        if (contractOpt.isPresent()) {
            BillingContractPort.ContractSnapshot snap = contractOpt.get();
            if (!snap.tenantId().equals(actorId) && !snap.landlordId().equals(actorId)) {
                throw new AccessDeniedException("Ban khong co quyen truy cap chi so dien nuoc cua hop dong nay");
            }
            return;
        }
        List<Invoice> invs = invoiceRepository.findByContractId(contractId);
        if (!invs.isEmpty()) {
            boolean participant = invs.stream().anyMatch(i ->
                    i.getTenantId().equals(actorId) || i.getLandlordId().equals(actorId));
            if (!participant) {
                throw new AccessDeniedException("Ban khong co quyen truy cap chi so dien nuoc cua hop dong nay");
            }
            return;
        }
        List<UtilityReading> readings = repository.findByContractId(contractId);
        if (!readings.isEmpty() && readings.stream().noneMatch(r -> actorId.equals(r.getRecordedBy()))) {
            throw new AccessDeniedException("Ban khong co quyen truy cap chi so dien nuoc cua hop dong nay");
        }
    }

    private UtilityReading findLockedChecked(String readingId) {
        UtilityReading reading = repository.findById(readingId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi so dien nuoc: " + readingId));
        List<Invoice> linkedInvoices = invoiceRepository.findByUtilityReadingId(readingId);
        boolean locked = linkedInvoices.stream().anyMatch(inv -> !"CANCELLED".equals(inv.getStatus()));
        if (locked) {
            throw new IllegalStateException("Chi so dien nuoc da gan voi hoa don phat hanh (ISSUED/PAID), khong the chinh sua hoac huy");
        }
        return reading;
    }

    private String writeJsonSafe(Object obj, String fallback) {
        if (obj == null) return fallback;
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return fallback; }
    }

    private String writeSnapshot(UtilityReading r) {
        return writeJsonSafe(Map.of(
                "electricPrev", r.getElectricPrev(), "electricCurr", r.getElectricCurr(),
                "waterPrev", r.getWaterPrev(), "waterCurr", r.getWaterCurr(),
                "electricityCost", r.getElectricityCost(), "waterCost", r.getWaterCost(),
                "status", r.getStatus(), "revision", r.getRevision()
        ), "{}");
    }

    @SuppressWarnings("unchecked")
    private List<String> readImagesJsonSafe(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private UtilityReadingDto.ReadingResponse toResponse(UtilityReading r, boolean surge) {
        List<String> images = readImagesJsonSafe(r.getReadingImagesJson());
        Long fixedElec = null;
        Long fixedWater = null;
        if (r.getTariffConfigJson() != null && !r.getTariffConfigJson().isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(r.getTariffConfigJson(), Map.class);
                if (map.get("fixedElectric") instanceof Number n) fixedElec = n.longValue();
                if (map.get("fixedWater") instanceof Number n) fixedWater = n.longValue();
            } catch (Exception ignored) {}
        }
        return new UtilityReadingDto.ReadingResponse(
                r.getId(), r.getContractId(), r.getRoomId(), r.getPeriodMonth(),
                r.getElectricPrev(), r.getElectricCurr(), r.getElectricConsumption(),
                r.getWaterPrev(), r.getWaterCurr(), r.getWaterConsumption(),
                surge, r.getRecordedAt(), r.getStatus(), r.getPricingModel(),
                r.getElectricityCost(), r.getWaterCost(), r.getRevision(),
                images, fixedElec, fixedWater, r.getEditReason()
        );
    }
}
