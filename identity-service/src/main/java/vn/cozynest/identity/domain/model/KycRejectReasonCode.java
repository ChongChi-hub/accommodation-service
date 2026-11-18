package vn.cozynest.identity.domain.model;

public enum KycRejectReasonCode {
    BLURRY,
    BLURRY_IMAGE,
    MISMATCH,
    ID_NUMBER_MISMATCH,
    EXPIRED,
    EXPIRED_DOCUMENT,
    INCOMPLETE,
    FAKE_SUSPECTED,
    SUSPECTED_FRAUD,
    UNDERAGE,
    OTHER
}

