package vn.cozynest.booking.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.CreateRentalRequestDto;
import vn.cozynest.booking.application.dto.RentalRequestDto;
import vn.cozynest.booking.application.exception.RentalRequestInvalidStateException;
import vn.cozynest.booking.application.exception.RentalRequestNotFoundException;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class RentalRequestService {

    private static final Logger log = LoggerFactory.getLogger(RentalRequestService.class);
    private static final int MAX_DAILY_REQUESTS_PER_TENANT = 5;

    private final RentalRequestRepository rentalRequestRepository;
    private final IdentityVerificationPort identityVerificationPort;
    private final AppointmentEventOutboxRepository outboxRepository;
    private final Optional<BookingListingPort> listingPort;
    private final Optional<ContractRepository> contractRepository;
    private final Optional<RentalSagaOrchestrator> sagaOrchestrator;
    private final Optional<vn.cozynest.booking.application.port.BookingBillingPort> billingPort;

    public RentalRequestService(
            RentalRequestRepository rentalRequestRepository,
            IdentityVerificationPort identityVerificationPort,
            AppointmentEventOutboxRepository outboxRepository) {
        this(rentalRequestRepository, identityVerificationPort, outboxRepository, Optional.empty(), null, null, null);
    }

    public RentalRequestService(
            RentalRequestRepository rentalRequestRepository,
            IdentityVerificationPort identityVerificationPort,
            AppointmentEventOutboxRepository outboxRepository,
            Optional<BookingListingPort> listingPort) {
        this(rentalRequestRepository, identityVerificationPort, outboxRepository, listingPort, null, null, null);
    }

    @Autowired
    public RentalRequestService(
            RentalRequestRepository rentalRequestRepository,
            IdentityVerificationPort identityVerificationPort,
            AppointmentEventOutboxRepository outboxRepository,
            Optional<BookingListingPort> listingPort,
            @Autowired(required = false) ContractRepository contractRepository,
            @Autowired(required = false) RentalSagaOrchestrator sagaOrchestrator,
            @Autowired(required = false) vn.cozynest.booking.application.port.BookingBillingPort billingPort) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.identityVerificationPort = identityVerificationPort;
        this.outboxRepository = outboxRepository;
        this.listingPort = listingPort != null ? listingPort : Optional.empty();
        this.contractRepository = Optional.ofNullable(contractRepository);
        this.sagaOrchestrator = Optional.ofNullable(sagaOrchestrator);
        this.billingPort = Optional.ofNullable(billingPort);
    }

    private String normalizeLandlordId(String landlordId) {
        if (landlordId == null) {
            return null;
        }
        String legacy = LandlordIdentityResolver.normalizeLegacy(landlordId);
        if (legacy != null && legacy.matches("^\\d+$") && identityVerificationPort != null) {
            return identityVerificationPort.resolveLandlordUserId(legacy);
        }
        return legacy;
    }

    private boolean matchesLandlord(String expected, String actual) {
        return LandlordIdentityResolver.matches(expected, actual);
    }

    public RentalRequestDto createRequest(String tenantId, CreateRentalRequestDto dto) {
        if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
            Optional<RentalRequest> existing = rentalRequestRepository.findByIdempotencyKey(dto.idempotencyKey());
            if (existing.isPresent()) {
                return RentalRequestDto.from(existing.get());
            }
        }
        IdentityVerificationPort.UserVerificationStatus verStatus = identityVerificationPort.getVerificationStatus(tenantId);
        if (verStatus != null) {
            if (!verStatus.emailVerified()) {
                throw new vn.cozynest.booking.application.exception.VerificationRequirementException(
                        "Vui lòng xác minh địa chỉ email trước khi gửi yêu cầu thuê phòng.", "EMAIL_NOT_VERIFIED"
                );
            }
            if (!verStatus.kycVerified()) {
                throw new vn.cozynest.booking.application.exception.VerificationRequirementException(
                        "Vui lòng hoàn tất xác thực CCCD (KYC) trước khi gửi yêu cầu thuê phòng.", "KYC_NOT_VERIFIED"
                );
            }
        }
        if (!identityVerificationPort.isPhoneVerified(tenantId)) {
            throw new IllegalStateException("Vui long xac thuc so dien thoai truoc khi gui yeu cau thue phong");
        }
        long recentCount = rentalRequestRepository.countByTenantIdCreatedAfter(
                tenantId, Instant.now().minus(24, ChronoUnit.HOURS));
        if (recentCount >= MAX_DAILY_REQUESTS_PER_TENANT) {
            throw new IllegalStateException("Ban da dat gioi han 5 yeu cau thue phong moi ngay");
        }
        if (rentalRequestRepository.existsActiveRequest(dto.roomId(), tenantId)) {
            throw new IllegalStateException("Ban da co yeu cau thue phong dang cho xu ly cho phong nay");
        }
        listingPort.ifPresent(port -> {
            BookingListingPort.RoomAvailabilityCheck availability = port.checkRoomAvailability(
                    dto.roomId(), dto.numOccupants());
            if (!availability.available()) {
                throw new IllegalStateException(
                        availability.reason() != null ? availability.reason()
                                : "Phong khong kha dung hoac vuot so nguoi o toi da");
            }
        });

        String targetLandlordId = normalizeLandlordId(dto.landlordId());
        if (LandlordIdentityResolver.matches(tenantId, targetLandlordId)) {
            throw new IllegalArgumentException("Chu tro khong the gui yeu cau thue phong cua chinh minh");
        }
        String snapshotRoomTitle = resolveRoomTitleSnapshot(dto.roomId());
        String snapshotTenantName = identityVerificationPort != null
                ? identityVerificationPort.resolveUserFullName(tenantId) : null;
        RentalRequest saved = rentalRequestRepository.save(RentalRequest.create(
                dto.roomId(), snapshotRoomTitle, tenantId, snapshotTenantName, targetLandlordId,
                dto.proposedMoveInDate(), dto.numOccupants(), dto.message(), dto.idempotencyKey()));
        publishOutbox("RENTAL_REQUEST_CREATED", saved);
        return RentalRequestDto.from(saved);
    }

    private String resolveRoomTitleSnapshot(String roomId) {
        if (listingPort.isEmpty() || roomId == null || roomId.isBlank()) {
            return null;
        }
        try {
            BookingListingPort.RoomContractSnapshot snap = listingPort.get().getRoomContractSnapshot(roomId);
            if (snap != null && snap.roomTitle() != null && !snap.roomTitle().isBlank()) {
                return snap.roomTitle().trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public RentalRequestDto approveRequest(String landlordId, String requestId, String contractId) {
        RentalRequest request = findOrThrow(requestId);
        if (!matchesLandlord(request.getLandlordId(), landlordId)) {
            throw new RentalRequestNotFoundException(requestId);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new RentalRequestInvalidStateException(
                    request.getStatus(),
                    "Yeu cau thue phong khong o trang thai cho duyet (hien tai: " + request.getStatus() + ")"
            );
        }

        String resolvedContractId = contractId;
        if ((resolvedContractId == null || resolvedContractId.isBlank()) && contractRepository.isPresent()) {
            String code = "HD-" + LocalDate.now().getYear() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            LocalDate start = request.getProposedMoveInDate() != null ? request.getProposedMoveInDate()
                    : LocalDate.now().plusDays(3);
            LocalDate end = start.plusMonths(6);

            long monthlyRent = 3500000L;
            long depositAmount = 3500000L;
            java.math.BigDecimal elecRate = java.math.BigDecimal.valueOf(3500);
            java.math.BigDecimal waterRate = java.math.BigDecimal.valueOf(25000);
            java.math.BigDecimal initialElec = java.math.BigDecimal.ZERO;
            java.math.BigDecimal initialWater = java.math.BigDecimal.ZERO;

            if (listingPort.isPresent()) {
                BookingListingPort.RoomContractSnapshot snapshot = listingPort.get()
                        .getRoomContractSnapshot(request.getRoomId());
                if (snapshot != null) {
                    if (snapshot.monthlyRent() != null && snapshot.monthlyRent() > 0) {
                        monthlyRent = snapshot.monthlyRent();
                    }
                    if (snapshot.depositAmount() != null && snapshot.depositAmount() > 0) {
                        depositAmount = snapshot.depositAmount();
                    }
                    if (snapshot.electricityRate() != null) {
                        elecRate = snapshot.electricityRate();
                    }
                    if (snapshot.waterRate() != null) {
                        waterRate = snapshot.waterRate();
                    }
                    if (snapshot.initialElectricityReading() != null) {
                        initialElec = snapshot.initialElectricityReading();
                    }
                    if (snapshot.initialWaterReading() != null) {
                        initialWater = snapshot.initialWaterReading();
                    }
                }
            }

            Contract newContract = contractRepository.get().save(Contract.create(
                    code, request.getRoomId(), request.getTenantId(), request.getLandlordId(),
                    request.getId(), start, end, monthlyRent, depositAmount,
                    "Hop dong thue phong tro tieu chuan To Am. Thoi han 06 thang, dat coc 01 thang.",
                    initialElec, initialWater, elecRate, waterRate));
            resolvedContractId = newContract.getId();
            billingPort.ifPresent(p -> p.openEscrowHold(newContract.getId(), newContract.getTenantId(), newContract.getLandlordId(), newContract.getDepositAmount()));
            final String finalCid = resolvedContractId;
            sagaOrchestrator.ifPresent(orchestrator -> {
                String payload = String.format(
                        "{\"sagaId\":\"%s\",\"requestId\":\"%s\",\"roomId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"contractId\":\"%s\"}",
                        "saga-" + request.getId(), request.getId(), request.getRoomId(),
                        request.getTenantId(), request.getLandlordId(), finalCid);
                orchestrator.startRentalSaga("saga-" + request.getId(), payload);
            });
        }

        request.approve(24, resolvedContractId);
        RentalRequest saved = rentalRequestRepository.save(request);
        publishOutbox("RENTAL_REQUEST_APPROVED", saved);
        return RentalRequestDto.from(saved);
    }

    public RentalRequestDto rejectRequest(String landlordId, String requestId) {
        return rejectRequest(landlordId, requestId, null);
    }

    public RentalRequestDto rejectRequest(String landlordId, String requestId, String reason) {
        RentalRequest request = findOrThrow(requestId);
        if (!matchesLandlord(request.getLandlordId(), landlordId)) {
            throw new RentalRequestNotFoundException(requestId);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new RentalRequestInvalidStateException(
                    request.getStatus(),
                    "Chi co the tu choi yeu cau dang cho duyet (hien tai: " + request.getStatus() + ")"
            );
        }
        request.reject(reason);
        RentalRequest saved = rentalRequestRepository.save(request);
        listingPort.ifPresent(port -> port.releaseRoomHold(saved.getRoomId()));
        log.info("Rejected rental request id={} by landlord={} reason={}", saved.getId(), landlordId, saved.getRejectionReason());
        publishOutbox("RENTAL_REQUEST_REJECTED", saved);
        return RentalRequestDto.from(saved);
    }

    public RentalRequestDto cancelRequest(String actorId, boolean isAdmin, String requestId) {
        RentalRequest request = findOrThrow(requestId);
        if (!isAdmin && !request.getTenantId().equals(actorId) && !matchesLandlord(request.getLandlordId(), actorId)) {
            throw new AccessDeniedException("Ban khong co quyen huy yeu cau thue nay");
        }
        request.cancel();
        RentalRequest saved = rentalRequestRepository.save(request);
        publishOutbox("RENTAL_REQUEST_CANCELLED", saved);
        return RentalRequestDto.from(saved);
    }

    @Transactional(readOnly = true)
    public RentalRequestDto getRequestById(String requestId, String actorId, boolean isAdmin) {
        RentalRequest request = findOrThrow(requestId);
        if (!isAdmin && !request.getTenantId().equals(actorId) && !matchesLandlord(request.getLandlordId(), actorId)) {
            throw new AccessDeniedException("Ban khong co quyen xem yeu cau thue nay");
        }
        return RentalRequestDto.from(request);
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getTenantRequests(String tenantId) {
        return rentalRequestRepository.findByTenantId(tenantId).stream().map(RentalRequestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getLandlordRequests(String landlordId) {
        Set<String> searchIds = LandlordIdentityResolver.resolveAliases(landlordId);

        List<RentalRequest> all = new ArrayList<>();
        for (String id : searchIds) {
            for (RentalRequest r : rentalRequestRepository.findByLandlordId(id)) {
                if (all.stream().noneMatch(existing -> existing.getId().equals(r.getId()))) {
                    all.add(r);
                }
            }
        }
        return all.stream().map(RentalRequestDto::from).toList();
    }

    @Scheduled(fixedDelay = 60000)
    public int releaseExpiredHoldings() {
        List<RentalRequest> expired = rentalRequestRepository.findExpiredApprovedHoldings(Instant.now());
        int released = 0;
        for (RentalRequest req : expired) {
            req.cancel();
            RentalRequest saved = rentalRequestRepository.save(req);
            publishOutbox("RENTAL_HOLDING_EXPIRED", saved);
            released++;
            log.info("Released expired 24h holding lock for rental request {}", req.getId());
        }
        return released;
    }

    private RentalRequest findOrThrow(String requestId) {
        return rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RentalRequestNotFoundException(requestId));
    }

    private void publishOutbox(String eventType, RentalRequest req) {
        String roomTitle = req.getRoomTitle();
        if ((roomTitle == null || roomTitle.isBlank()) && listingPort.isPresent() && req.getRoomId() != null) {
            roomTitle = resolveRoomTitleSnapshot(req.getRoomId());
        }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"requestId\":\"").append(escapeJson(req.getId())).append("\"");
        sb.append(",\"roomId\":\"").append(escapeJson(req.getRoomId())).append("\"");
        if (roomTitle != null) {
            sb.append(",\"roomTitle\":\"").append(escapeJson(roomTitle)).append("\"");
        }
        sb.append(",\"tenantId\":\"").append(escapeJson(req.getTenantId())).append("\"");
        sb.append(",\"landlordId\":\"").append(escapeJson(req.getLandlordId())).append("\"");
        sb.append(",\"status\":\"").append(req.getStatus() != null ? escapeJson(req.getStatus()) : "").append("\"");
        if (req.getRejectionReason() != null && !req.getRejectionReason().isBlank()) {
            sb.append(",\"rejectionReason\":\"").append(escapeJson(req.getRejectionReason())).append("\"");
        }
        if (req.getProposedMoveInDate() != null) {
            sb.append(",\"proposedMoveInDate\":\"").append(req.getProposedMoveInDate().toString()).append("\"");
        }
        sb.append(",\"numOccupants\":").append(req.getNumOccupants());
        if (req.getMessage() != null) {
            sb.append(",\"message\":\"").append(escapeJson(req.getMessage())).append("\"");
        }
        if (req.getContractId() != null) {
            sb.append(",\"contractId\":\"").append(escapeJson(req.getContractId())).append("\"");
        }
        sb.append("}");
        outboxRepository.save(new AppointmentEventOutbox(eventType, req.getId(), sb.toString()));
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
