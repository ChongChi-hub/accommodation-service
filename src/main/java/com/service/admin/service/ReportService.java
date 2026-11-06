package com.service.admin.service;

import com.service.admin.dto.CreateReportRequest;
import com.service.admin.repository.ReportRepository;
import com.service.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReportService(ReportRepository reportRepository, JdbcTemplate jdbcTemplate) {
        this.reportRepository = reportRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createReport(String reporterId, CreateReportRequest request) {
        jdbcTemplate.update("""
            INSERT INTO reports (id, reporter_id, target_type, target_id, reason, description, status)
            VALUES (?, ?, ?, ?, ?, ?, 'pending')
        """,
                UUID.randomUUID().toString(),
                reporterId,
                request.getTargetType(),
                request.getTargetId(),
                request.getReason(),
                request.getDescription()
        );
    }

    public List<Map<String, Object>> listReports(String status, String targetType) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            conditions.add("rpt.status = ?");
            params.add(status);
        }
        if (targetType != null && !targetType.isBlank()) {
            conditions.add("rpt.target_type = ?");
            params.add(targetType);
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String sql = """
            SELECT rpt.*, u.full_name as reporter_name, u.email as reporter_email
            FROM reports rpt
            JOIN users u ON rpt.reporter_id = u.id
        """ + whereClause + " ORDER BY rpt.created_at DESC";

        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Transactional
    public void updateReportStatus(String id, String status) {
        if (!List.of("resolved", "dismissed").contains(status)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ");
        }
        int updated = jdbcTemplate.update("UPDATE reports SET status = ? WHERE id = ?", status, id);
        if (updated == 0) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo");
        }
    }
}

