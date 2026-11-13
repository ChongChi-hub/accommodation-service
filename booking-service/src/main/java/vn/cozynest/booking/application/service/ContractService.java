package vn.cozynest.booking.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.ContractAmendment;
import vn.cozynest.booking.domain.model.ContractOccupant;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ContractAmendmentRepository;
import vn.cozynest.booking.domain.repository.ContractOccupantRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private static final int MAX_OCCUPANTS_PER_CONTRACT = 6;

    private final ContractRepository contractRepository;
    private final ContractAmendmentRepository amendmentRepository;
    private final ContractOccupantRepository occupantRepository;
    private final ContractSigningCoordinator signingCoordinator;
    private final AppointmentEventOutboxRepository outboxRepository;
    private final Optional<BookingListingPort> listingPort;

    public ContractService(
            ContractRepository contractRepository,
            ContractAmendmentRepository amendmentRepository,
            ContractOccupantRepository occupantRepository,
            ContractSigningCoordinator signingCoordinator,
            AppointmentEventOutboxRepository outboxRepository
    ) {
        this(contractRepository, amendmentRepository, occupantRepository, signingCoordinator, outboxRepository, Optional.empty());
    }

    @Autowired
    public ContractService(
            ContractRepository contractRepository,
            ContractAmendmentRepository amendmentRepository,
            ContractOccupantRepository occupantRepository,
            ContractSigningCoordinator signingCoordinator,
            AppointmentEventOutboxRepository outboxRepository,
            Optional<BookingListingPort> listingPort
    ) {
        this.contractRepository = contractRepository;
        this.amendmentRepository = amendmentRepository;
        this.occupantRepository = occupantRepository;
        this.signingCoordinator = signingCoordinator;
        this.outboxRepository = outboxRepository;
        this.listingPort = listingPort != null ? listingPort : Optional.empty();
    }

    public ContractDto createContract(CreateContractDto dto) {
        if (!dto.endDate().isAfter(dto.startDate())) {
            throw new IllegalArgumentException("Ngay ket thuc hop dong phai sau ngay bat dau");
        }
        String code = "HD-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Contract saved = contractRepository.save(Contract.create(
                code, dto.roomId(), dto.tenantId(), dto.landlordId(),
                dto.requestId(), dto.startDate(), dto.endDate(),
                dto.monthlyRent(), dto.depositAmount(), dto.terms(),
                dto.initialElectricityReading(), dto.initialWaterReading(),
                dto.electricityRate(), dto.waterRate()
        ));
        emitContractEvent("CONTRACT_CREATED", saved);
        return ContractDto.from(saved);
    }

    public ContractDto signContract(String contractId, String userId, String otpCode, boolean isLandlord, String ip, String ua) {
        Contract contract = findContractOrThrow(contractId);
        signingCoordinator.verifyAndSign(contract, userId, otpCode, isLandlord, ip, ua);
        Contract saved = contractRepository.save(contract);
        emitContractEvent("ACTIVE".equals(saved.getStatus()) ? "CONTRACT_ACTIVATED" : "CONTRACT_SIGNED", saved);
        return ContractDto.from(saved);
    }

    @Transactional(readOnly = true)
    public ContractDto getContractById(String contractId, String actorId, boolean isAdmin) {
        Contract contract = findContractOrThrow(contractId);
        assertParticipantOrAdmin(contract, actorId, isAdmin);
        return ContractDto.from(contract);
    }

    @Transactional(readOnly = true)
    public String getSignedPdfDownloadUrl(String contractId, String actorId, boolean isAdmin) {
        Contract contract = findContractOrThrow(contractId);
        assertParticipantOrAdmin(contract, actorId, isAdmin);
        return signingCoordinator.createSignedPdfUrl(contract, actorId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadSignedPdf(String contractId, String actorId, boolean isAdmin, String token) {
        Contract contract = findContractOrThrow(contractId);
        assertParticipantOrAdmin(contract, actorId, isAdmin);
        if (token != null && !token.isBlank()) {
            return signingCoordinator.downloadSignedPdfByToken(contractId, actorId, isAdmin, token);
        }
        return signingCoordinator.getOrGeneratePdf(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByTenant(String tenantId) {
        return contractRepository.findByTenantId(tenantId).stream().map(ContractDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByLandlord(String landlordId) {
        return contractRepository.findByLandlordId(landlordId).stream().map(ContractDto::from).toList();
    }

    public ContractDto updateDraftContract(String contractId, String actorId, boolean isAdmin, CreateContractDto dto) {
        Contract contract = findContractOrThrow(contractId);
        if (!isAdmin && !contract.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Chi chu tro moi duoc sua ban nhap hop dong");
        }
        contract.updateDraft(dto.startDate(), dto.endDate(), dto.monthlyRent(), dto.depositAmount(), dto.terms());
        Contract saved = contractRepository.save(contract);
        emitContractEvent("CONTRACT_DRAFT_UPDATED", saved);
        return ContractDto.from(saved);
    }

    public ContractDto uploadPhysicalSignedContract(String contractId, String actorId, boolean isAdmin, String scanUrl) {
        Contract contract = findContractOrThrow(contractId);
        if (!isAdmin && !contract.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Chi chu tro moi co quyen tai len ban scan hop dong da ky");
        }
        if (scanUrl == null || scanUrl.isBlank()) {
            throw new IllegalArgumentException("Duong dan anh scan hop dong khong duoc de trong");
        }
        contract.uploadPhysicalSignedContract(scanUrl);
        Contract saved = contractRepository.save(contract);
        emitContractEvent("CONTRACT_PHYSICAL_SCAN_UPLOADED", saved);
        return ContractDto.from(saved);
    }

    public ContractDto tenantConfirmPhysicalContract(String contractId, String actorId, boolean isAdmin) {
        Contract contract = findContractOrThrow(contractId);
        if (!isAdmin && !contract.getTenantId().equals(actorId)) {
            throw new AccessDeniedException("Chi khach thue moi co quyen xac nhan hop dong giay hop le");
        }
        if (!"WAITING_TENANT_CONFIRMATION".equals(contract.getPhysicalContractStatus())) {
            throw new IllegalStateException("Hop dong chua co ban scan cho khach xac nhan");
        }
        contract.tenantConfirmPhysicalContract();
        Contract saved = contractRepository.save(contract);
        emitContractEvent("CONTRACT_ACTIVATED", saved);
        return ContractDto.from(saved);
    }

    public ContractDto tenantRejectPhysicalContract(String contractId, String actorId, boolean isAdmin, String reason) {
        Contract contract = findContractOrThrow(contractId);
        if (!isAdmin && !contract.getTenantId().equals(actorId)) {
            throw new AccessDeniedException("Chi khach thue moi co quyen tu choi ban scan hop dong");
        }
        contract.tenantRejectPhysicalContract(reason != null ? reason : "Khach thue phat hien sai lech thong tin tren ban scan");
        Contract saved = contractRepository.save(contract);
        emitContractEvent("CONTRACT_PHYSICAL_SCAN_REJECTED", saved);
        return ContractDto.from(saved);
    }

    public ContractOccupantDto addOccupant(String contractId, String actorId, boolean isAdmin, AddOccupantDto dto) {
        Contract contract = findContractOrThrow(contractId);
        assertParticipantOrAdmin(contract, actorId, isAdmin);
        List<ContractOccupant> current = occupantRepository.findByContractId(contractId).stream()
                .filter(ContractOccupant::isActive)
                .toList();
        int effectiveMax = MAX_OCCUPANTS_PER_CONTRACT;
        if (listingPort.isPresent()) {
            BookingListingPort.RoomAvailabilityCheck roomCheck = listingPort.get()
                    .checkRoomAvailability(contract.getRoomId(), current.size() + 1);
            if (roomCheck.maxOccupants() > 0) {
                effectiveMax = Math.min(MAX_OCCUPANTS_PER_CONTRACT, roomCheck.maxOccupants());
            }
        }
        if (current.size() >= effectiveMax) {
            throw new IllegalStateException("So nguoi o cung vuot qua gioi han toi da cua hop dong (" + effectiveMax + ")");
        }
        boolean duplicateIdCard = current.stream()
                .anyMatch(o -> o.getIdCardNumber() != null && o.getIdCardNumber().equalsIgnoreCase(dto.idCardNumber()));
        if (duplicateIdCard) {
            throw new IllegalArgumentException("So CCCD/CMND nay da duoc dang ky trong hop dong");
        }
        ContractOccupant occ = ContractOccupant.create(
                contractId, dto.fullName(), dto.phoneNumber(),
                dto.idCardNumber(), dto.relationship(), dto.primaryTenant()
        );
        return ContractOccupantDto.from(occupantRepository.save(occ));
    }

    @Transactional(readOnly = true)
    public List<ContractOccupantDto> getOccupants(String contractId, String actorId, boolean isAdmin) {
        assertParticipantOrAdmin(findContractOrThrow(contractId), actorId, isAdmin);
        return occupantRepository.findByContractId(contractId).stream()
                .filter(ContractOccupant::isActive)
                .map(ContractOccupantDto::from)
                .toList();
    }

    public void removeOccupant(String contractId, String occupantId, String actorId, boolean isAdmin) {
        assertParticipantOrAdmin(findContractOrThrow(contractId), actorId, isAdmin);
        ContractOccupant occ = occupantRepository.findById(occupantId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay thong tin nguoi o cung: " + occupantId));
        if (!occ.getContractId().equals(contractId)) {
            throw new AccessDeniedException("Nguoi o cung khong thuoc hop dong nay");
        }
        occ.softRemove(actorId, "Participant removed");
        occupantRepository.save(occ);
    }

    public ContractAmendmentDto createAmendment(String contractId, String actorId, boolean isAdmin, CreateAmendmentDto dto) {
        Contract contract = findContractOrThrow(contractId);
        assertParticipantOrAdmin(contract, actorId, isAdmin);
        String code = "PL-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        ContractAmendment amendment = ContractAmendment.create(
                contractId, code, dto.amendmentType(),
                dto.newRent(), dto.newEndDate(), dto.effectiveDate(), dto.reason()
        );
        return ContractAmendmentDto.from(amendmentRepository.save(amendment));
    }

    public ContractAmendmentDto signAmendment(String amendmentId, String userId, String otpCode, boolean isLandlord) {
        ContractAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phu luc: " + amendmentId));
        Contract contract = findContractOrThrow(amendment.getContractId());
        String expectedSigner = isLandlord ? contract.getLandlordId() : contract.getTenantId();
        if (!expectedSigner.equals(userId)) {
            throw new AccessDeniedException("Ban khong co quyen ky phu luc hop dong nay");
        }
        signingCoordinator.verifyOtpForUser(userId, otpCode);
        if (isLandlord) {
            amendment.signLandlord();
        } else {
            amendment.signTenant();
        }
        ContractAmendment saved = amendmentRepository.save(amendment);
        if ("ACTIVE".equals(saved.getStatus())) {
            contract.applyAmendment(saved.getNewRent(), saved.getNewEndDate());
            contractRepository.save(contract);
            emitContractEvent("CONTRACT_AMENDMENT_APPLIED", contract);
        }
        return ContractAmendmentDto.from(saved);
    }

    @Transactional(readOnly = true)
    public EffectiveRentSnapshot getEffectiveRent(String contractId, LocalDate billingDate) {
        Contract contract = findContractOrThrow(contractId);
        LocalDate asOf = billingDate != null ? billingDate : LocalDate.now();
        long rent = amendmentRepository.findByContractId(contractId).stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()) && a.getNewRent() != null && a.getNewRent() > 0)
                .filter(a -> a.getEffectiveDate() == null || !a.getEffectiveDate().isAfter(asOf))
                .max(Comparator.comparing(a -> a.getEffectiveDate() != null ? a.getEffectiveDate() : LocalDate.MIN))
                .map(ContractAmendment::getNewRent)
                .orElse(contract.getMonthlyRent());
        return new EffectiveRentSnapshot(
                contract.getId(), contract.getRoomId(), contract.getTenantId(),
                contract.getLandlordId(), contract.getStatus(), rent, contract.getPaymentDueDay());
    }

    public record EffectiveRentSnapshot(
            String contractId,
            String roomId,
            String tenantId,
            String landlordId,
            String status,
            long effectiveMonthlyRent,
            int paymentDueDay
    ) {}

    @Transactional(readOnly = true)
    public List<ContractAmendmentDto> getAmendments(String contractId, String actorId, boolean isAdmin) {
        assertParticipantOrAdmin(findContractOrThrow(contractId), actorId, isAdmin);
        return amendmentRepository.findByContractId(contractId).stream().map(ContractAmendmentDto::from).toList();
    }

    private Contract findContractOrThrow(String contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong: " + contractId));
    }

    private void assertParticipantOrAdmin(Contract c, String actorId, boolean isAdmin) {
        if (!isAdmin && !c.getTenantId().equals(actorId) && !c.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Ban khong co quyen truy cap hop dong nay");
        }
    }

    private void emitContractEvent(String eventType, Contract contract) {
        String payload = String.format(
                "{\"contractId\":\"%s\",\"contractCode\":\"%s\",\"roomId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"status\":\"%s\"}",
                contract.getId(), contract.getContractCode(), contract.getRoomId(),
                contract.getTenantId(), contract.getLandlordId(), contract.getStatus()
        );
        outboxRepository.save(new AppointmentEventOutbox(eventType, contract.getId(), payload));
    }
}
