package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomImage;
import vn.cozynest.listing.domain.repository.RoomImageRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.domain.service.AntiFraudRuleEngine;
import vn.cozynest.listing.domain.service.AntiFraudRuleEngine.AntiFraudResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AntiFraudService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;

    public AntiFraudResult evaluateRoom(Room room, List<String> currentRoomPhashes) {
        List<BigDecimal> marketPrices = fetchMarketPrices(room);
        List<String> otherPhashes = roomImageRepository.findOtherRoomImagesWithPhash(room.getId()).stream()
                .map(RoomImage::getPhash)
                .filter(Objects::nonNull)
                .toList();

        AntiFraudResult result = AntiFraudRuleEngine.evaluate(
                room.getPrice(),
                marketPrices,
                room.getTitle(),
                room.getDescription(),
                room.getHouseRules(),
                currentRoomPhashes,
                otherPhashes
        );

        if (result.highRisk()) {
            log.warn("Room #{} flagged as HIGH_RISK: {}", room.getId(), result.reasons());
        }
        return result;
    }

    public Optional<String> checkDuplicateUploadedImage(Long roomId, String candidatePhash) {
        List<String> otherPhashes = roomImageRepository.findOtherRoomImagesWithPhash(roomId).stream()
                .map(RoomImage::getPhash)
                .filter(Objects::nonNull)
                .toList();

        return AntiFraudRuleEngine.checkDuplicatePhash(candidatePhash, otherPhashes);
    }

    private List<BigDecimal> fetchMarketPrices(Room room) {
        List<BigDecimal> wardPrices = List.of();
        if (room.getWardId() != null || room.getWardCode() != null) {
            wardPrices = roomRepository.findPublishedPricesByWard(room.getWardId(), room.getWardCode());
        }
        if (wardPrices.size() >= 3) {
            return wardPrices;
        }
        if (room.getDistrictCode() != null && !room.getDistrictCode().isBlank()) {
            List<BigDecimal> districtPrices = roomRepository.findPublishedPricesByDistrict(room.getDistrictCode());
            if (districtPrices.size() >= 3) {
                return districtPrices;
            }
        }
        return wardPrices;
    }
}
