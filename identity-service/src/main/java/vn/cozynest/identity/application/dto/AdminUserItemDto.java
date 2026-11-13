package vn.cozynest.identity.application.dto;

import java.time.Instant;

public record AdminUserItemDto(
        String id,
        String name,
        String email,
        String phone,
        String role,
        String roleText,
        String status,
        Instant joinedAt
) {}

