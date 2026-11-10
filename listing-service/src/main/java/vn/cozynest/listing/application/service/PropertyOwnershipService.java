package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.domain.model.PropertyOwnershipDocument;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.PropertyOwnershipDocumentRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyOwnershipService {

    private final PropertyOwnershipDocumentRepository docRepository;
    private final RoomRepository roomRepository;

    public PropertyOwnershipDocument uploadDocument(Long landlordId, Long roomId, String docType, String fileUrl, String rawNumber) {
        Room room = roomRepository.findByIdAndLandlordIdAndDeletedAtIsNull(roomId, landlordId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + roomId));

        String masked = maskDocumentNumber(rawNumber);
        PropertyOwnershipDocument doc = PropertyOwnershipDocument.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .landlordId(landlordId)
                .docType(docType)
                .fileUrl(fileUrl)
                .maskedNumber(masked)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Landlord {} uploaded ownership doc for room {}", landlordId, roomId);
        return docRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<PropertyOwnershipDocument> getDocumentsByRoom(Long roomId) {
        return docRepository.findByRoomId(roomId);
    }

    public PropertyOwnershipDocument reviewDocument(String docId, String reviewerId, boolean approve, String rejectReason) {
        PropertyOwnershipDocument doc = docRepository.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy giấy tờ sở hữu #" + docId));

        LocalDateTime now = LocalDateTime.now();
        doc.setReviewedAt(now);
        doc.setReviewerId(reviewerId);
        doc.setUpdatedAt(now);

        if (approve) {
            doc.setStatus("APPROVED");
            Room room = roomRepository.findById(doc.getRoomId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng trọ #" + doc.getRoomId()));
            room.setVerifiedOwnership(true);
            room.setVerifiedAt(now);
            roomRepository.save(room);
            log.info("Moderator {} approved ownership doc {} for room {}", reviewerId, docId, doc.getRoomId());
        } else {
            doc.setStatus("REJECTED");
            doc.setRejectReason(rejectReason != null ? rejectReason : "Giấy tờ không hợp lệ hoặc thiếu thông tin");
            log.info("Moderator {} rejected ownership doc {}: {}", reviewerId, docId, doc.getRejectReason());
        }

        return docRepository.save(doc);
    }

    private String maskDocumentNumber(String raw) {
        if (raw == null || raw.length() < 6) return "***";
        int len = raw.length();
        return raw.substring(0, 3) + "***" + raw.substring(len - 3);
    }
}

