package vn.cozynest.identity.interfaces.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.identity.domain.port.AvatarStoragePort;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Serves user avatar media stored in identity-service alongside the default avatar fallback.
 */
@RestController
@RequestMapping({"/uploads/avatars", "/api/v1/media/avatars"})
public class AvatarMediaController {

    private static final Map<String, String> LEGACY_SEED_ALIASES = Map.ofEntries(
            Map.entry("tenant1.jpg", "avatars/u-tenant-01.webp"),
            Map.entry("tenant2.jpg", "avatars/u-tenant-02.webp"),
            Map.entry("landlord1.jpg", "avatars/u-landlord-01.webp"),
            Map.entry("landlord2.jpg", "avatars/u-landlord-02.webp"),
            Map.entry("landlord3.jpg", "avatars/u-landlord-03.webp"),
            Map.entry("landlord4.jpg", "avatars/u-landlord-04.webp"),
            Map.entry("landlord5.jpg", "avatars/u-landlord-05.webp"),
            Map.entry("landlord6.jpg", "avatars/u-landlord-06.webp"),
            Map.entry("mod1.jpg", "avatars/u-mod-01.webp"),
            Map.entry("admin1.jpg", "avatars/u-admin-01.webp")
    );

    private final AvatarStoragePort avatarStorage;
    private volatile byte[] cachedDefaultPng;

    public AvatarMediaController(AvatarStoragePort avatarStorage) {
        this.avatarStorage = avatarStorage;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String primaryKey = "avatars/" + filename;
        byte[] data = avatarStorage.load(primaryKey);
        String resolvedKey = primaryKey;

        if ((data == null || data.length == 0) && LEGACY_SEED_ALIASES.containsKey(filename)) {
            resolvedKey = LEGACY_SEED_ALIASES.get(filename);
            data = avatarStorage.load(resolvedKey);
        }

        if ((data == null || data.length == 0) && "default.png".equals(filename)) {
            data = getOrCreateDefaultAvatarPng();
            resolvedKey = "default.png";
        }

        if (data == null || data.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(resolvedKey));
        headers.setCacheControl("public, max-age=86400");
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    private MediaType resolveMediaType(String key) {
        if (key.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (key.endsWith(".jpg") || key.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.parseMediaType("image/webp");
    }

    private byte[] getOrCreateDefaultAvatarPng() {
        byte[] current = cachedDefaultPng;
        if (current != null) {
            return current;
        }
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.decode("#0F766E"));
            g.fillRect(0, 0, 128, 128);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            FontMetrics fm = g.getFontMetrics();
            String label = "CN";
            int x = (128 - fm.stringWidth(label)) / 2;
            int y = (128 - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(label, x, y);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            current = baos.toByteArray();
            cachedDefaultPng = current;
            return current;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate default avatar PNG", e);
        }
    }
}

