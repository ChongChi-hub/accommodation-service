package vn.cozynest.admin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {
    private String id;
    private String disputeCode;
    private String contractId;
    private String roomTitle;
    private String complainantId;
    private String defendantId;
    private String disputeType;
    private Long amount;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
