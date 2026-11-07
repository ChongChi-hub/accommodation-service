package vn.toam.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.listing.application.dto.CreateRoomRequest;
import vn.toam.listing.application.dto.UpdateRoomRequest;
import vn.toam.listing.domain.model.Room;
import vn.toam.listing.domain.model.RoomStatusHistory;
import vn.toam.listing.domain.port.RoomUsagePort;
import vn.toam.listing.domain.repository.RoomRepository;
import vn.toam.listing.domain.repository.RoomStatusHistoryRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LandlordRoomManagementService {

    private final RoomRepository roomRepository;
    private final RoomStatusHistoryRepository roomStatusHistoryRepository;
    private final RoomUsagePort roomUsagePort;
    private final vn.toam.listing.domain.repository.RoomImageRepository roomImageRepository;

    @org.springframework.beans.factory.annotation.Value("${toam.listing.min-images-for-review:3}")
    private int minImagesForReview = 3;

    public Room toggleVisibility(Long landlordId, Long roomId, boolean visible, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        String fromStatus = room.getStatus();

        if (!visible) {
            // Hiding the room: only allowed if currently PUBLISHED
            if (!"PUBLISHED".equalsIgnoreCase(fromStatus)) {
                throw new IllegalStateException("Chỉ có thể tạm ẩn bài đăng khi phòng đang ở trạng thái đã duyệt (PUBLISHED).");
            }
            room.setStatus("HIDDEN");
            room.setHiddenAt(LocalDateTime.now());
            room.setHiddenBy("OWNER");
            room.setHiddenReason("Chủ trọ chủ động tạm ẩn bài đăng");

            roomStatusHistoryRepository.save(RoomStatusHistory.builder()
                    .roomId(room.getId())
                    .fromStatus(fromStatus)
                    .toStatus("HIDDEN")
                    .actorId(actorId)
                    .actorRole("LANDLORD")
                    .reason("Chủ trọ chủ động tạm ẩn bài đăng")
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("Landlord {} hid room {}", landlordId, roomId);
        } else {
            // Un-hiding the room: only allowed if currently HIDDEN by OWNER and was previously published
            if (!"HIDDEN".equalsIgnoreCase(fromStatus)) {
                throw new IllegalStateException("Phòng không ở trạng thái bị ẩn để mở hiển thị.");
            }
            if ("MODERATOR".equalsIgnoreCase(room.getHiddenBy()) || "ADMIN".equalsIgnoreCase(room.getHiddenBy())) {
                throw new IllegalStateException("Bài đăng bị kiểm duyệt viên ẩn do vi phạm quy chuẩn (" + room.getHiddenReason() + "). Bạn không thể tự mở lại.");
            }
            if (room.getPublishedAt() == null) {
                throw new IllegalStateException("Phòng trọ chưa từng được kiểm duyệt duyệt xuất bản.");
            }

            room.setStatus("PUBLISHED");
            room.setHiddenAt(null);
            room.setHiddenBy(null);
            room.setHiddenReason(null);

            roomStatusHistoryRepository.save(RoomStatusHistory.builder()
                    .roomId(room.getId())
                    .fromStatus(fromStatus)
                    .toStatus("PUBLISHED")
                    .actorId(actorId)
                    .actorRole("LANDLORD")
                    .reason("Chủ trọ mở lại bài đăng")
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("Landlord {} un-hid room {}", landlordId, roomId);
        }

        return roomRepository.save(room);
    }

    public void softDeleteRoom(Long landlordId, Long roomId, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        // Safeguard: Do not delete room if in active use (RENTED or active contracts)
        if (roomUsagePort.isRoomInActiveUse(roomId)) {
            throw new IllegalStateException("Không thể xóa phòng đang có khách thuê (RENTED) hoặc đang có hợp đồng còn hiệu lực.");
        }

        String fromStatus = room.getStatus();
        LocalDateTime now = LocalDateTime.now();

        room.setDeletedAt(now);
        room.setStatus("HIDDEN");
        room.setHiddenAt(now);
        room.setHiddenBy("OWNER");
        room.setHiddenReason("Chủ trọ đã xóa bài đăng");

        roomStatusHistoryRepository.save(RoomStatusHistory.builder()
                .roomId(room.getId())
                .fromStatus(fromStatus)
                .toStatus("DELETED")
                .actorId(actorId)
                .actorRole("LANDLORD")
                .reason("Chủ trọ xóa bài đăng")
                .createdAt(now)
                .build());

        roomRepository.save(room);
        log.info("Landlord {} soft-deleted room {}", landlordId, roomId);
    }

    public Room updateRoom(Long landlordId, Long roomId, UpdateRoomRequest req, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        room.setTitle(req.getTitle());
        room.setTitleSearch(LandlordRoomQueryService.removeAccents(req.getTitle().toLowerCase()));
        room.setDescription(req.getDescription());
        room.setAddress(req.getAddress());
        if (req.getProvinceCode() != null) room.setProvinceCode(req.getProvinceCode());
        if (req.getDistrictCode() != null) room.setDistrictCode(req.getDistrictCode());
        if (req.getWardCode() != null) room.setWardCode(req.getWardCode());
        if (req.getWardId() != null) room.setWardId(req.getWardId());

        if (req.getRoomTypeId() != null) room.setRoomTypeId(req.getRoomTypeId());
        if (req.getRoomTypeCode() != null) room.setRoomTypeCode(req.getRoomTypeCode());

        room.setArea(req.getArea());
        if (req.getMaxOccupants() != null) room.setMaxOccupants(req.getMaxOccupants());
        room.setPrice(req.getPrice());
        if (req.getDeposit() != null) room.setDeposit(req.getDeposit());
        if (req.getElectricityCost() != null) room.setElectricityCost(req.getElectricityCost());
        if (req.getWaterCost() != null) room.setWaterCost(req.getWaterCost());
        if (req.getInternetCost() != null) room.setInternetCost(req.getInternetCost());
        if (req.getServiceCost() != null) room.setServiceCost(req.getServiceCost());

        if (req.getAllowPets() != null) room.setAllowPets(req.getAllowPets());
        if (req.getAllowCooking() != null) room.setAllowCooking(req.getAllowCooking());

        room.setUpdatedAt(LocalDateTime.now());

        log.info("Landlord {} updated room {}", landlordId, roomId);
        return roomRepository.save(room);
    }

    public Room createRoom(Long landlordId, CreateRoomRequest req, String actorId, String landlordName, String landlordPhone) {
        String slug = LandlordRoomQueryService.removeAccents(req.getTitle().toLowerCase())
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                + "-" + (System.currentTimeMillis() % 100000);

        // In new architecture, room creation always starts as DRAFT. Images must be uploaded before submitting.
        String initialStatus = "DRAFT";

        Room room = Room.builder()
                .roomUuid(java.util.UUID.randomUUID().toString())
                .landlordId(landlordId)
                .landlordName(landlordName != null ? landlordName : "Chủ trọ #" + landlordId)
                .landlordPhone(landlordPhone != null ? landlordPhone : "0901234567")
                .roomTypeId(req.getRoomTypeId() != null ? req.getRoomTypeId() : 1L)
                .roomTypeCode(req.getRoomTypeCode() != null ? req.getRoomTypeCode() : "MINI_APARTMENT")
                .title(req.getTitle())
                .titleSearch(LandlordRoomQueryService.removeAccents(req.getTitle().toLowerCase()))
                .slug(slug)
                .description(req.getDescription())
                .address(req.getAddress())
                .provinceCode(req.getProvinceCode() != null ? req.getProvinceCode() : "HN")
                .districtCode(req.getDistrictCode())
                .wardCode(req.getWardCode())
                .wardId(req.getWardId())
                .area(req.getArea())
                .maxOccupants(req.getMaxOccupants() != null ? req.getMaxOccupants() : 2)
                .price(req.getPrice())
                .deposit(req.getDeposit() != null ? req.getDeposit() : java.math.BigDecimal.ZERO)
                .electricityCost(req.getElectricityCost())
                .waterCost(req.getWaterCost())
                .internetCost(req.getInternetCost())
                .serviceCost(req.getServiceCost())
                .status(initialStatus)
                .approved(false)
                .allowPets(Boolean.TRUE.equals(req.getAllowPets()))
                .allowCooking(Boolean.TRUE.equals(req.getAllowCooking()))
                .viewCount(0)
                .ratingAvg(0.0)
                .reviewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Room saved = roomRepository.save(room);

        roomStatusHistoryRepository.save(RoomStatusHistory.builder()
                .roomId(saved.getId())
                .fromStatus(null)
                .toStatus(saved.getStatus())
                .actorId(actorId)
                .actorRole("LANDLORD")
                .reason("Lưu bản nháp bài đăng mới")
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Landlord {} created room #{} with status {}", landlordId, saved.getId(), saved.getStatus());
        return saved;
    }

    public Room submitForReview(Long landlordId, Long roomId, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        long imageCount = roomImageRepository.countByRoomId(roomId);
        if (imageCount < minImagesForReview) {
            throw new IllegalStateException("Bài đăng phải có ít nhất " + minImagesForReview + " hình ảnh trước khi gửi duyệt.");
        }

        boolean hasCover = roomImageRepository.findByRoomIdAndCoverTrue(roomId).isPresent();
        if (!hasCover) {
            throw new IllegalStateException("Bài đăng phải có đúng 1 ảnh bìa trước khi gửi duyệt.");
        }

        String fromStatus = room.getStatus();
        room.setStatus("PENDING_REVIEW");
        room.setUpdatedAt(LocalDateTime.now());

        roomStatusHistoryRepository.save(RoomStatusHistory.builder()
                .roomId(room.getId())
                .fromStatus(fromStatus)
                .toStatus("PENDING_REVIEW")
                .actorId(actorId)
                .actorRole("LANDLORD")
                .reason("Chủ trọ gửi duyệt bài đăng")
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Landlord {} submitted room {} for review (images: {}, hasCover: true)", landlordId, roomId, imageCount);
        return roomRepository.save(room);
    }
}

