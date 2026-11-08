package vn.cozynest.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    private String id;
    private String contractCode;
    private String roomId;
    private String tenantId;
    private String landlordId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long monthlyRent;
    private Long depositAmount;
    private String status;
    private Long version;
}
