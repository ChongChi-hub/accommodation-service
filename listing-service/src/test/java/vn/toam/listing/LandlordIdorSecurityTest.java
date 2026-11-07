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
class LandlordIdorSecurityTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomStatusHistoryRepository roomStatusHistoryRepository;

    @Mock
    private RoomUsagePort roomUsagePort;

    @InjectMocks
    private LandlordRoomManagementService managementService;

    private static final Long LANDLORD_A = 1L;
    private static final Long LANDLORD_B = 2L;
    private static final Long ROOM_B_ID = 202L;

    private Room roomOfLandlordB;

    @BeforeEach
    void setUp() {
        roomOfLandlordB = Room.builder()
                .id(ROOM_B_ID)
                .landlordId(LANDLORD_B)
                .title("Phòng của chủ trọ B")
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now().minusDays(3))
                .build();
    }

    @Test
    @DisplayName("IDOR Prevention: Landlord A cannot hide or modify Landlord B's room")
    void testLandlordACannotToggleLandlordBRoom() {
        // Query scoped by landlord_id: Landlord A does not own ROOM_B_ID
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(ROOM_B_ID, LANDLORD_A))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                managementService.toggleVisibility(LANDLORD_A, ROOM_B_ID, false, "u-landlord-a"));

        // Verify room of landlord B remains untouched
        assertThat(roomOfLandlordB.getStatus()).isEqualTo("PUBLISHED");
        assertThat(roomOfLandlordB.getHiddenBy()).isNull();
        verify(roomRepository, never()).save(any());
        verify(roomStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR Prevention: Landlord A cannot soft delete Landlord B's room")
    void testLandlordACannotDeleteLandlordBRoom() {
        when(roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(ROOM_B_ID, LANDLORD_A))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                managementService.softDeleteRoom(LANDLORD_A, ROOM_B_ID, "u-landlord-a"));

        assertThat(roomOfLandlordB.getDeletedAt()).isNull();
        verify(roomRepository, never()).save(any());
        verify(roomStatusHistoryRepository, never()).save(any());
    }
}
