package vn.toam.listing.application.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderImagesRequest {

    @NotEmpty(message = "Danh sách imageIds không được rỗng")
    private List<Long> imageIds;
}

