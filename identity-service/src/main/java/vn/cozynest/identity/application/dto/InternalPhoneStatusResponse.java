package vn.cozynest.identity.application.dto;

public record InternalPhoneStatusResponse(
        String userId,
        boolean phoneVerified,
        String maskedPhone
) {}

