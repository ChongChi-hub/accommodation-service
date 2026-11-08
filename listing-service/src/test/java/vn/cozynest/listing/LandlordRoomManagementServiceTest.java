package vn.cozynest.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.service.LandlordRoomManagementService;
import vn.cozynest.listing.domain.exception.*;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomStatusHistory;
import vn.cozynest.listing.domain.port.RoomUsagePort;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.domain.repository.RoomStatusHistoryRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandlordRoomManagementServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomStatusHistoryRepository roomStatusHistoryRepository;

    @Mock
    private RoomUsagePort roomUsagePort;

    @Mock
    private vn.cozynest.listing.domain.repository.RoomImageRepository roomImageRepository;

    @Mock
    private vn.cozynest.listing.domain.port.KycVerificationPort kycVerificationPort;

    @InjectMocks
    private LandlordRoomManagementService service;

    private Room publishedRoom;
    private Room hiddenByOwnerRoom;
    private Room hiddenByModRoom;
    private Room rentedRoom;

    @BeforeEach
    void setup() {
        publishedRoom = Room.builder().id(101L).landlordId(1L).title("Phòng Cầu Giấy").status("PUBLISHED").publishedAt(LocalDateTime.now().minusDays(5)).build();
        hiddenByOwnerRoom = Room.builder().id(103L).landlordId(1L).title("Phòng tạm ẩn").status("HIDDEN").hiddenBy("OWNER").publishedAt(LocalDateTime.now().minusDays(10)).build();
        hiddenByModRoom = Room.builder().id(104L).landlordId(1L).title("Phòng bị mod ẩn").status("HIDDEN").hiddenBy("MODERATOR").hiddenReason("Vi phạm an toàn PCCC").publishedAt(LocalDateTime.now().minusDays(20)).build();
        rentedRoom = Room.builder().id(108L).landlordId(1L).title("Phòng đang thuê").status("RENTED").publishedAt(LocalDateTime.now().minusDays(30)).build();
        lenient().when(kycVerificationPort.checkLandlordKyc(any(), any()))
                .thenReturn(vn.cozynest.listing.domain.port.KycVerificationPort.KycVerificationStatus.of("APPROVED", true));
    }

    @Test
    @DisplayName("Hiding a PUBLISHED room succeeds and records status history")
    void testHidePublishedRoom() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(publishedRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        Room result = service.toggleVisibility(1L, 101L, false, "u-landlord-01");

        assertThat(result.getStatus()).isEqualTo("HIDDEN");
        assertThat(result.getHiddenBy()).isEqualTo("OWNER");
        assertThat(result.getHiddenAt()).isNotNull();
        verify(roomStatusHistoryRepository, times(1)).save(any(RoomStatusHistory.class));
    }

    @Test
    @DisplayName("Unhiding a room hidden by OWNER succeeds")
    void testUnhideOwnerRoom() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(103L, 1L)).thenReturn(Optional.of(hiddenByOwnerRoom));
        when(kycVerificationPort.checkLandlordKyc(any(), any()))
                .thenReturn(vn.cozynest.listing.domain.port.KycVerificationPort.KycVerificationStatus.of("APPROVED", true));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        Room result = service.toggleVisibility(1L, 103L, true, "u-landlord-01");

        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        assertThat(result.getHiddenAt()).isNull();
        assertThat(result.getHiddenBy()).isNull();
        verify(roomStatusHistoryRepository, times(1)).save(any(RoomStatusHistory.class));
    }

    @Test
    @DisplayName("Unhiding a room hidden by MODERATOR throws business rule exception")
    void testUnhideModeratorHiddenRoomFails() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(104L, 1L)).thenReturn(Optional.of(hiddenByModRoom));

        RoomModeratedException ex = assertThrows(RoomModeratedException.class, () -> {
            service.toggleVisibility(1L, 104L, true, "u-landlord-01");
        });

        assertThat(ex.getMessage()).contains("kiểm duyệt viên ẩn");
    }

    @Test
    @DisplayName("IDOR Protection: Landlord 2 trying to access Landlord 1's room throws 404 NoSuchElementException")
    void testIdorAccessReturns404() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 2L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            service.toggleVisibility(2L, 101L, false, "u-landlord-02");
        });
    }

    @Test
    @DisplayName("Soft delete room succeeds when not in active use")
    void testSoftDeleteSucceeds() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(publishedRoom));
        when(roomUsagePort.isRoomInActiveUse(101L)).thenReturn(false);

        service.softDeleteRoom(1L, 101L, "u-landlord-01");

        assertThat(publishedRoom.getDeletedAt()).isNotNull();
        verify(roomStatusHistoryRepository, times(1)).save(any(RoomStatusHistory.class));
    }

    @Test
    @DisplayName("Soft delete RENTED room throws business exception")
    void testSoftDeleteRentedRoomFails() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(108L, 1L)).thenReturn(Optional.of(rentedRoom));
        when(roomUsagePort.isRoomInActiveUse(108L)).thenReturn(true);

        RoomDeletionBlockedException ex = assertThrows(RoomDeletionBlockedException.class, () -> {
            service.softDeleteRoom(1L, 108L, "u-landlord-01");
        });

        assertThat(ex.getMessage()).contains("Không thể xóa phòng");
    }

    @Test
    @DisplayName("Submit room for review fails when room has less than 3 images")
    void testSubmitFailsWithLessThanThreeImages() {
        Room draftRoom = Room.builder().id(201L).landlordId(1L).status("DRAFT").build();
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(201L, 1L)).thenReturn(Optional.of(draftRoom));
        when(roomImageRepository.countByRoomId(201L)).thenReturn(2L);

        RoomSubmissionValidationException ex = assertThrows(RoomSubmissionValidationException.class, () -> {
            service.submitForReview(1L, 201L, "u-landlord-01");
        });

        assertThat(ex.getErrorCode()).isEqualTo("ERR_SUBMIT_MIN_IMAGES");
    }

    @Test
    @DisplayName("Submit room for review fails when room has no cover image")
    void testSubmitFailsWithoutCoverImage() {
        Room draftRoom = Room.builder().id(202L).landlordId(1L).status("DRAFT").build();
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(202L, 1L)).thenReturn(Optional.of(draftRoom));
        when(roomImageRepository.countByRoomId(202L)).thenReturn(3L);
        when(roomImageRepository.findByRoomIdAndCoverTrue(202L)).thenReturn(Optional.empty());

        RoomSubmissionValidationException ex = assertThrows(RoomSubmissionValidationException.class, () -> {
            service.submitForReview(1L, 202L, "u-landlord-01");
        });

        assertThat(ex.getErrorCode()).isEqualTo("ERR_SUBMIT_NO_COVER");
    }

    @Test
    @DisplayName("Submit room for review fails when landlord KYC is not approved")
    void testSubmitFailsWhenKycNotApproved() {
        Room draftRoom = Room.builder().id(204L).landlordId(1L).status("DRAFT").build();
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(204L, 1L)).thenReturn(Optional.of(draftRoom));
        when(kycVerificationPort.checkLandlordKyc(any(), any()))
                .thenReturn(vn.cozynest.listing.domain.port.KycVerificationPort.KycVerificationStatus.of("PENDING", false));

        vn.cozynest.listing.domain.exception.KycRequiredException ex = assertThrows(
                vn.cozynest.listing.domain.exception.KycRequiredException.class,
                () -> service.submitForReview(1L, 204L, "u-landlord-01")
        );

        assertThat(ex.getKycStatus()).isEqualTo("PENDING");
        assertThat(ex.getMessage()).contains("xác minh danh tính");
    }

    @Test
    @DisplayName("Submit room for review succeeds when >= 3 images and cover present")
    void testSubmitSucceeds() {
        Room draftRoom = Room.builder().id(203L).landlordId(1L).status("DRAFT").build();
        vn.cozynest.listing.domain.model.RoomImage coverImage = vn.cozynest.listing.domain.model.RoomImage.builder()
                .id(1L).roomId(203L).cover(true).build();

        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(203L, 1L)).thenReturn(Optional.of(draftRoom));
        when(kycVerificationPort.checkLandlordKyc(any(), any()))
                .thenReturn(vn.cozynest.listing.domain.port.KycVerificationPort.KycVerificationStatus.of("APPROVED", true));
        when(roomImageRepository.countByRoomId(203L)).thenReturn(4L);
        when(roomImageRepository.findByRoomIdAndCoverTrue(203L)).thenReturn(Optional.of(coverImage));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        Room submitted = service.submitForReview(1L, 203L, "u-landlord-01");

        assertThat(submitted.getStatus()).isEqualTo("PENDING_REVIEW");
        verify(roomStatusHistoryRepository, times(1)).save(any(RoomStatusHistory.class));
    }

    @Test
    @DisplayName("Creating room always starts in DRAFT status regardless of request flag")
    void testCreateRoomAlwaysStartsInDraft() {
        vn.cozynest.listing.application.dto.CreateRoomRequest req = vn.cozynest.listing.application.dto.CreateRoomRequest.builder()
                .title("Phòng Mới Tạo")
                .address("123 Cầu Giấy")
                .area(25.0)
                .price(java.math.BigDecimal.valueOf(3500000))
                .submitForReview(true)
                .build();

        when(roomRepository.save(any(Room.class))).thenAnswer(i -> {
            Room r = i.getArgument(0);
            r.setId(301L);
            return r;
        });

        Room created = service.createRoom(1L, req, "u-landlord-01", "Chủ trọ", "0901234567");

        assertThat(created.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Updating room with conflicting version throws RoomVersionConflictException")
    void testUpdateRoomVersionConflictThrows() {
        Room r = Room.builder().id(101L).landlordId(1L).version(2L).build();
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(r));

        vn.cozynest.listing.application.dto.UpdateRoomRequest req = vn.cozynest.listing.application.dto.UpdateRoomRequest.builder()
                .title("Cập nhật phòng")
                .address("Địa chỉ mới")
                .area(20.0)
                .price(java.math.BigDecimal.valueOf(3000000))
                .version(1L)
                .build();

        assertThrows(RoomVersionConflictException.class, () ->
                service.updateRoom(1L, 101L, req, "u-landlord-01"));
    }
}

