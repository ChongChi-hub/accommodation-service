package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private String token;
    private String refreshToken;
    private UserDto user;
    private List<String> roles;
    private Boolean isNewUser;
    private Boolean hasPassword;
    private LandlordApplicationSummaryDto landlordApplication;
    private Boolean emailVerified;
}
