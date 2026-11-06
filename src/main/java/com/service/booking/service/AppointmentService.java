package com.service.booking.service;

import com.service.booking.dto.CreateAppointmentRequest;
import com.service.booking.entity.ViewingAppointmentEntity;
import com.service.booking.repository.ViewingAppointmentRepository;
import com.service.common.dto.PaginationResponse;
import com.service.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AppointmentService {

    private final ViewingAppointmentRepository appointmentRepository;
    private final JdbcTemplate jdbcTemplate;

    public AppointmentService(ViewingAppointmentRepository appointmentRepository, JdbcTemplate jdbcTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String createAppointment(String tenantId, CreateAppointmentRequest request) {
        LocalDateTime appointmentDateTime = LocalDateTime.of(request.getAppointmentDate(), request.getAppointmentTime());
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thời gian hẹn phải ở tương lai");
        }

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(
                "SELECT id, landlord_id, status FROM rooms WHERE id = ?",
                request.getRoomId()
        );
        if (rooms.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng");
        }

        Map<String, Object> room = rooms.get(0);
        if (!"available".equals(room.get("status"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Phòng này hiện không còn trống");
        }
        if (tenantId.equals(room.get("landlord_id"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể tự hẹn lịch xem phòng của chính mình");
        }

        // Check duplicate pending appointment on same date
        if (appointmentRepository.findByRoomIdAndTenantIdAndAppointmentDateAndStatus(
                request.getRoomId(), tenantId, request.getAppointmentDate(), "pending").isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã có lịch hẹn trong ngày này cho phòng này");
        }

        String appointmentId = UUID.randomUUID().toString();
        ViewingAppointmentEntity entity = ViewingAppointmentEntity.builder()
                .id(appointmentId)
                .roomId(request.getRoomId())
                .tenantId(tenantId)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .message(request.getMessage())
                .status("pending")
                .build();
        appointmentRepository.save(entity);

        // Create notification for landlord
        jdbcTemplate.update(
                "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                room.get("landlord_id"),
                "viewing_appointment",
                "Có lịch hẹn xem phòng mới",
                "Có khách đặt lịch hẹn xem phòng của bạn",
                appointmentId
        );

        return appointmentId;
    }

    public Map<String, Object> listAppointments(String userId, String userRole, String status, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();

        StringBuilder whereClause = new StringBuilder();
        if ("landlord".equals(userRole)) {
            whereClause.append(" WHERE r.landlord_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        } else {
            whereClause.append(" WHERE va.tenant_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        }

        if (status != null && !status.isBlank()) {
            whereClause.append(" AND va.status = ?");
            countParams.add(status);
            queryParams.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM viewing_appointments va JOIN rooms r ON va.room_id = r.id" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT va.id, va.room_id, va.tenant_id, va.appointment_date, va.appointment_time,
                   va.message, va.status, va.created_at,
                   r.title as room_title, r.address as room_address,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as room_image,
                   u.full_name as tenant_name, u.phone as tenant_phone, u.email as tenant_email, u.avatar_url as tenant_avatar,
                   lu.full_name as landlord_name, lu.phone as landlord_phone
            FROM viewing_appointments va
            JOIN rooms r ON va.room_id = r.id
            JOIN users u ON va.tenant_id = u.id
            JOIN users lu ON r.landlord_id = lu.id
        """ + whereClause + " ORDER BY va.appointment_date ASC, va.appointment_time ASC LIMIT ? OFFSET ?";

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
    public void confirmAppointment(String appointmentId, String landlordId) {
        List<Map<String, Object>> appts = jdbcTemplate.queryForList(
                "SELECT va.*, r.landlord_id FROM viewing_appointments va JOIN rooms r ON va.room_id = r.id WHERE va.id = ?",
                appointmentId
        );
        if (appts.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn");
        }

        Map<String, Object> appt = appts.get(0);
        if (!landlordId.equals(appt.get("landlord_id"))) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận lịch hẹn này");
        }

        jdbcTemplate.update("UPDATE viewing_appointments SET status = 'confirmed' WHERE id = ?", appointmentId);

        // Notify tenant
        jdbcTemplate.update(
                "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, 'appointment_confirmed', 'Lịch hẹn được xác nhận', 'Chủ trọ đã xác nhận lịch hẹn xem phòng của bạn', ?)",
                UUID.randomUUID().toString(),
                appt.get("tenant_id"),
                appointmentId
        );
    }

    @Transactional
    public void cancelAppointment(String appointmentId, String userId) {
        List<Map<String, Object>> appts = jdbcTemplate.queryForList(
                "SELECT va.*, r.landlord_id FROM viewing_appointments va JOIN rooms r ON va.room_id = r.id WHERE va.id = ?",
                appointmentId
        );
        if (appts.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn");
        }

        Map<String, Object> appt = appts.get(0);
        String tenantId = (String) appt.get("tenant_id");
        String landlordId = (String) appt.get("landlord_id");

        if (!userId.equals(tenantId) && !userId.equals(landlordId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy lịch hẹn này");
        }

        if (!"pending".equals(appt.get("status"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy lịch hẹn đang chờ");
        }

        jdbcTemplate.update("UPDATE viewing_appointments SET status = 'cancelled' WHERE id = ?", appointmentId);
    }
}
