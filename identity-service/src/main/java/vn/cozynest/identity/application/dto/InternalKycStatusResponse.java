package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InternalKycStatusResponse {
    private String userId;
    private String kycStatus;
    private boolean isApproved;
}

