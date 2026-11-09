package vn.cozynest.listing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.dto.HotAreaDto;
import vn.cozynest.listing.application.service.HomeDataService;
import vn.cozynest.listing.domain.repository.BannerRepository;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeDataServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private vn.cozynest.listing.domain.repository.CmsArticleRepository cmsArticleRepository;

    @InjectMocks
    private HomeDataService homeDataService;

    @Test
    void shouldReturnUpToEightHotAreasAndRespectNonEmptyOnly() {
        List<Object[]> rows = List.of(
                new Object[]{"HN", "Hà Nội", "Thành phố Hà Nội", "ha-noi", "/uploads/locations/ha-noi.webp", 27L},
                new Object[]{"HCM", "TP. Hồ Chí Minh", "Thành phố Hồ Chí Minh", "ho-chi-minh", "/uploads/locations/ho-chi-minh.webp", 6L},
                new Object[]{"DNG", "Đà Nẵng", "Thành phố Đà Nẵng", "da-nang", "/uploads/locations/da-nang.webp", 2L},
                new Object[]{"CT", "Cần Thơ", "Thành phố Cần Thơ", "can-tho", "/uploads/locations/can-tho.webp", 1L},
                new Object[]{"HP", "Hải Phòng", "Thành phố Hải Phòng", "hai-phong", "/uploads/locations/hai-phong.webp", 1L},
                new Object[]{"HUE", "Huế", "Thành phố Huế", "hue", null, 0L},
                new Object[]{"BN", "Bắc Ninh", "Tỉnh Bắc Ninh", "bac-ninh", null, 0L},
                new Object[]{"BDG", "Bình Dương", "Tỉnh Bình Dương", "binh-duong", null, 0L},
                new Object[]{"NA", "Nghệ An", "Tỉnh Nghệ An", "nghe-an", null, 0L}
        );

        when(roomRepository.findHotAreas()).thenReturn(rows);

        List<HotAreaDto> eightAreas = homeDataService.getHotAreas();
        assertEquals(8, eightAreas.size());
        assertEquals("HN", eightAreas.get(0).getCode());
        assertEquals(27L, eightAreas.get(0).getRoomCount());
        assertEquals("/uploads/locations/fallback.webp", eightAreas.get(5).getImageUrl());

        List<HotAreaDto> nonEmpty = homeDataService.getHotAreas(8, true);
        assertEquals(5, nonEmpty.size());
    }
}

