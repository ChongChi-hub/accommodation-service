package vn.cozynest.listing.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.listing.application.dto.UpdateRoomRequest;
import vn.cozynest.listing.domain.model.PropertyOwnershipDocument;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.port.KycVerificationPort;
import vn.cozynest.listing.domain.port.RoomUsagePort;
import vn.cozynest.listing.domain.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ListingSprint3HardeningTest {

    @Test
    void shouldLockRentedRoomAndResetPublishedRoomOnCriticalChange() {
        RoomRepository roomRepo = mock(RoomRepository.class);
        RoomStatusHistoryRepository histRepo = mock(RoomStatusHistoryRepository.class);
        LandlordRoomManagementService service = new LandlordRoomManagementService(
                roomRepo, histRepo, mock(RoomUsagePort.class), mock(RoomImageRepository.class), mock(KycVerificationPort.class));

        Room rented = Room.builder()
                .id(1L).landlordId(10L).title("Old").address("123 Le Loi")
                .price(new BigDecimal("3000000")).deposit(new BigDecimal("3000000"))
                .maxOccupants(2).minLeaseMonths(6).status("RENTED").approved(true).version(0L)
                .build();
        when(roomRepo.findByIdAndLandlordIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(rented));

        UpdateRoomRequest criticalDepositEdit = UpdateRoomRequest.builder()
                .title("Old").address("123 Le Loi").area(25.0).price(new BigDecimal("3000000"))
                .deposit(new BigDecimal("5000000")).minLeaseMonths(6).version(0L)
                .build();

        assertThatThrownBy(() -> service.updateRoom(10L, 1L, criticalDepositEdit, "u-10"))
                .isInstanceOf(IllegalStateException.class);

        Room published = Room.builder()
                .id(2L).landlordId(10L).title("Pub").address("123 Le Loi")
                .price(new BigDecimal("3000000")).deposit(new BigDecimal("3000000"))
                .maxOccupants(2).minLeaseMonths(6).status("PUBLISHED").approved(false).version(0L)
                .build();
        when(roomRepo.findByIdAndLandlordIdAndDeletedAtIsNull(2L, 10L)).thenReturn(Optional.of(published));
        when(roomRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateRoomRequest priceChange = UpdateRoomRequest.builder()
                .title("Pub").address("123 Le Loi").area(25.0).price(new BigDecimal("3500000"))
                .deposit(new BigDecimal("3000000")).minLeaseMonths(6).version(0L)
                .build();

        Room saved = service.updateRoom(10L, 2L, priceChange, "u-10");
        assertThat(saved.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(saved.isApproved()).isFalse();
    }

    @Test
    void shouldPreventCrossLandlordDocIdorAndRevokeExpiredOwnershipBadge() {
        PropertyOwnershipDocumentRepository docRepo = mock(PropertyOwnershipDocumentRepository.class);
        RoomRepository roomRepo = mock(RoomRepository.class);
        PropertyOwnershipService service = new PropertyOwnershipService(docRepo, roomRepo);

        when(roomRepo.findByIdAndLandlordIdAndDeletedAtIsNull(99L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDocumentsByRoom(99L, 2L, false))
                .isInstanceOf(AccessDeniedException.class);

        Room room = Room.builder().id(99L).landlordId(1L).verifiedOwnership(true).build();
        PropertyOwnershipDocument expiredDoc = PropertyOwnershipDocument.builder()
                .id("doc-1").roomId(99L).landlordId(1L).status("APPROVED")
                .expiresAt(LocalDateTime.now().minusDays(2))
                .build();
        when(docRepo.findByStatusAndExpiresAtBefore(eq("APPROVED"), any())).thenReturn(List.of(expiredDoc));
        when(docRepo.findByRoomId(99L)).thenReturn(List.of(expiredDoc));
        when(roomRepo.findById(99L)).thenReturn(Optional.of(room));

        int revoked = service.revokeExpiredOwnershipBadges();
        assertThat(revoked).isEqualTo(1);
        assertThat(expiredDoc.getStatus()).isEqualTo("EXPIRED");
        assertThat(room.isVerifiedOwnership()).isFalse();
    }
}

