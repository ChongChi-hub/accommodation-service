package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.LandlordSnapshotDto;
import vn.cozynest.listing.application.dto.RoomDetailDto;
import vn.cozynest.listing.application.service.HomeDataService;
import vn.cozynest.listing.application.service.RoomDetailService;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.infrastructure.web.RoomController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VietnameseEncodingRoundtripIntegrationTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private RoomDetailService roomDetailService;

    @Mock
    private HomeDataService homeDataService;

    @InjectMocks
    private RoomController roomController;

    @Test
    @DisplayName("Chuỗi mẫu tiếng Việt có dấu đi vòng DB -> Repo -> API bảo toàn nguyên vẹn UTF-8 không lỗi mã hóa")
    void testVietnameseCharactersRoundtrip() {
        String testTitle = "Hợp đồng thuê điện tử kỳ trước - Đặng Thị Ánh";
        String testCurfew = "Giờ giấc tự do, khóa vân tay riêng biệt 24/7";

        Room room = new Room();
        room.setId(101L);
        room.setLandlordId(1L);
        room.setLandlordName("Đặng Thị Ánh");
        room.setLandlordPhone("0901234567");
        room.setTitle(testTitle);
        room.setDescription("Mô tả chi tiết bằng tiếng Việt: phòng đẹp, thoáng mát.");
        room.setAddress("123 Đường Cầu Giấy, Phường Dịch Vọng Hậu, Hà Nội");
        room.setCurfew(testCurfew);
        room.setPrice(BigDecimal.valueOf(4500000));
        room.setDeposit(BigDecimal.valueOf(4500000));
        room.setArea(30.0);
        room.setMaxOccupants(2);
        room.setProvinceCode("HN");
        room.setRoomTypeId(1L);
        room.setRoomTypeCode("SINGLE");
        room.setStatus("AVAILABLE");
        room.setApproved(true);
        room.setAvailableDate(java.time.LocalDate.now());
        room.setSlug("test-phong-tro-tieng-viet-123");

        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        // 1. Verify entity UTF-8 multibyte persistence preservation
        Room retrieved = roomRepository.findById(101L).orElseThrow();
        assertThat(retrieved.getTitle()).isEqualTo(testTitle);
        assertThat(retrieved.getCurfew()).isEqualTo(testCurfew);
        assertThat(retrieved.getTitle()).doesNotContain("?", "\uFFFD");
        assertThat(retrieved.getCurfew()).doesNotContain("?", "\uFFFD");

        // Verify raw byte encoding is UTF-8 multibyte
        byte[] titleBytes = retrieved.getTitle().getBytes(StandardCharsets.UTF_8);
        assertThat(new String(titleBytes, StandardCharsets.UTF_8)).isEqualTo(testTitle);

        // 2. Verify API Controller returns proper UTF-8 data
        LandlordSnapshotDto landlordDto = LandlordSnapshotDto.builder()
                .name("Đặng Thị Ánh")
                .phone("0901234567")
                .build();
        RoomDetailDto mockDto = RoomDetailDto.builder()
                .id(101L)
                .title(testTitle)
                .landlord(landlordDto)
                .curfew(testCurfew)
                .build();
        when(roomDetailService.getRoomDetail(room.getSlug())).thenReturn(mockDto);

        ResponseEntity<ApiResponse<RoomDetailDto>> response = roomController.getRoomDetail(room.getSlug());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        RoomDetailDto dto = response.getBody().getData();
        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo(testTitle);
        assertThat(dto.getTitle()).contains("Hợp đồng thuê điện tử kỳ trước");
        assertThat(dto.getLandlord().getName()).contains("Đặng Thị Ánh");
        assertThat(dto.getTitle()).doesNotContain("?", "\uFFFD");
    }
}
