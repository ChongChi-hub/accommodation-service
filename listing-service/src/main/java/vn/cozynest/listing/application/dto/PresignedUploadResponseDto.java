package vn.cozynest.listing.application.dto;

import java.time.Instant;

public record PresignedUploadResponseDto(
        String uploadId,
        String objectKey,
        String bucket,
        String method,
        String uploadUrl,
        String publicCdnUrl,
        Instant expiresAt,
        boolean isPrivate
) {
}
