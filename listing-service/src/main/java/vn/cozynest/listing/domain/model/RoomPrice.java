package vn.cozynest.listing.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPrice {

    private Long id;
    private Long roomId;
    private String label;
    private BigDecimal price;
    private String unit;

    @Builder.Default
    private String billingMode = "METERED";

    @Builder.Default
    private boolean metered = false;

    private String meterType;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
