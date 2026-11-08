package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.listing.domain.port.ImageStoragePort;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Streaming", description = "Phục vụ hình ảnh tĩnh đã qua xử lý chuẩn WebP")
public class MediaController {

    private final ImageStoragePort imageStoragePort;

    @GetMapping("/**")
    @Operation(summary = "Lấy nội dung hình ảnh qua khóa lưu trữ")
    public ResponseEntity<byte[]> getMedia(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Remove prefix "/api/v1/media/"
        String key = uri.replaceFirst("^/api/v1/media/", "");

        byte[] data = imageStoragePort.load(key);
        if (data == null || data.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        MediaType mediaType = MediaType.parseMediaType("image/webp");
        if (key.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (key.endsWith(".jpg") || key.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setCacheControl("public, max-age=31536000, immutable");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}

