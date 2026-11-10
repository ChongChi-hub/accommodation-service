package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.CreateRoomRequest;
import vn.cozynest.listing.application.dto.UpdateRoomRequest;
import vn.cozynest.listing.domain.exception.*;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomStatusHistory;
import vn.cozynest.listing.domain.port.KycVerificationPort;
import vn.cozynest.listing.domain.port.RoomUsagePort;
import vn.cozynest.listing.domain.repository.RoomImageRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.domain.repository.RoomStatusHistoryRepository;

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
    private final RoomImageRepository roomImageRepository;
    private final KycVerificationPort kycVerificationPort;

    @Value("${toam.listing.min-images-for-review:3}")
    private int minImagesForReview = 3;

    public Room toggleVisibility(Long landlordId, Long roomId, boolean visible, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        String fromStatus = room.getStatus();

        if (!visible) {
            if (!"PUBLISHED".equalsIgnoreCase(fromStatus)) {
                throw new IllegalStateException("Chỉ có thể tạm ẩn bài đăng khi phòng đang ở trạng thái đã duyệt (PUBLISHED).");
            }
            room.setStatus("HIDDEN");
            room.setHiddenAt(LocalDateTime.now());
            room.setHiddenBy("OWNER");
            room.setHiddenReason("Chủ trọ chủ động tạm ẩn bài đăng");
            recordHistory(room.getId(), fromStatus, "HIDDEN", actorId, "Chủ trọ chủ động tạm ẩn bài đăng", LocalDateTime.now());
            log.info("Landlord {} hid room {}", landlordId, roomId);
        } else {
            if (!"HIDDEN".equalsIgnoreCase(fromStatus)) {
                throw new IllegalStateException("Phòng không ở trạng thái bị ẩn để mở hiển thị.");
            }
            if ("MODERATOR".equalsIgnoreCase(room.getHiddenBy()) || "ADMIN".equalsIgnoreCase(room.getHiddenBy())) {
                throw new RoomModeratedException("Bài đăng bị kiểm duyệt viên ẩn do vi phạm quy chuẩn. Bạn không thể tự mở lại.");
            }
            if (room.getPublishedAt() == null) {
                throw new IllegalStateException("Phòng trọ chưa từng được kiểm duyệt duyệt xuất bản.");
            }

            // KYC gate check
            KycVerificationPort.KycVerificationStatus kyc = kycVerificationPort.checkLandlordKyc(actorId, landlordId);
            if (!kyc.isApproved()) {
                throw new KycRequiredException("Chủ trọ cần hoàn tất xác minh danh tính (KYC) để mở lại hiển thị phòng.", kyc.status());
            }

            room.setStatus("PUBLISHED");
            room.setHiddenAt(null);
            room.setHiddenBy(null);
            room.setHiddenReason(null);
            recordHistory(room.getId(), fromStatus, "PUBLISHED", actorId, "Chủ trọ mở lại bài đăng", LocalDateTime.now());
            log.info("Landlord {} un-hid room {}", landlordId, roomId);
        }

        return roomRepository.save(room);
    }

    public void softDeleteRoom(Long landlordId, Long roomId, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        if (roomUsagePort.isRoomInActiveUse(roomId)) {
            throw new RoomDeletionBlockedException("Không thể xóa phòng: Phòng đang có khách thuê, có hợp đồng hiệu lực hoặc có lịch hẹn xem phòng đã xác nhận trong tương lai.");
        }

        LocalDateTime now = LocalDateTime.now();
        String fromStatus = room.getStatus();
        room.setDeletedAt(now);
        room.setStatus("HIDDEN");
        room.setHiddenAt(now);
        room.setHiddenBy("OWNER");
        room.setHiddenReason("Chủ trọ đã xóa bài đăng");

        recordHistory(room.getId(), fromStatus, "DELETED", actorId, "Chủ trọ xóa bài đăng", now);
        roomRepository.save(room);
        log.info("Landlord {} soft-deleted room {}", landlordId, roomId);
    }

    public Room updateRoom(Long landlordId, Long roomId, UpdateRoomRequest req, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        if (req.getVersion() != null && !req.getVersion().equals(room.getVersion())) {
            throw new RoomVersionConflictException("Dữ liệu phòng đã bị thay đổi bởi phiên làm việc khác. Vui lòng tải lại trang.");
        }

        if (room.isRented()) {
            boolean criticalChanged = (req.getPrice() != null && req.getPrice().compareTo(room.getPrice()) != 0)
                    || (req.getAddress() != null && !req.getAddress().equalsIgnoreCase(room.getAddress()));
            if (criticalChanged) {
                throw new IllegalStateException("Không thể sửa đổi giá thuê hoặc địa chỉ khi phòng đang ở trạng thái RENTED.");
            }
        }

        boolean criticalChanged = (req.getPrice() != null && req.getPrice().compareTo(room.getPrice()) != 0)
                || (req.getAddress() != null && !req.getAddress().equalsIgnoreCase(room.getAddress()));
        if (room.isApproved() && criticalChanged) {
            String fromStatus = room.getStatus();
            room.setApproved(false);
            room.setStatus("PENDING_REVIEW");
            recordHistory(room.getId(), fromStatus, "PENDING_REVIEW", actorId,
                    "Hệ thống tự động chuyển sang chờ duyệt lại do thay đổi thông tin quan trọng", LocalDateTime.now());
            log.info("Room {} reset to PENDING_REVIEW due to critical change", roomId);
        }

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
        if (req.getPriceUnit() != null && !req.getPriceUnit().isBlank()) room.setPriceUnit(req.getPriceUnit());
        if (req.getDeposit() != null) room.setDeposit(req.getDeposit());
        if (req.getElectricityCost() != null) room.setElectricityCost(req.getElectricityCost());
        if (req.getWaterCost() != null) room.setWaterCost(req.getWaterCost());
        if (req.getInternetCost() != null) room.setInternetCost(req.getInternetCost());
        if (req.getServiceCost() != null) room.setServiceCost(req.getServiceCost());
        if (req.getAllowPets() != null) room.setAllowPets(req.getAllowPets());
        if (req.getAllowCooking() != null) room.setAllowCooking(req.getAllowCooking());
        if (req.getHouseRules() != null) room.setHouseRules(req.getHouseRules());
        if (req.getAvailableDate() != null) room.setAvailableDate(req.getAvailableDate());
        if (req.getMinLeaseMonths() != null) room.setMinLeaseMonths(req.getMinLeaseMonths());
        room.setUpdatedAt(LocalDateTime.now());

        log.info("Landlord {} updated room {}", landlordId, roomId);
        return roomRepository.save(room);
    }

    public Room createRoom(Long landlordId, CreateRoomRequest req, String actorId, String landlordName, String landlordPhone) {
        String slug = LandlordRoomQueryService.removeAccents(req.getTitle().toLowerCase())
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                + "-" + (System.currentTimeMillis() % 100000);

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
                .priceUnit(req.getPriceUnit() != null && !req.getPriceUnit().isBlank() ? req.getPriceUnit() : "ROOM")
                .deposit(req.getDeposit() != null ? req.getDeposit() : java.math.BigDecimal.ZERO)
                .electricityCost(req.getElectricityCost())
                .waterCost(req.getWaterCost())
                .internetCost(req.getInternetCost())
                .serviceCost(req.getServiceCost())
                .status("DRAFT")
                .approved(false)
                .allowPets(Boolean.TRUE.equals(req.getAllowPets()))
                .allowCooking(Boolean.TRUE.equals(req.getAllowCooking()))
                .houseRules(req.getHouseRules())
                .availableDate(req.getAvailableDate() != null ? req.getAvailableDate() : java.time.LocalDate.now())
                .minLeaseMonths(req.getMinLeaseMonths() != null ? req.getMinLeaseMonths() : 6)
                .viewCount(0)
                .ratingAvg(0.0)
                .reviewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Room saved = roomRepository.save(room);
        recordHistory(saved.getId(), null, saved.getStatus(), actorId, "Lưu bản nháp bài đăng mới", LocalDateTime.now());
        log.info("Landlord {} created room #{} with status {}", landlordId, saved.getId(), saved.getStatus());
        return saved;
    }

    public Room submitForReview(Long landlordId, Long roomId, String actorId) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        // KYC Gate check
        KycVerificationPort.KycVerificationStatus kyc = kycVerificationPort.checkLandlordKyc(actorId, landlordId);
        if (!kyc.isApproved()) {
            throw new KycRequiredException("Bạn cần hoàn tất xác minh danh tính chủ trọ (KYC) để gửi duyệt bài đăng.", kyc.status());
        }

        long imageCount = roomImageRepository.countByRoomId(roomId);
        if (imageCount < minImagesForReview) {
            throw new RoomSubmissionValidationException("Bài đăng phải có ít nhất " + minImagesForReview + " hình ảnh trước khi gửi duyệt.", "ERR_SUBMIT_MIN_IMAGES");
        }

        boolean hasCover = roomImageRepository.findByRoomIdAndCoverTrue(roomId).isPresent();
        if (!hasCover) {
            throw new RoomSubmissionValidationException("Bài đăng phải có đúng 1 ảnh bìa trước khi gửi duyệt.", "ERR_SUBMIT_NO_COVER");
        }

        String fromStatus = room.getStatus();
        room.setStatus("PENDING_REVIEW");
        room.setUpdatedAt(LocalDateTime.now());

        recordHistory(room.getId(), fromStatus, "PENDING_REVIEW", actorId, "Chủ trọ gửi duyệt bài đăng", LocalDateTime.now());
        log.info("Landlord {} submitted room {} for review (images: {}, hasCover: true)", landlordId, roomId, imageCount);
        return roomRepository.save(room);
    }

    private void recordHistory(Long roomId, String from, String to, String actorId, String reason, LocalDateTime time) {
        roomStatusHistoryRepository.save(RoomStatusHistory.builder()
                .roomId(roomId)
                .fromStatus(from)
                .toStatus(to)
                .actorId(actorId)
                .actorRole("LANDLORD")
                .reason(reason)
                .createdAt(time)
                .build());
    }
}
