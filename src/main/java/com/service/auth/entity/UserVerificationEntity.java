package com.service.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVerificationEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, unique = true)
    private String userId;

    @Column(name = "id_card_front", nullable = false, length = 500)
    private String idCardFront;

    @Column(name = "id_card_back", nullable = false, length = 500)
    private String idCardBack;

    @Column(name = "selfie_url", length = 500)
    private String selfieUrl;

    @Column(name = "status", nullable = false)
    private String status; // 'pending', 'approved', 'rejected'

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

