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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public RentalRequestService(
            RentalRequestRepository rentalRequestRepository,
            IdentityVerificationPort identityVerificationPort,
            AppointmentEventOutboxRepository outboxRepository
    ) {
        this(rentalRequestRepository, identityVerificationPort, outboxRepository, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Autowired
    public RentalRequestService(
            RentalRequestRepository rentalRequestRepository,
            IdentityVerificationPort identityVerificationPort,
            AppointmentEventOutboxRepository outboxRepository,
            Optional<BookingListingPort> listingPort,
            Optional<ContractRepository> contractRepository,
            Optional<RentalSagaOrchestrator> sagaOrchestrator
    ) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.identityVerificationPort = identityVerificationPort;
        this.outboxRepository = outboxRepository;
        this.listingPort = listingPort != null ? listingPort : Optional.empty();
        this.contractRepository = contractRepository != null ? contractRepository : Optional.empty();
        this.sagaOrchestrator = sagaOrchestrator != null ? sagaOrchestrator : Optional.empty();
    }

    public RentalRequestDto createRequest(String tenantId, CreateRentalRequestDto dto) {
        if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
            Optional<RentalRequest> existing = rentalRequestRepository.findByIdempotencyKey(dto.idempotencyKey());
            if (existing.isPresent()) {
                return RentalRequestDto.from(existing.get());
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
                        availability.reason() != null ? availability.reason() : "Phong khong kha dung hoac vuot so nguoi o toi da");
            }
        });

        String rawLandlordId = dto.landlordId() != null ? dto.landlordId().trim() : "";
        String normalizedLandlordId = rawLandlordId;
        if (normalizedLandlordId.matches("\\d+")) {
            normalizedLandlordId = String.format("u-landlord-%02d", Integer.parseInt(normalizedLandlordId));
        }

        RentalRequest saved = rentalRequestRepository.save(RentalRequest.create(
                dto.roomId(), tenantId, normalizedLandlordId,
                dto.proposedMoveInDate(), dto.numOccupants(), dto.message(), dto.idempotencyKey()
        ));
        publishOutbox("RENTAL_REQUEST_CREATED", saved);
        return RentalRequestDto.from(saved);
    }

    public RentalRequestDto approveRequest(String landlordId, String requestId, String contractId) {
        RentalRequest request = findOrThrow(requestId);
        boolean landlordMatches = request.getLandlordId().equals(landlordId)
                || (landlordId != null && landlordId.matches("u-landlord-0*(\\d+)") && request.getLandlordId().equals(landlordId.replaceFirst("u-landlord-0*", "")))
                || (request.getLandlordId().matches("\\d+") && landlordId != null && landlordId.equals(String.format("u-landlord-%02d", Integer.parseInt(request.getLandlordId()))));
        if (!landlordMatches) {
            throw new AccessDeniedException("Ban khong co quyen duyet yeu cau thue nay");
        }

        String effectiveContractId = contractId;
        if ((effectiveContractId == null || effectiveContractId.isBlank()) && contractRepository.isPresent()) {
            String contractCode = "HD-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            LocalDate start = request.getProposedMoveInDate() != null ? request.getProposedMoveInDate() : LocalDate.now().plusDays(3);
            LocalDate end = start.plusMonths(12);
            long monthlyRent = 4500000L;
            long depositAmount = 4500000L;
            String terms = "Hợp đồng thuê phòng điện tử CozyNest tiêu chuẩn. Người thuê cam kết tuân thủ đầy đủ nội quy phòng trọ.";

            Contract newContract = Contract.create(
                    contractCode,
                    request.getRoomId(),
                    request.getTenantId(),
                    landlordId,
                    request.getId(),
                    start,
                    end,
                    monthlyRent,
                    depositAmount,
                    terms
            );
            Contract savedContract = contractRepository.get().save(newContract);
            effectiveContractId = savedContract.getId();
            log.info("Auto-generated electronic contract {} (code {}) for approved request {}",
                    effectiveContractId, contractCode, request.getId());
        }

        String sagaId = UUID.randomUUID().toString();
        String payloadJson = String.format(
                "{\"requestId\":\"%s\",\"roomId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"contractId\":\"%s\"}",
                request.getId(), request.getRoomId(), request.getTenantId(), landlordId, effectiveContractId != null ? effectiveContractId : ""
        );
        sagaOrchestrator.ifPresent(orchestrator -> {
            try {
                orchestrator.startRentalSaga(sagaId, payloadJson);
                log.info("Started rental saga {} for approved request {}", sagaId, request.getId());
            } catch (Exception e) {
                log.warn("Failed to start saga for request {}: {}", request.getId(), e.getMessage());
            }
        });

        listingPort.ifPresent(port -> {
            try {
                port.holdRoom(request.getRoomId());
                log.info("Requested listing-service to HOLD room {}", request.getRoomId());
            } catch (Exception e) {
                log.warn("Failed to hold room {}: {}", request.getRoomId(), e.getMessage());
            }
        });

        request.approve(24, effectiveContractId);
        RentalRequest saved = rentalRequestRepository.save(request);
        publishOutbox("RENTAL_REQUEST_APPROVED", saved);
        return RentalRequestDto.from(saved);
    }

    public RentalRequestDto rejectRequest(String landlordId, String requestId) {
        RentalRequest request = findOrThrow(requestId);
        boolean landlordMatches = request.getLandlordId().equals(landlordId)
                || (landlordId != null && landlordId.matches("u-landlord-0*(\\d+)") && request.getLandlordId().equals(landlordId.replaceFirst("u-landlord-0*", "")))
                || (request.getLandlordId().matches("\\d+") && landlordId != null && landlordId.equals(String.format("u-landlord-%02d", Integer.parseInt(request.getLandlordId()))));
        if (!landlordMatches) {
            throw new AccessDeniedException("Ban khong co quyen tu choi yeu cau thue nay");
        }
        request.reject();
        RentalRequest saved = rentalRequestRepository.save(request);
        publishOutbox("RENTAL_REQUEST_REJECTED", saved);
        return RentalRequestDto.from(saved);
    }

    public RentalRequestDto cancelRequest(String actorId, boolean isAdmin, String requestId) {
        RentalRequest request = findOrThrow(requestId);
        if (!isAdmin && !request.getTenantId().equals(actorId) && !request.getLandlordId().equals(actorId)) {
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
        if (!isAdmin && !request.getTenantId().equals(actorId) && !request.getLandlordId().equals(actorId)) {
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
        return rentalRequestRepository.findByLandlordId(landlordId).stream().map(RentalRequestDto::from).toList();
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
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau thue: " + requestId));
    }

    private void publishOutbox(String eventType, RentalRequest req) {
        String payload = String.format(
                "{\"requestId\":\"%s\",\"roomId\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"status\":\"%s\"}",
                req.getId(), req.getRoomId(), req.getTenantId(), req.getLandlordId(), req.getStatus()
        );
        outboxRepository.save(new AppointmentEventOutbox(eventType, req.getId(), payload));
    }
}
