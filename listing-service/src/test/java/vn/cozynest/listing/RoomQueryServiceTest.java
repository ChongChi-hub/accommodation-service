package vn.cozynest.listing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.AmenityRepository;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomQueryServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private RoomQueryService roomQueryService;

    @Test
    void shouldGetRoomById() {
        Room mockRoom = Room.builder()
                .id(1L)
                .title("Phòng trọ tiện nghi")
                .price(new BigDecimal("4500000"))
                .area(30.0)
                .maxOccupants(2)
                .approved(true)
                .status("AVAILABLE")
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));

        RoomDto result = roomQueryService.getRoomById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Phòng trọ tiện nghi", result.getTitle());
        verify(roomRepository, times(1)).findById(1L);
    }
}

