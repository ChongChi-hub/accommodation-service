package vn.cozynest.identity.infrastructure.ekyc;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;
import vn.cozynest.identity.domain.port.FaceProviderResult;
import vn.cozynest.identity.domain.port.FaceVerificationProvider;

import java.util.List;

@Component("manualReviewProvider")
public class ManualReviewProvider implements FaceVerificationProvider {

    @Override
    public String getProviderName() {
        return "manual";
    }

    @Override
    public FaceProviderResult verify(FaceVerification verification, List<FaceVerificationFrame> frames) {
        return FaceProviderResult.pendingManualReview();
    }
}

