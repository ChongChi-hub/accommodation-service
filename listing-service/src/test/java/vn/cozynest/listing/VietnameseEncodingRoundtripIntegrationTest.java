package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.RoomDetailDto;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.infrastructure.web.RoomController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:5555/cozynest_listing?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci",
        "spring.datasource.username=root",
        "spring.datasource.password=rootpassword123",
        "spring.flyway.enabled=false"
})
class VietnameseEncodingRoundtripIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomController roomController;

    @Test
    @DisplayName("Chuỗi mẫu tiếng Việt có dấu đi vòng DB -> Repo -> API bảo toàn nguyên vẹn UTF-8 không lỗi mã hóa")
    void testVietnameseCharactersRoundtrip() {
        String testTitle = "Hợp đồng thuê điện tử kỳ trước - Đặng Thị Ánh";
        String testCurfew = "Giờ giấc tự do, khóa vân tay riêng biệt 24/7";

        Room room = new Room();
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
        room.setSlug("test-phong-tro-tieng-viet-" + System.currentTimeMillis());

        Room saved = roomRepository.save(room);

        try {
            // 1. Verify DB persistence roundtrip
            Room retrieved = roomRepository.findById(saved.getId()).orElseThrow();
            assertThat(retrieved.getTitle()).isEqualTo(testTitle);
            assertThat(retrieved.getCurfew()).isEqualTo(testCurfew);
            assertThat(retrieved.getTitle()).doesNotContain("?", "\uFFFD");
            assertThat(retrieved.getCurfew()).doesNotContain("?", "\uFFFD");

            // Verify raw byte encoding is UTF-8 multibyte
            byte[] titleBytes = retrieved.getTitle().getBytes(StandardCharsets.UTF_8);
            assertThat(new String(titleBytes, StandardCharsets.UTF_8)).isEqualTo(testTitle);

            // 2. Verify API Controller returns proper UTF-8 data
            ResponseEntity<ApiResponse<RoomDetailDto>> response = roomController.getRoomDetail(saved.getSlug());
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            RoomDetailDto dto = response.getBody().getData();
            assertThat(dto).isNotNull();
            assertThat(dto.getTitle()).isEqualTo(testTitle);
            assertThat(dto.getTitle()).contains("Hợp đồng thuê điện tử kỳ trước");
            assertThat(dto.getTitle()).contains("Đặng Thị Ánh");
            assertThat(dto.getTitle()).doesNotContain("?", "\uFFFD");
        } finally {
            roomRepository.delete(saved);
        }
    }
}
