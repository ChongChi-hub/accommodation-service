package vn.cozynest.listing.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadOwnershipDocRequest {
    @NotBlank(message = "docType khong duoc de trong")
    private String docType;

    @NotBlank(message = "fileUrl khong duoc de trong")
    private String fileUrl;

    private String rawNumber;
}

