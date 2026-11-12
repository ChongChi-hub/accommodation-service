package vn.cozynest.listing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.dto.RoomFilterParams;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.domain.model.DomainPage;
import vn.cozynest.listing.domain.model.Province;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomSearchQuery;
import vn.cozynest.listing.domain.repository.AmenityRepository;
import vn.cozynest.listing.domain.repository.ProvinceRepository;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomQueryServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private vn.cozynest.listing.domain.repository.LandlordSnapshotRepository landlordSnapshotRepository;

    @org.mockito.Spy
    private vn.cozynest.listing.application.service.RoomDtoMapper roomDtoMapper = new vn.cozynest.listing.application.service.RoomDtoMapper();

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

    @Test
    @SuppressWarnings("unchecked")
    void shouldSearchRoomsWithQueryAndProvinceSlugAndSizeCap() {
        Room mockRoom = Room.builder()
                .id(2L)
                .slug("studio-cau-giay-2")
                .roomTypeCode("STUDIO")
                .provinceCode("HN")
                .title("Studio Cầu Giấy")
                .price(new BigDecimal("4200000"))
                .area(28.0)
                .maxOccupants(2)
                .approved(true)
                .status("PUBLISHED")
                .build();

        when(provinceRepository.findAll())
                .thenReturn(List.of(Province.builder().code("HN").name("Hà Nội").slug("ha-noi").build()));
        when(roomRepository.searchRooms(any(RoomSearchQuery.class), eq(0), eq(50), eq("priceAsc")))
                .thenReturn(new DomainPage<>(List.of(mockRoom), 0, 50, 1L));

        RoomFilterParams params = RoomFilterParams.builder()
                .q("cau giay %_")
                .provinceSlug("ha-noi")
                .roomTypeCode("can-ho")
                .priceMax(new BigDecimal("5000000"))
                .sort("priceAsc")
                .page(0)
                .size(100)
                .build();

        Page<RoomDto> page = roomQueryService.searchRooms(params);

        assertEquals(1, page.getTotalElements());
        assertEquals("Hà Nội", page.getContent().get(0).getProvinceName());
        verify(roomRepository).searchRooms(
                argThat((RoomSearchQuery q) -> "ha-noi".equals(q.getProvinceSlug()) && "can-ho".equals(q.getRoomTypeCode())),
                eq(0),
                eq(50),
                eq("priceAsc")
        );
    }
}

