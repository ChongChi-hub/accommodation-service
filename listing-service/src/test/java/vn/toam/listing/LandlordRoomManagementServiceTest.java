package vn.toam.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.toam.listing.application.service.LandlordRoomManagementService;
import vn.toam.listing.domain.model.Room;
import vn.toam.listing.domain.model.RoomStatusHistory;
import vn.toam.listing.domain.port.RoomUsagePort;
import vn.toam.listing.domain.repository.RoomRepository;
import vn.toam.listing.domain.repository.RoomStatusHistoryRepository;

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

    @InjectMocks
    private LandlordRoomManagementService service;

    private Room publishedRoom;
    private Room hiddenByOwnerRoom;
    private Room hiddenByModRoom;
    private Room rentedRoom;

    @BeforeEach
    void setup() {
        publishedRoom = Room.builder()
                .id(101L)
                .landlordId(1L)
                .title("Phòng Cầu Giấy")
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now().minusDays(5))
                .build();

        hiddenByOwnerRoom = Room.builder()
                .id(103L)
                .landlordId(1L)
                .title("Phòng tạm ẩn")
                .status("HIDDEN")
                .hiddenBy("OWNER")
                .publishedAt(LocalDateTime.now().minusDays(10))
                .build();

        hiddenByModRoom = Room.builder()
                .id(104L)
                .landlordId(1L)
                .title("Phòng bị mod ẩn")
                .status("HIDDEN")
                .hiddenBy("MODERATOR")
                .hiddenReason("Vi phạm an toàn PCCC")
                .publishedAt(LocalDateTime.now().minusDays(20))
                .build();

        rentedRoom = Room.builder()
                .id(108L)
                .landlordId(1L)
                .title("Phòng đang thuê")
                .status("RENTED")
                .publishedAt(LocalDateTime.now().minusDays(30))
                .build();
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

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            service.toggleVisibility(1L, 104L, true, "u-landlord-01");
        });

        assertThat(ex.getMessage()).contains("Bài đăng bị kiểm duyệt viên ẩn");
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

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            service.softDeleteRoom(1L, 108L, "u-landlord-01");
        });

        assertThat(ex.getMessage()).contains("Không thể xóa phòng đang có khách thuê");
    }
}

