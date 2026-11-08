package vn.cozynest.identity.domain.port;

import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;

import java.util.List;

public interface FaceVerificationProvider {
    String getProviderName();
    FaceProviderResult verify(FaceVerification verification, List<FaceVerificationFrame> frames);
}

