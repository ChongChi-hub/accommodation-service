package vn.cozynest.listing.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.service.PrivateStorageService;
import vn.cozynest.listing.infrastructure.security.LandlordContextResolver;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PrivateStorageController {

    private final PrivateStorageService privateStorageService;
    private final LandlordContextResolver contextResolver;

    @PostMapping("/api/v1/landlord/storage/private-upload")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<PrivateStorageService.UploadResult>> uploadPrivate(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws IOException {
        Long landlordId = contextResolver.getCurrentLandlordId();
        String actorId = contextResolver.getCurrentActorId();
        String role = extractPrimaryRole(auth);
        PrivateStorageService.UploadResult result = privateStorageService.uploadPrivateDoc(
                actorId, role, landlordId != null ? landlordId : 0L,
                file.getOriginalFilename(), file.getBytes(), file.getContentType());
        return ResponseEntity.ok(ApiResponse.success("Tai len tai lieu bao mat thanh cong", result));
    }

    @GetMapping("/api/v1/landlord/storage/signed-url")
    @PreAuthorize("hasAnyRole('LANDLORD', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> issueSignedUrl(
            @RequestParam("objectKey") String objectKey,
            Authentication auth
    ) {
        Long landlordId = contextResolver.getCurrentLandlordId();
        String actorId = contextResolver.getCurrentActorId();
        boolean privileged = hasPrivilegedRole(auth);
        String url = privateStorageService.createSignedUrl(actorId, extractPrimaryRole(auth), landlordId, objectKey, privileged);
        return ResponseEntity.ok(ApiResponse.success(Map.of("signedUrl", url)));
    }

    @GetMapping("/api/v1/storage/private/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadPrivate(
            @RequestParam("token") String token,
            Authentication auth
    ) {
        String actorId = contextResolver.getCurrentActorId();
        boolean privileged = hasPrivilegedRole(auth);
        PrivateStorageService.DownloadedFile file = privateStorageService.downloadByToken(
                actorId, extractPrimaryRole(auth), privileged, token);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String lower = file.objectKey().toLowerCase();
        if (lower.endsWith(".pdf")) mediaType = MediaType.APPLICATION_PDF;
        else if (lower.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mediaType = MediaType.IMAGE_JPEG;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl("private, no-store");
        return ResponseEntity.ok().headers(headers).body(file.bytes());
    }

    private boolean hasPrivilegedRole(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MODERATOR".equals(a.getAuthority()));
    }

    private String extractPrimaryRole(Authentication auth) {
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return "USER";
        }
        return auth.getAuthorities().iterator().next().getAuthority();
    }
}

