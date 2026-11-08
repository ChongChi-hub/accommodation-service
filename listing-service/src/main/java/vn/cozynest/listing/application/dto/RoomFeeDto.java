package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomFeeDto {
    private Long id;
    private Long roomId;
    private String label;
    private BigDecimal price;
    private String unit;
    private String billingMode; // METERED, PER_PERSON, FIXED, INCLUDED
    private boolean metered;
    private String meterType;
}
