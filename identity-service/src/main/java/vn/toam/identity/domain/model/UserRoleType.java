package vn.toam.identity.domain.model;

public enum UserRoleType {
    TENANT,
    LANDLORD,
    MODERATOR,
    ADMIN;

    public static UserRoleType fromString(String role) {
        if (role == null) return TENANT;
        try {
            return UserRoleType.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TENANT;
        }
    }
}

