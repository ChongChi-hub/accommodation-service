package vn.toam.listing.domain.port;

public interface KycVerificationPort {

    KycVerificationStatus checkLandlordKyc(String actorId, Long landlordId);

    record KycVerificationStatus(String status, boolean isApproved) {
        public static KycVerificationStatus of(String status, boolean isApproved) {
            return new KycVerificationStatus(status, isApproved);
        }

        public static KycVerificationStatus unverified(String status) {
            return new KycVerificationStatus(status != null ? status : "NONE", false);
        }
    }
}

