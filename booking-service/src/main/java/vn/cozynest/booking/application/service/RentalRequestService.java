package vn.cozynest.booking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.CreateRentalRequestDto;
import vn.cozynest.booking.application.dto.RentalRequestDto;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RentalRequestService {

    private final RentalRequestRepository rentalRequestRepository;

    public RentalRequestService(RentalRequestRepository rentalRequestRepository) {
        this.rentalRequestRepository = rentalRequestRepository;
    }

    public RentalRequestDto createRequest(String tenantId, CreateRentalRequestDto dto) {
        if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
            Optional<RentalRequest> existing = rentalRequestRepository.findByIdempotencyKey(dto.idempotencyKey());
            if (existing.isPresent()) {
                return RentalRequestDto.from(existing.get());
            }
        }

        RentalRequest request = RentalRequest.create(
                dto.roomId(),
                tenantId,
                dto.landlordId(),
                dto.proposedMoveInDate(),
                dto.numOccupants(),
                dto.message(),
                dto.idempotencyKey()
        );
        RentalRequest saved = rentalRequestRepository.save(request);
        return RentalRequestDto.from(saved);
    }

    public RentalRequestDto approveRequest(String landlordId, String requestId, String contractId) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau thue: " + requestId));

        if (!request.getLandlordId().equals(landlordId)) {
            throw new IllegalArgumentException("Ban khong co quyen duyet yeu cau thue nay");
        }

        request.approve(24, contractId); // 24h holding lock
        return RentalRequestDto.from(rentalRequestRepository.save(request));
    }

    public RentalRequestDto rejectRequest(String landlordId, String requestId) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau thue: " + requestId));

        if (!request.getLandlordId().equals(landlordId)) {
            throw new IllegalArgumentException("Ban khong co quyen tu choi yeu cau thue nay");
        }

        request.reject();
        return RentalRequestDto.from(rentalRequestRepository.save(request));
    }

    public RentalRequestDto cancelRequest(String tenantId, String requestId) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau thue: " + requestId));

        if (!request.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Ban khong co quyen huy yeu cau thue nay");
        }

        request.cancel();
        return RentalRequestDto.from(rentalRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public RentalRequestDto getRequestById(String requestId) {
        return rentalRequestRepository.findById(requestId)
                .map(RentalRequestDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau thue: " + requestId));
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getTenantRequests(String tenantId) {
        return rentalRequestRepository.findByTenantId(tenantId).stream().map(RentalRequestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalRequestDto> getLandlordRequests(String landlordId) {
        return rentalRequestRepository.findByLandlordId(landlordId).stream().map(RentalRequestDto::from).toList();
    }
}

