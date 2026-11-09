package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.dto.PlatformStatsDto;
import vn.cozynest.listing.application.dto.RoomFacetsDto;
import vn.cozynest.listing.application.service.HomeDataService;
import vn.cozynest.listing.application.service.RoomDtoMapper;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.application.service.RoomSpecificationBuilder;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.CmsArticleRepository;
import vn.cozynest.listing.domain.repository.ProvinceRepository;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedHomeCoverageTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CmsArticleRepository cmsArticleRepository;

    @Spy
    private RoomDtoMapper roomDtoMapper = new RoomDtoMapper();

    @Spy
    private RoomSpecificationBuilder roomSpecificationBuilder = new RoomSpecificationBuilder();

    @InjectMocks
    private RoomQueryService roomQueryService;

    @InjectMocks
    private HomeDataService homeDataService;

    @Test
    @DisplayName("RoomFacetsDto should compute correct counts for all 4 categories")
    void shouldComputeAllFourCategoriesInFacets() {
        when(roomRepository.countActiveByProvince()).thenReturn(List.of(
                new Object[]{"HN", "Hà Nội", 12L},
                new Object[]{"HCM", "TP. Hồ Chí Minh", 10L},
                new Object[]{"DNG", "Đà Nẵng", 8L}
        ));

        when(roomRepository.findActivePriceRange()).thenReturn(List.<Object[]>of(
                new Object[]{new BigDecimal("1200000"), new BigDecimal("15000000")}
        ));

        when(roomRepository.countActiveByAmenity()).thenReturn(List.<Object[]>of(
                new Object[]{1L, "WIFI", "Wifi", 25L}
        ));

        when(roomRepository.countActiveByRoomTypeCode()).thenReturn(List.of(
                new Object[]{"SINGLE", 20L},
                new Object[]{"HOMESTAY", 6L},
                new Object[]{"FULL_HOUSE", 8L},
                new Object[]{"MINI_APARTMENT", 10L},
                new Object[]{"STUDIO", 5L},
                new Object[]{"APARTMENT", 3L},
                new Object[]{"SHARED", 8L}
        ));

        RoomFacetsDto facets = roomQueryService.getFacets();

        assertNotNull(facets);
        assertEquals(3, facets.getProvinces().size());
        assertEquals(new BigDecimal("1200000"), facets.getPriceRange().getMin());
        assertEquals(new BigDecimal("15000000"), facets.getPriceRange().getMax());

        // Check 4 categories
        assertEquals(4, facets.getCategories().size());

        RoomFacetsDto.FacetItemDto phongTro = facets.getCategories().stream().filter(c -> "phong-tro".equals(c.getCode())).findFirst().orElseThrow();
        assertEquals(26L, phongTro.getCount()); // 20 SINGLE + 6 HOMESTAY

        RoomFacetsDto.FacetItemDto nhaNguyenCan = facets.getCategories().stream().filter(c -> "nha-nguyen-can".equals(c.getCode())).findFirst().orElseThrow();
        assertEquals(8L, nhaNguyenCan.getCount()); // 8 FULL_HOUSE

        RoomFacetsDto.FacetItemDto canHo = facets.getCategories().stream().filter(c -> "can-ho".equals(c.getCode())).findFirst().orElseThrow();
        assertEquals(18L, canHo.getCount()); // 10 + 5 + 3

        RoomFacetsDto.FacetItemDto oGhep = facets.getCategories().stream().filter(c -> "o-ghep".equals(c.getCode())).findFirst().orElseThrow();
        assertEquals(8L, oGhep.getCount()); // 8 SHARED
    }

    @Test
    @DisplayName("HomeDataService should return real provincesWithRooms in platform stats")
    void shouldReturnRealProvincesWithRoomsInStats() {
        when(roomRepository.countActiveRooms()).thenReturn(45L);
        when(roomRepository.countActiveProvinces()).thenReturn(8L);
        when(roomRepository.countVerifiedLandlords()).thenReturn(6L);
        when(reviewRepository.count()).thenReturn(30L);

        PlatformStatsDto stats = homeDataService.getPlatformStats();

        assertEquals(45L, stats.getActiveRooms());
        assertEquals(8L, stats.getProvincesWithRooms());
        assertEquals(6L, stats.getVerifiedLandlords());
        assertEquals(30L, stats.getTotalReviews());
    }

    @Test
    @DisplayName("Room and Dto mapper should preserve priceUnit")
    void shouldMapPriceUnitCorrectly() {
        Room roomSingle = Room.builder()
                .id(1L)
                .title("Phòng đơn")
                .price(new BigDecimal("2500000"))
                .priceUnit("ROOM")
                .build();

        Room roomShared = Room.builder()
                .id(2L)
                .title("Phòng ở ghép")
                .price(new BigDecimal("1500000"))
                .priceUnit("BED")
                .build();

        assertEquals("ROOM", roomDtoMapper.toDto(roomSingle).getPriceUnit());
        assertEquals("BED", roomDtoMapper.toDto(roomShared).getPriceUnit());
    }
}
