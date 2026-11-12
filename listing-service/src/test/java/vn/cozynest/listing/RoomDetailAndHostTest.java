package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.dto.HostPublicProfileDto;
import vn.cozynest.listing.application.dto.RoomDetailDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.service.HostQueryService;
import vn.cozynest.listing.application.service.RoomDetailService;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.domain.model.*;
import vn.cozynest.listing.domain.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomDetailAndHostTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomPriceRepository roomPriceRepository;
    @Mock
    private LandlordSnapshotRepository landlordSnapshotRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProvinceRepository provinceRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RoomQueryService roomQueryService;

    @Test
    @DisplayName("ROOM-01: RoomDetailService resolves real provinceName and roomTypeName from catalog repositories")
    void shouldResolveProvinceNameAndRoomTypeName() {
        Room room = Room.builder()
                .id(101L)
                .slug("phong-studio-cau-giay")
                .title("Studio Cầu Giấy")
                .provinceCode("HN")
                .roomTypeCode("STUDIO")
                .price(new BigDecimal("4500000"))
                .deposit(new BigDecimal("4500000"))
                .area(28.0)
                .maxOccupants(2)
                .status("AVAILABLE")
                .publicationStatus("PUBLISHED")
                .approved(true)
                .amenities(Set.of())
                .images(Set.of())
                .landlordId(1L)
                .build();

        when(roomRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(room));
        when(roomPriceRepository.findByRoomId(101L)).thenReturn(List.of());
        when(landlordSnapshotRepository.findByLandlordId(1L)).thenReturn(Optional.empty());
        when(reviewRepository.findByRoomIdOrderByCreatedAtDesc(101L)).thenReturn(List.of());
        when(provinceRepository.findByCodeIgnoreCase("HN"))
                .thenReturn(Optional.of(Province.builder().code("HN").name("Thành phố Hà Nội").build()));
        when(roomTypeRepository.findByCodeIgnoreCase("STUDIO"))
                .thenReturn(Optional.of(RoomType.builder().code("STUDIO").name("Căn hộ Studio").build()));

        RoomDetailService service = new RoomDetailService(
                roomRepository, roomPriceRepository, landlordSnapshotRepository,
                reviewRepository, provinceRepository, roomTypeRepository
        );

        RoomDetailDto dto = service.getRoomDetail("101");

        assertThat(dto.getProvinceName()).isEqualTo("Thành phố Hà Nội");
        assertThat(dto.getRoomTypeName()).isEqualTo("Căn hộ Studio");
    }

    @Test
    @DisplayName("ROOM-01: HostQueryService returns safe non-PII public profile and filters out hidden/deleted rooms")
    void shouldReturnSafePublicHostProfileAndPublishedRooms() {
        LandlordSnapshot snapshot = LandlordSnapshot.builder()
                .id(10L)
                .landlordId(101L)
                .name("Trần Minh Tuấn")
                .email("private-secret@cozynest.vn")
                .phone("0902000002")
                .avatarUrl("/uploads/avatars/u-landlord-01.webp")
                .verified(true)
                .ratingAvg(4.9)
                .reviewCount(15)
                .responseRatePercent(98)
                .responseTimeMinutes(12)
                .build();

        Room publishedRoom = Room.builder().id(1L).landlordId(101L).title("Active").approved(true).status("AVAILABLE").publicationStatus("PUBLISHED").build();
        Room hiddenRoom = Room.builder().id(2L).landlordId(101L).title("Hidden").approved(true).status("AVAILABLE").hiddenAt(java.time.LocalDateTime.now()).build();

        when(landlordSnapshotRepository.findByLandlordId(101L)).thenReturn(Optional.of(snapshot));
        when(roomRepository.findByLandlordId(101L, 0, 50)).thenReturn(new DomainPage<>(List.of(publishedRoom, hiddenRoom), 0, 50, 2));
        when(roomQueryService.toRoomDtos(List.of(publishedRoom))).thenReturn(List.of(RoomDto.builder().id(1L).title("Active").build()));

        HostQueryService service = new HostQueryService(landlordSnapshotRepository, roomRepository, roomQueryService);
        HostPublicProfileDto result = service.getPublicHostProfile(101L);

        assertThat(result.getLandlordId()).isEqualTo(101L);
        assertThat(result.getName()).isEqualTo("Trần Minh Tuấn");
        assertThat(result.getRoomCount()).isEqualTo(1);
        assertThat(result.getRooms()).hasSize(1);

        when(landlordSnapshotRepository.findByLandlordId(999L)).thenReturn(Optional.empty());
        when(roomRepository.findByLandlordId(999L, 0, 50)).thenReturn(new DomainPage<>(List.of(), 0, 50, 0));
        assertThatThrownBy(() -> service.getPublicHostProfile(999L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
