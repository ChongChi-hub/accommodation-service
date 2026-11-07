package vn.toam.identity.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.toam.identity.domain.model.KycRejectReasonCode;

@Getter
@Setter
public class RejectKycRequest {

    @NotNull(message = "Ma ly do tu choi la bat buoc")
    private KycRejectReasonCode reasonCode;

    private String reasonNote;
}

