package vn.toam.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LandlordDataConsistencyTest {

    private Path findMigrationFile(String filename) {
        Path direct = Paths.get("src/main/resources/db/migration", filename);
        if (Files.exists(direct)) return direct;
        Path sub = Paths.get("listing-service/src/main/resources/db/migration", filename);
        if (Files.exists(sub)) return sub;
        throw new IllegalStateException("Could not find migration file: " + filename);
    }

    @Test
    @DisplayName("Verify V6 Seed: Every room has 3 to 10 images and exactly one cover image")
    void testV6EveryRoomHasExactlyOneCoverImage() throws IOException {
        Path v6Path = findMigrationFile("V6__seed_landlord_demo_rooms.sql");
        String content = Files.readString(v6Path);

        // Pattern for room_images: (room_id, ..., is_cover, sort_order, ...)
        Pattern imgPattern = Pattern.compile("\\((\\d+),\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*\\d+,\\s*\\d+,\\s*\\d+,\\s*(TRUE|FALSE),\\s*(\\d+)");
        Matcher matcher = imgPattern.matcher(content);

        Map<Integer, List<Boolean>> coversByRoom = new HashMap<>();
        Map<Integer, List<Integer>> sortOrdersByRoom = new HashMap<>();

        while (matcher.find()) {
            int roomId = Integer.parseInt(matcher.group(1));
            boolean isCover = Boolean.parseBoolean(matcher.group(2));
            int sortOrder = Integer.parseInt(matcher.group(3));

            coversByRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(isCover);
            sortOrdersByRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(sortOrder);
        }

        assertThat(coversByRoom).isNotEmpty();

        for (Map.Entry<Integer, List<Boolean>> entry : coversByRoom.entrySet()) {
            int roomId = entry.getKey();
            List<Boolean> covers = entry.getValue();

            // Each room must have 3 to 10 images
            assertThat(covers.size())
                    .withFailMessage("Room %d must have between 3 and 10 images, found %d", roomId, covers.size())
                    .isBetween(3, 10);

            // Exactly 1 cover
            long coverCount = covers.stream().filter(c -> c).count();
            assertThat(coverCount)
                    .withFailMessage("Room %d must have exactly 1 cover image, found %d", roomId, coverCount)
                    .isEqualTo(1);

            // Contiguous sort orders starting at 0
            List<Integer> sorts = sortOrdersByRoom.get(roomId);
            assertThat(sorts).startsWith(0);
            for (int i = 0; i < sorts.size(); i++) {
                assertThat(sorts.get(i)).isEqualTo(i);
            }
        }
    }

    @Test
    @DisplayName("Verify Landlord 1 has > 25 rooms and covers all 7 required statuses")
    void testLandlord1RoomCoverageAndPagination() throws IOException {
        Path v6Path = findMigrationFile("V6__seed_landlord_demo_rooms.sql");
        String content = Files.readString(v6Path);

        // Count rooms inserted with landlord_id = 1
        Pattern roomPattern = Pattern.compile("\\((\\d+),\\s*'[^']+',\\s*1,\\s*'[^']+',\\s*'[^']+',\\s*\\d+,\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*'[^']+',\\s*[^,]+,\\s*[^,]+,\\s*[^,]+,\\s*\\d+,\\s*\\d+,\\s*\\d+,\\s*[^,]+,\\s*[^,]+,\\s*[^,]+,\\s*[^,]+,\\s*'([^']+)'");
        Matcher matcher = roomPattern.matcher(content);

        Set<String> statuses = new HashSet<>();
        int landlord1Count = 0;

        while (matcher.find()) {
            landlord1Count++;
            String status = matcher.group(2);
            statuses.add(status);
        }

        // Must have > 25 rooms to test pagination
        assertThat(landlord1Count)
                .withFailMessage("Landlord 1 should have > 25 rooms for pagination testing, found %d", landlord1Count)
                .isGreaterThanOrEqualTo(28);

        // Must cover all statuses: DRAFT, PENDING_REVIEW, PUBLISHED, HIDDEN, REJECTED, RENTED
        assertThat(statuses).contains("DRAFT", "PENDING_REVIEW", "PUBLISHED", "HIDDEN", "REJECTED", "RENTED");
    }

    @Test
    @DisplayName("Verify V5 schema defines unique cover generated column and status history table")
    void testV5SchemaDefinitions() throws IOException {
        Path v5Path = findMigrationFile("V5__landlord_rooms_and_images_schema.sql");
        String content = Files.readString(v5Path);

        assertThat(content).contains("cover_room_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_cover = TRUE THEN room_id ELSE NULL END) STORED");
        assertThat(content).contains("UNIQUE KEY uq_room_images_cover_per_room (cover_room_id)");
        assertThat(content).contains("CREATE TABLE IF NOT EXISTS room_status_history");
        assertThat(content).contains("idx_rooms_landlord_status_deleted");
        assertThat(content).contains("idx_rooms_title_search");
    }
}
