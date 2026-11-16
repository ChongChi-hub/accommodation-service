package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestDto {

    @NotBlank(message = "Loại đồng thuận không được để trống")
    private String consentType;

    @NotBlank(message = "Phiên bản văn bản không được để trống")
    private String version;

    @Builder.Default
    private boolean agreed = true;

    private String metadataJson;
}
