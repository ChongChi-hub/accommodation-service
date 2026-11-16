package vn.cozynest.listing.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUploadRequestDto(
        @NotBlank String filename,
        @NotBlank String contentType,
        long sizeBytes,
        @NotBlank String category,
        String targetId
) {
}
