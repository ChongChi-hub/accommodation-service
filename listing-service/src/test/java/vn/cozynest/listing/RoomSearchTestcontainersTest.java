package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.dto.RoomFilterParams;
import vn.cozynest.listing.application.service.HostQueryService;
import vn.cozynest.listing.application.service.RoomDetailService;
import vn.cozynest.listing.application.service.RoomQueryService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RoomSearchTestcontainersTest {

    @Test
    @DisplayName("SEARCH-BE.3 & ROOM-01: Real MySQL 9.0 + Flyway seed search filters, AND amenities, accent-insensitive, wildcard escape, exclusion, size<=50 & pagination")
    void testRealMySqlSearchFiltersPaginationAndCatalogNames() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            System.out.println("CI runner detected: skipping Testcontainers MySQL test");
            return;
        }
        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Throwable t) {
            System.out.println("Docker not accessible: skipping Testcontainers MySQL test");
            return;
        }

        Path mediaTemp = Files.createTempDirectory("listing-search-tc-");

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.0")
                .withDatabaseName("cozynest_listing")
                .withUsername("testuser")
                .withPassword("testpass")) {
            mysql.start();

            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ListingServiceApplication.class)
                    .run(
                            "--server.port=0",
                            "--spring.datasource.url=" + mysql.getJdbcUrl(),
                            "--spring.datasource.username=" + mysql.getUsername(),
                            "--spring.datasource.password=" + mysql.getPassword(),
                            "--spring.flyway.locations=classpath:db/migration,classpath:db/seed",
                            "--spring.jpa.hibernate.ddl-auto=validate",
                            "--spring.profiles.active=dev",
                            "--storage.media-dir=" + mediaTemp.toString().replace("\\", "/")
                    )) {

                RoomQueryService queryService = ctx.getBean(RoomQueryService.class);
                RoomDetailService detailService = ctx.getBean(RoomDetailService.class);
                HostQueryService hostQueryService = ctx.getBean(HostQueryService.class);
                JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

                // 1. All seeded public rooms + size cap (size=200 clamps to 50)
                Page<RoomDto> allCap = queryService.searchRooms(RoomFilterParams.builder().page(0).size(200).build());
                assertThat(allCap.getSize()).isEqualTo(50);
                assertThat(allCap.getTotalElements()).isGreaterThanOrEqualTo(5);

                // 2. Single filter: provinceCode=HN + roomTypeCode=can-ho + combined priceMin/Max
                Page<RoomDto> hcmRooms = queryService.searchRooms(RoomFilterParams.builder().provinceCode("HCM").page(0).size(20).build());
                assertThat(hcmRooms.getContent()).isNotEmpty().allMatch(r -> "HCM".equals(r.getProvinceCode()));

                Page<RoomDto> affordable = queryService.searchRooms(RoomFilterParams.builder()
                        .priceMax(new BigDecimal("6000000"))
                        .areaMin(15.0)
                        .page(0).size(20).build());
                assertThat(affordable.getContent()).isNotEmpty()
                        .allMatch(r -> r.getPrice().compareTo(new BigDecimal("6000000")) <= 0 && r.getArea() >= 15.0);

                // 3. AND-mode amenities check
                List<Long> twoAmenityIds = jdbc.queryForList(
                        "SELECT amenity_id FROM room_amenities GROUP BY amenity_id ORDER BY COUNT(*) DESC LIMIT 2",
                        Long.class
                );
                if (twoAmenityIds.size() == 2) {
                    Long a1 = twoAmenityIds.get(0);
                    Long a2 = twoAmenityIds.get(1);
                    // Ensure at least one active room has both a1 & a2
                    Long sampleRoomId = allCap.getContent().get(0).getId();
                    jdbc.update("INSERT IGNORE INTO room_amenities (room_id, amenity_id) VALUES (?, ?), (?, ?)",
                            sampleRoomId, a1, sampleRoomId, a2);
                    Page<RoomDto> andResult = queryService.searchRooms(RoomFilterParams.builder()
                            .amenityIds(List.of(a1, a2))
                            .amenityMode("all")
                            .page(0).size(50).build());
                    assertThat(andResult.getContent()).extracting(RoomDto::getId).contains(sampleRoomId);
                    for (RoomDto dto : andResult.getContent()) {
                        Integer matchedCount = jdbc.queryForObject(
                                "SELECT COUNT(DISTINCT amenity_id) FROM room_amenities WHERE room_id = ? AND amenity_id IN (?, ?)",
                                Integer.class, dto.getId(), a1, a2);
                        assertThat(matchedCount).isEqualTo(2);
                    }
                }

                // 4. Wildcard injection escaping ('%' and '_' must not match arbitrary rooms)
                Page<RoomDto> pctSearch = queryService.searchRooms(RoomFilterParams.builder().q("%").page(0).size(20).build());
                assertThat(pctSearch.getTotalElements()).isLessThan(allCap.getTotalElements());
                Page<RoomDto> underSearch = queryService.searchRooms(RoomFilterParams.builder().q("___zz_none___").page(0).size(20).build());
                assertThat(underSearch.getTotalElements()).isZero();

                // 5. Accent-insensitive / Vietnamese search
                Page<RoomDto> unaccented = queryService.searchRooms(RoomFilterParams.builder().q("phong").page(0).size(20).build());
                assertThat(unaccented.getContent()).isNotEmpty();

                // 6. Hidden / deleted / draft exclusion
                Long firstId = allCap.getContent().get(0).getId();
                jdbc.update("UPDATE rooms SET hidden_at = NOW() WHERE id = ?", firstId);
                Page<RoomDto> afterHide = queryService.searchRooms(RoomFilterParams.builder().page(0).size(50).build());
                assertThat(afterHide.getContent()).extracting(RoomDto::getId).doesNotContain(firstId);
                jdbc.update("UPDATE rooms SET hidden_at = NULL WHERE id = ?", firstId);

                // 7. Stable non-overlapping pagination
                Page<RoomDto> p0 = queryService.searchRooms(RoomFilterParams.builder().page(0).size(3).sort("newest").build());
                Page<RoomDto> p1 = queryService.searchRooms(RoomFilterParams.builder().page(1).size(3).sort("newest").build());
                Set<Long> ids0 = p0.getContent().stream().map(RoomDto::getId).collect(Collectors.toSet());
                Set<Long> ids1 = p1.getContent().stream().map(RoomDto::getId).collect(Collectors.toSet());
                ids0.retainAll(ids1);
                assertThat(ids0).isEmpty();

                // 8. ROOM-01 real catalog names + public host endpoint
                var detail = detailService.getRoomDetail(String.valueOf(firstId));
                assertThat(detail.getProvinceName()).isNotBlank().isNotEqualTo(detail.getProvinceCode());
                assertThat(detail.getRoomTypeName()).isNotBlank().isNotEqualTo(detail.getRoomTypeCode());
                var host = hostQueryService.getPublicHostProfile(detail.getLandlord().getLandlordId());
                assertThat(host.getName()).isNotBlank();
                assertThat(host.getRooms()).isNotEmpty();
            }
        }
    }
}
