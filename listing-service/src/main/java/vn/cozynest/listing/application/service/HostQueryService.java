package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.HostPublicProfileDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.domain.model.LandlordSnapshot;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.LandlordSnapshotRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostQueryService {

    private final LandlordSnapshotRepository landlordSnapshotRepository;
    private final RoomRepository roomRepository;
    private final RoomQueryService roomQueryService;

    public HostPublicProfileDto getPublicHostProfile(Long landlordId) {
        Optional<LandlordSnapshot> snapshotOpt = landlordSnapshotRepository.findByLandlordId(landlordId);
        List<Room> publicRooms = roomRepository.findByLandlordId(landlordId, 0, 50).content().stream()
                .filter(this::isPublicRoom)
                .toList();

        if (snapshotOpt.isEmpty() && publicRooms.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy thông tin chủ trọ: " + landlordId);
        }

        List<RoomDto> roomDtos = roomQueryService.toRoomDtos(publicRooms);
        if (snapshotOpt.isPresent()) {
            LandlordSnapshot ls = snapshotOpt.get();
            return HostPublicProfileDto.builder()
                    .id(ls.getId() != null ? ls.getId() : landlordId)
                    .landlordId(ls.getLandlordId() != null ? ls.getLandlordId() : landlordId)
                    .name(ls.getName())
                    .avatarUrl(ls.getAvatarUrl() != null && !ls.getAvatarUrl().isBlank() ? ls.getAvatarUrl() : "/uploads/avatars/default.png")
                    .verified(ls.isVerified())
                    .ratingAvg(ls.getRatingAvg())
                    .reviewCount(ls.getReviewCount())
                    .responseRatePercent(ls.getResponseRatePercent())
                    .responseTimeMinutes(ls.getResponseTimeMinutes())
                    .roomCount(roomDtos.size())
                    .joinedAt(ls.getJoinedAt())
                    .rooms(roomDtos)
                    .build();
        }

        Room first = publicRooms.get(0);
        return HostPublicProfileDto.builder()
                .id(landlordId)
                .landlordId(landlordId)
                .name(first.getLandlordName())
                .avatarUrl("/uploads/avatars/default.png")
                .verified(first.isVerifiedOwnership())
                .ratingAvg(4.8)
                .reviewCount(0)
                .responseRatePercent(95)
                .responseTimeMinutes(30)
                .roomCount(roomDtos.size())
                .rooms(roomDtos)
                .build();
    }

    public List<HostPublicProfileDto> getTopHosts() {
        List<LandlordSnapshot> snapshots = landlordSnapshotRepository.findAll().stream()
                .sorted(Comparator.comparing(LandlordSnapshot::getRatingAvg, Comparator.reverseOrder()))
                .toList();
        return snapshots.stream().map(ls -> {
            int roomCount = ls.getRoomCount() > 0 ? ls.getRoomCount() : 1;
            String avatar = (ls.getAvatarUrl() != null && !ls.getAvatarUrl().isBlank())
                    ? ls.getAvatarUrl()
                    : "/uploads/avatars/default.png";
            return HostPublicProfileDto.builder()
                    .id(ls.getId() != null ? ls.getId() : ls.getLandlordId())
                    .landlordId(ls.getLandlordId())
                    .name(ls.getName())
                    .avatarUrl(avatar)
                    .verified(ls.isVerified())
                    .ratingAvg(ls.getRatingAvg())
                    .reviewCount(ls.getReviewCount())
                    .responseRatePercent(ls.getResponseRatePercent())
                    .responseTimeMinutes(ls.getResponseTimeMinutes())
                    .roomCount(roomCount)
                    .joinedAt(ls.getJoinedAt())
                    .rooms(List.of())
                    .build();
        }).toList();
    }

    private boolean isPublicRoom(Room r) {
        if (r.isDeleted() || r.isHidden() || !r.isApproved()) {
            return false;
        }
        return "PUBLISHED".equalsIgnoreCase(r.getPublicationStatus())
                || "PUBLISHED".equalsIgnoreCase(r.getStatus())
                || "AVAILABLE".equalsIgnoreCase(r.getStatus());
    }
}

