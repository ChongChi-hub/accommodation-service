package vn.cozynest.booking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.LandlordTenantRosterDto;
import vn.cozynest.booking.application.port.BookingListingPort;
import vn.cozynest.booking.application.port.IdentityVerificationPort;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.ContractOccupant;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.ContractOccupantRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LandlordTenantRosterService {

    private final ContractRepository contractRepository;
    private final ContractOccupantRepository occupantRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final Optional<IdentityVerificationPort> identityPort;
    private final Optional<BookingListingPort> listingPort;

    public LandlordTenantRosterService(
            ContractRepository contractRepository,
            ContractOccupantRepository occupantRepository,
            RentalRequestRepository rentalRequestRepository,
            Optional<IdentityVerificationPort> identityPort,
            Optional<BookingListingPort> listingPort
    ) {
        this.contractRepository = contractRepository;
        this.occupantRepository = occupantRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.identityPort = identityPort != null ? identityPort : Optional.empty();
        this.listingPort = listingPort != null ? listingPort : Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<LandlordTenantRosterDto> getActiveTenantsForLandlord(String landlordId) {
        Set<String> aliases = LandlordIdentityResolver.resolveAliases(landlordId);
        List<Contract> activeContracts = aliases.stream()
                .flatMap(id -> contractRepository.findByLandlordId(id).stream())
                .collect(Collectors.toMap(Contract::getId, c -> c, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .toList();

        if (activeContracts.isEmpty()) {
            return List.of();
        }

        Map<String, RentalRequest> requestByContract = new HashMap<>();
        Map<String, RentalRequest> requestByTenant = new HashMap<>();
        for (String alias : aliases) {
            for (RentalRequest req : rentalRequestRepository.findByLandlordId(alias)) {
                if (req.getContractId() != null) requestByContract.putIfAbsent(req.getContractId(), req);
                if (req.getTenantId() != null) requestByTenant.putIfAbsent(req.getTenantId(), req);
            }
        }

        Map<String, IdentityVerificationPort.UserProfileSummary> profileCache = new HashMap<>();
        Map<String, String> roomTitleCache = new HashMap<>();

        return activeContracts.stream()
                .map(c -> toDto(c, requestByContract, requestByTenant, profileCache, roomTitleCache))
                .toList();
    }

    private LandlordTenantRosterDto toDto(
            Contract c,
            Map<String, RentalRequest> requestByContract,
            Map<String, RentalRequest> requestByTenant,
            Map<String, IdentityVerificationPort.UserProfileSummary> profileCache,
            Map<String, String> roomTitleCache
    ) {
        IdentityVerificationPort.UserProfileSummary profile = profileCache.computeIfAbsent(
                c.getTenantId(),
                id -> identityPort.map(port -> port.resolveUserProfile(id)).orElse(null)
        );
        RentalRequest req = requestByContract.getOrDefault(c.getId(), requestByTenant.get(c.getTenantId()));
        List<ContractOccupant> occupants = occupantRepository.findByContractId(c.getId());
        ContractOccupant primary = occupants.stream()
                .filter(ContractOccupant::isActive)
                .filter(ContractOccupant::isPrimaryTenant)
                .findFirst()
                .orElseGet(() -> occupants.stream().filter(ContractOccupant::isActive).findFirst().orElse(null));

        String tenantName = firstNonBlank(
                profile != null ? profile.fullName() : null,
                primary != null ? primary.getFullName() : null,
                req != null ? req.getTenantName() : null
        );
        String tenantPhone = firstNonBlank(
                profile != null ? profile.phone() : null,
                primary != null ? primary.getPhoneNumber() : null
        );
        String tenantEmail = profile != null ? profile.email() : null;
        String roomTitle = resolveRoomTitle(c.getRoomId(), req, roomTitleCache);

        return new LandlordTenantRosterDto(
                c.getId(),
                c.getContractCode(),
                c.getOrderCode(),
                c.getRoomId(),
                roomTitle,
                c.getTenantId(),
                tenantName,
                tenantEmail,
                tenantPhone,
                c.getStatus(),
                c.getStartDate(),
                c.getEndDate(),
                c.getMonthlyRent(),
                c.getDepositAmount(),
                c.getDepositPaidAt()
        );
    }

    private String resolveRoomTitle(String roomId, RentalRequest req, Map<String, String> cache) {
        if (roomId == null || roomId.isBlank()) {
            return req != null ? req.getRoomTitle() : null;
        }
        return cache.computeIfAbsent(roomId, id -> {
            String fromListing = listingPort.map(port -> {
                try {
                    BookingListingPort.RoomContractSnapshot snap = port.getRoomContractSnapshot(id);
                    return snap != null ? snap.roomTitle() : null;
                } catch (Exception ignored) {
                    return null;
                }
            }).orElse(null);
            return firstNonBlank(fromListing, req != null ? req.getRoomTitle() : null);
        });
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
