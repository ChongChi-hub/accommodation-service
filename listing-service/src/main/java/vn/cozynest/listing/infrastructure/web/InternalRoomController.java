package vn.cozynest.listing.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomRepository;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
public class InternalRoomController {

    private static final Logger log = LoggerFactory.getLogger(InternalRoomController.class);
    private final SpringDataRoomRepository roomRepository;

    @PutMapping("/{id}/hold")
    @Transactional
    public ResponseEntity<Map<String, Object>> holdRoom(@PathVariable String id) {
        log.info("Received internal hold request for room: {}", id);
        RoomJpaEntity room = findRoom(id);
        if (room != null) {
            room.setStatus("HOLD");
            room.setOccupancyStatus("HOLD");
            roomRepository.save(room);
            log.info("Room {} placed on HOLD successfully", id);
            return ResponseEntity.ok(Map.of("success", true, "roomId", id, "status", "HOLD"));
        }
        log.warn("Room {} not found for hold request", id);
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/release")
    @Transactional
    public ResponseEntity<Map<String, Object>> releaseRoom(@PathVariable String id) {
        log.info("Received internal release request for room: {}", id);
        RoomJpaEntity room = findRoom(id);
        if (room != null) {
            room.setStatus("AVAILABLE");
            room.setOccupancyStatus("AVAILABLE");
            roomRepository.save(room);
            log.info("Room {} released to AVAILABLE successfully", id);
            return ResponseEntity.ok(Map.of("success", true, "roomId", id, "status", "AVAILABLE"));
        }
        log.warn("Room {} not found for release request", id);
        return ResponseEntity.notFound().build();
    }

    private RoomJpaEntity findRoom(String id) {
        if (id == null || id.isBlank()) return null;
        if (id.matches("\\d+")) {
            return roomRepository.findByIdAndDeletedAtIsNull(Long.parseLong(id)).orElse(null);
        }
        // Try roomUuid
        Optional<RoomJpaEntity> byUuid = roomRepository.findByRoomUuidAndDeletedAtIsNull(id);
        if (byUuid.isPresent()) return byUuid.get();

        // Try slug
        Optional<RoomJpaEntity> bySlug = roomRepository.findBySlugAndDeletedAtIsNull(id);
        if (bySlug.isPresent()) return bySlug.get();

        // Try extracting trailing digits
        Matcher m = Pattern.compile("(\\d+)$").matcher(id);
        if (m.find()) {
            try {
                return roomRepository.findByIdAndDeletedAtIsNull(Long.parseLong(m.group(1))).orElse(null);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

