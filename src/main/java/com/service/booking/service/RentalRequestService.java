package com.service.booking.service;

import com.service.booking.dto.AcceptRentalRequestDto;
import com.service.booking.dto.CreateRentalRequestDto;
import com.service.booking.entity.RentalRequestEntity;
import com.service.booking.repository.RentalRequestRepository;
import com.service.common.dto.PaginationResponse;
import com.service.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RentalRequestService {

    private final RentalRequestRepository requestRepository;
    private final JdbcTemplate jdbcTemplate;

    public RentalRequestService(RentalRequestRepository requestRepository, JdbcTemplate jdbcTemplate) {
        this.requestRepository = requestRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String createRentalRequest(String tenantId, CreateRentalRequestDto request) {
        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(
                "SELECT id, status, landlord_id FROM rooms WHERE id = ?",
                request.getRoomId()
        );
        if (rooms.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng");
        }

        Map<String, Object> room = rooms.get(0);
        if (!"available".equals(room.get("status"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Phòng không còn trống");
        }
        if (tenantId.equals(room.get("landlord_id"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể thuê phòng của chính mình");
        }

        if (requestRepository.findByRoomIdAndTenantIdAndStatus(request.getRoomId(), tenantId, "pending").isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã gửi yêu cầu thuê phòng này");
        }

        String requestId = UUID.randomUUID().toString();
        RentalRequestEntity entity = RentalRequestEntity.builder()
                .id(requestId)
                .roomId(request.getRoomId())
                .tenantId(tenantId)
                .moveInDate(request.getMoveInDate())
                .numPeople(request.getNumPeople() != null ? request.getNumPeople() : 1)
                .message(request.getMessage())
                .status("pending")
                .build();
        requestRepository.save(entity);

        // Notify landlord
        jdbcTemplate.update(
                "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, 'new_request', 'Yêu cầu thuê phòng mới', 'Có người gửi yêu cầu thuê phòng của bạn', ?)",
                UUID.randomUUID().toString(),
                room.get("landlord_id"),
                requestId
        );

        return requestId;
    }

    public Map<String, Object> listRentalRequests(String userId, String userRole, String status, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();

        StringBuilder whereClause = new StringBuilder();
        if ("landlord".equals(userRole)) {
            whereClause.append(" WHERE r.landlord_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        } else {
            whereClause.append(" WHERE rr.tenant_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        }

        if (status != null && !status.isBlank()) {
            whereClause.append(" AND rr.status = ?");
            countParams.add(status);
            queryParams.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM rental_requests rr JOIN rooms r ON rr.room_id = r.id" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT rr.id, rr.room_id, rr.tenant_id, rr.message, rr.move_in_date,
                   rr.num_people, rr.status, rr.contract_id, rr.created_at,
                   r.title as room_title, r.address as room_address, r.price as room_price,
                   r.deposit as deposit_amount, (r.price - r.deposit) as outstanding_amount,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as room_image,
                   u.full_name as tenant_name, u.avatar_url as tenant_avatar, u.phone as tenant_phone,
                   lu.full_name as landlord_name
            FROM rental_requests rr
            JOIN rooms r ON rr.room_id = r.id
            JOIN users u ON rr.tenant_id = u.id
            JOIN users lu ON r.landlord_id = lu.id
        """ + whereClause + " ORDER BY rr.created_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(selectSql, queryParams.toArray());
        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", list,
                "pagination", pagination
        );
    }

    @Transactional
    public String acceptRequest(String requestId, String landlordId, AcceptRentalRequestDto dto) {
        List<Map<String, Object>> requests = jdbcTemplate.queryForList(
                "SELECT rr.*, r.landlord_id, r.price, r.deposit FROM rental_requests rr JOIN rooms r ON rr.room_id = r.id WHERE rr.id = ?",
                requestId
        );

        if (requests.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu");
        }
        Map<String, Object> request = requests.get(0);
        if (!landlordId.equals(request.get("landlord_id"))) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền");
        }
        if (!"pending".equals(request.get("status"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Yêu cầu đã được xử lý");
        }

        LocalDate startDate = dto != null && dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now();
        LocalDate endDate = dto != null && dto.getEndDate() != null ? dto.getEndDate() : startDate.plusMonths(12);
        String terms = dto != null ? dto.getTerms() : null;

        String contractId = UUID.randomUUID().toString();
        String roomId = (String) request.get("room_id");
        String tenantId = (String) request.get("tenant_id");
        BigDecimal price = (BigDecimal) request.get("price");
        BigDecimal deposit = (BigDecimal) request.get("deposit");

        // Insert contract
        jdbcTemplate.update(
                "INSERT INTO contracts (id, room_id, tenant_id, landlord_id, request_id, start_date, end_date, monthly_rent, deposit_amount, terms, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending_sign')",
                contractId, roomId, tenantId, landlordId, requestId, startDate, endDate, price, deposit, terms
        );

        // Update rental request
        jdbcTemplate.update(
                "UPDATE rental_requests SET status = 'accepted', contract_id = ? WHERE id = ?",
                contractId, requestId
        );

        // Reject other pending requests
        jdbcTemplate.update(
                "UPDATE rental_requests SET status = 'rejected' WHERE room_id = ? AND id != ? AND status = 'pending'",
                roomId, requestId
        );

        // Update room status
        jdbcTemplate.update("UPDATE rooms SET status = 'rented' WHERE id = ?", roomId);

        // Notify tenant
        jdbcTemplate.update(
                "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, 'request_accepted', 'Yêu cầu thuê được chấp nhận', 'Chủ nhà đã chấp nhận yêu cầu thuê phòng của bạn', ?)",
                UUID.randomUUID().toString(),
                tenantId,
                contractId
        );

        return contractId;
    }

    @Transactional
    public void rejectRequest(String requestId, String landlordId) {
        List<Map<String, Object>> requests = jdbcTemplate.queryForList(
                "SELECT rr.*, r.landlord_id FROM rental_requests rr JOIN rooms r ON rr.room_id = r.id WHERE rr.id = ?",
                requestId
        );
        if (requests.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu");
        }
        Map<String, Object> req = requests.get(0);
        if (!landlordId.equals(req.get("landlord_id"))) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền");
        }

        jdbcTemplate.update("UPDATE rental_requests SET status = 'rejected' WHERE id = ?", requestId);

        // Notify tenant
        jdbcTemplate.update(
                "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, 'request_rejected', 'Yêu cầu thuê bị từ chối', 'Chủ nhà đã từ chối yêu cầu thuê phòng của bạn', ?)",
                UUID.randomUUID().toString(),
                req.get("tenant_id"),
                requestId
        );
    }

    @Transactional
    public void cancelRequest(String requestId, String tenantId) {
        List<Map<String, Object>> requests = jdbcTemplate.queryForList(
                "SELECT * FROM rental_requests WHERE id = ? AND tenant_id = ?",
                requestId, tenantId
        );
        if (requests.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu");
        }
        Map<String, Object> req = requests.get(0);
        if (!"pending".equals(req.get("status"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy yêu cầu đang chờ");
        }

        jdbcTemplate.update("UPDATE rental_requests SET status = 'cancelled' WHERE id = ?", requestId);
    }
}
