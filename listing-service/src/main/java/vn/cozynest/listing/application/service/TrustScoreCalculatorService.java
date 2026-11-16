package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.domain.model.LandlordSnapshot;
import vn.cozynest.listing.domain.repository.LandlordSnapshotRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TrustScoreCalculatorService {

    private final LandlordSnapshotRepository landlordSnapshotRepository;
    private final RoomRepository roomRepository;

    public int calculateAndUpdateTrustScore(Long landlordId) {
        Optional<LandlordSnapshot> opt = landlordSnapshotRepository.findByLandlordId(landlordId);
        if (opt.isEmpty()) {
            return 85;
        }

        LandlordSnapshot snapshot = opt.get();
        int score = computeScore(snapshot);
        snapshot.setTrustScore(score);
        landlordSnapshotRepository.save(snapshot);
        log.info("Updated Trust Score for Landlord #{}: {}", landlordId, score);
        return score;
    }

    public int computeScore(LandlordSnapshot snapshot) {
        if (snapshot == null) return 85;

        double total = 0.0;

        // 1. eKYC Identity Verification: 30 pts
        if (snapshot.isVerified()) {
            total += 30.0;
        }

        // 2. Response Performance: up to 25 pts
        int rate = snapshot.getResponseRatePercent() != null ? snapshot.getResponseRatePercent() : 80;
        total += (Math.max(0, Math.min(100, rate)) * 0.20);

        int minutes = snapshot.getResponseTimeMinutes() != null ? snapshot.getResponseTimeMinutes() : 60;
        if (minutes <= 15) {
            total += 5.0;
        } else if (minutes <= 60) {
            total += 3.0;
        }

        // 3. Verified Stay Ratings & Reviews: up to 35 pts
        double rating = snapshot.getRatingAvg() != null ? snapshot.getRatingAvg() : 4.5;
        total += (Math.max(0.0, Math.min(5.0, rating)) / 5.0) * 25.0;

        int reviews = snapshot.getReviewCount() != null ? snapshot.getReviewCount() : 0;
        total += Math.min(10, reviews);

        // 4. Listing Quality Bonus: 10 pts
        if (snapshot.getRoomCount() != null && snapshot.getRoomCount() > 0) {
            total += 10.0;
        }

        // 5. Anti-Fraud Penalty: -25 pts for high risk rooms
        if (snapshot.getLandlordId() != null) {
            long highRiskRooms = roomRepository.countByLandlordIdAndRiskLevelAndDeletedAtIsNull(
                    snapshot.getLandlordId(), "HIGH_RISK");
            if (highRiskRooms > 0) {
                total -= 25.0;
                log.warn("Landlord #{} penalized -25 pts on Trust Score due to {} HIGH_RISK rooms",
                        snapshot.getLandlordId(), highRiskRooms);
            }
        }

        int clamped = (int) Math.round(Math.max(0.0, Math.min(100.0, total)));
        return clamped;
    }
}
