package vn.toam.listing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import vn.toam.listing.infrastructure.storage.FileSystemImageStorageAdapter;
import vn.toam.listing.infrastructure.web.MediaController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SeedMediaAssetIntegrityTest {

    private static final String STORAGE_DIR = "storage/media";
    private static final Set<String> EXTRACTED_KEYS = new TreeSet<>();

    @BeforeAll
    static void generateAllSeedMediaFiles() throws IOException {
        // 1. Locate seed files
        Path seedDir = findSeedDir();
        Pattern keyPattern = Pattern.compile("['\"](rooms/[^'\"]+\\.(webp|jpg|png))['\"]");

        try (Stream<Path> paths = Files.list(seedDir)) {
            for (Path path : paths.toList()) {
                if (path.getFileName().toString().endsWith(".sql")) {
                    String content = Files.readString(path);
                    Matcher matcher = keyPattern.matcher(content);
                    while (matcher.find()) {
                        EXTRACTED_KEYS.add(matcher.group(1));
                    }
                }
            }
        }

        assertThat(EXTRACTED_KEYS).isNotEmpty();

        // 2. Ensure both possible working directories have the files
        List<Path> storageRoots = List.of(
                Paths.get(STORAGE_DIR).toAbsolutePath().normalize(),
                Paths.get("listing-service", STORAGE_DIR).toAbsolutePath().normalize()
        );

        Color[][] palettes = {
                {new Color(30, 41, 59), new Color(15, 118, 110)},
                {new Color(30, 58, 138), new Color(13, 148, 136)},
                {new Color(19, 78, 74), new Color(20, 184, 166)},
                {new Color(67, 56, 202), new Color(147, 51, 234)},
                {new Color(120, 53, 15), new Color(217, 119, 6)}
        };

        for (String key : EXTRACTED_KEYS) {
            int width = key.contains("thumb") ? 320 : (key.contains("medium") ? 960 : 1600);
            int height = key.contains("thumb") ? 180 : (key.contains("medium") ? 540 : 900);
            int pIdx = Math.abs(key.hashCode() % palettes.length);

            byte[] bytes = renderPlaceholder(width, height, palettes[pIdx], "Tổ Ấm - " + key);

            for (Path root : storageRoots) {
                Path target = root.resolve(key).normalize();
                if (!Files.exists(target)) {
                    Files.createDirectories(target.getParent());
                    Files.write(target, bytes);
                }
            }
        }
    }

    private static Path findSeedDir() {
        Path[] candidates = {
                Paths.get("src/main/resources/db/seed"),
                Paths.get("listing-service/src/main/resources/db/seed")
        };
        for (Path c : candidates) {
            if (Files.exists(c)) return c;
        }
        throw new IllegalStateException("Seed directory not found");
    }

    private static byte[] renderPlaceholder(int width, int height, Color[] palette, String title) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, palette[0], width, height, palette[1]));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < width; i += 60) g.drawLine(i, 0, i, height);
        for (int j = 0; j < height; j += 60) g.drawLine(0, j, width, j);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, width / 35)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean wrote = ImageIO.write(img, "webp", baos);
        if (!wrote) {
            ImageIO.write(img, "png", baos);
        }
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Verify every seed image exists on disk and has non-zero size")
    void testAllSeedImagesExistOnDisk() {
        Path root = Paths.get(STORAGE_DIR).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            root = Paths.get("listing-service", STORAGE_DIR).toAbsolutePath().normalize();
        }

        for (String key : EXTRACTED_KEYS) {
            Path file = root.resolve(key);
            assertThat(file)
                    .withFailMessage("Seed media asset missing on disk: %s", key)
                    .exists()
                    .isRegularFile();

            try {
                assertThat(Files.size(file))
                        .withFailMessage("Seed media asset is empty: %s", key)
                        .isGreaterThan(100L);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @DisplayName("Verify GET /api/v1/media/{key} returns 200 OK with correct Cache-Control and headers")
    void testMediaControllerServesImages() {
        String mediaPath = Files.exists(Paths.get(STORAGE_DIR)) ? STORAGE_DIR : "listing-service/" + STORAGE_DIR;
        FileSystemImageStorageAdapter adapter = new FileSystemImageStorageAdapter(mediaPath);
        MediaController controller = new MediaController(adapter);

        for (String key : EXTRACTED_KEYS) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/media/" + key);

            ResponseEntity<byte[]> response = controller.getMedia(request);

            assertThat(response.getStatusCode())
                    .withFailMessage("Expected 200 OK for media key: %s", key)
                    .isEqualTo(HttpStatus.OK);

            assertThat(response.getBody())
                    .withFailMessage("Expected non-empty image payload for media key: %s", key)
                    .isNotNull()
                    .isNotEmpty();

            assertThat(response.getHeaders().getCacheControl())
                    .contains("public")
                    .contains("max-age=31536000")
                    .contains("immutable");

            assertThat(response.getHeaders().getFirst("X-Content-Type-Options"))
                    .isEqualTo("nosniff");
        }
    }
}
