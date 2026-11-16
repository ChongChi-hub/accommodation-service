package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.listing.application.service.TrustScoreCalculatorService;
import vn.cozynest.listing.domain.model.LandlordSnapshot;
import vn.cozynest.listing.domain.repository.LandlordSnapshotRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustScoreCalculatorServiceTest {

    @Mock
    private LandlordSnapshotRepository landlordSnapshotRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private TrustScoreCalculatorService trustScoreService;

    @Test
    @DisplayName("Verified landlord with excellent metrics gets near-perfect trust score")
    void testComputeScoreVerifiedLandlordHighRating() {
        LandlordSnapshot snapshot = LandlordSnapshot.builder()
                .landlordId(1L)
                .verified(true)
                .responseRatePercent(99)
                .responseTimeMinutes(10)
                .ratingAvg(5.0)
                .reviewCount(30)
                .roomCount(5)
                .build();

        when(roomRepository.countByLandlordIdAndRiskLevelAndDeletedAtIsNull(1L, "HIGH_RISK")).thenReturn(0L);

        int score = trustScoreService.computeScore(snapshot);

        assertThat(score).isGreaterThanOrEqualTo(95);
        assertThat(score).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Unverified landlord with low metrics gets lower trust score")
    void testComputeScoreUnverifiedLowRating() {
        LandlordSnapshot snapshot = LandlordSnapshot.builder()
                .landlordId(2L)
                .verified(false)
                .responseRatePercent(50)
                .responseTimeMinutes(120)
                .ratingAvg(3.0)
                .reviewCount(1)
                .roomCount(1)
                .build();

        when(roomRepository.countByLandlordIdAndRiskLevelAndDeletedAtIsNull(2L, "HIGH_RISK")).thenReturn(0L);

        int score = trustScoreService.computeScore(snapshot);

        assertThat(score).isLessThan(50);
    }

    @Test
    @DisplayName("Landlord with HIGH_RISK rooms receives -25 penalty on trust score")
    void testPenaltyForHighRiskRooms() {
        LandlordSnapshot snapshot = LandlordSnapshot.builder()
                .landlordId(3L)
                .verified(true)
                .responseRatePercent(90)
                .responseTimeMinutes(15)
                .ratingAvg(4.8)
                .reviewCount(10)
                .roomCount(3)
                .build();

        when(roomRepository.countByLandlordIdAndRiskLevelAndDeletedAtIsNull(3L, "HIGH_RISK")).thenReturn(1L);

        int penalizedScore = trustScoreService.computeScore(snapshot);

        // Without penalty: 30 (verified) + 18 (rate) + 5 (time) + 24 (rating) + 10 (reviews) + 10 (quality) = 97
        // With penalty: 97 - 25 = 72
        assertThat(penalizedScore).isLessThan(75);
    }

    @Test
    @DisplayName("calculateAndUpdateTrustScore updates and saves snapshot")
    void testCalculateAndUpdateTrustScore() {
        LandlordSnapshot snapshot = LandlordSnapshot.builder()
                .landlordId(1L)
                .verified(true)
                .responseRatePercent(95)
                .responseTimeMinutes(10)
                .ratingAvg(4.9)
                .reviewCount(20)
                .roomCount(4)
                .build();

        when(landlordSnapshotRepository.findByLandlordId(1L)).thenReturn(Optional.of(snapshot));
        when(roomRepository.countByLandlordIdAndRiskLevelAndDeletedAtIsNull(1L, "HIGH_RISK")).thenReturn(0L);

        int score = trustScoreService.calculateAndUpdateTrustScore(1L);

        assertThat(score).isGreaterThan(90);
        assertThat(snapshot.getTrustScore()).isEqualTo(score);
        verify(landlordSnapshotRepository, times(1)).save(snapshot);
    }
}
