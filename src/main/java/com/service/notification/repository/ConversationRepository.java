package com.service.notification.repository;

import com.service.notification.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
    Optional<ConversationEntity> findByTenantIdAndLandlordIdAndRoomId(String tenantId, String landlordId, String roomId);
}

