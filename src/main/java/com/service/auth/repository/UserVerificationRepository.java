package com.service.auth.repository;

import com.service.auth.entity.UserVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVerificationRepository extends JpaRepository<UserVerificationEntity, String> {
    Optional<UserVerificationEntity> findByUserId(String userId);
    List<UserVerificationEntity> findByStatus(String status);
}

