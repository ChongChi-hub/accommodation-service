package vn.cozynest.listing.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.cozynest.listing.domain.exception.ValidationException;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetailDto {

    @Builder.Default
    private String type = "about:blank";

    private String title;

    private Integer status;

    private String detail;

    private String instance;

    private List<ValidationException.FieldError> errors;
}

