package vn.cozynest.identity.application.dto;

public record InternalUserVerificationStatusResponse(
        String userId,
        boolean emailVerified,
        boolean kycVerified,
        boolean faceVerified,
        String role
) {}
