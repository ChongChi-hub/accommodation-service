package vn.cozynest.identity.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FaceDecisionRequest {
    private String decision; // "PASSED" or "FAILED"
    private String reasonCode; // Standard codes: KHONG_KHOP_GIAY_TO, NGHI_NGO_GIA_MAO, ANH_KEM_CHAT_LUONG, KHAC
    private String note;
}

