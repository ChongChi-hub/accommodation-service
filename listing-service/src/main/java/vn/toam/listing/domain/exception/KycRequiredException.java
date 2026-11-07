package vn.toam.listing.domain.exception;

import lombok.Getter;

@Getter
public class KycRequiredException extends RuntimeException {

    private final String kycStatus;

    public KycRequiredException(String message, String kycStatus) {
        super(message);
        this.kycStatus = kycStatus != null ? kycStatus : "NONE";
    }
}

