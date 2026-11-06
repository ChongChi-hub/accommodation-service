package com.service.notification.service;

import com.service.common.dto.PaginationResponse;
import com.service.notification.repository.NotificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JdbcTemplate jdbcTemplate;

    public NotificationService(NotificationRepository notificationRepository, JdbcTemplate jdbcTemplate) {
        this.notificationRepository = notificationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> listNotifications(String userId, boolean unreadOnly, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();

        StringBuilder whereClause = new StringBuilder(" WHERE user_id = ?");
        countParams.add(userId);
        queryParams.add(userId);

        if (unreadOnly) {
            whereClause.append(" AND is_read = 0");
        }

        String countSql = "SELECT COUNT(*) FROM notifications " + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT id, type, title, body, is_read, ref_id, created_at
            FROM notifications
        """ + whereClause + " ORDER BY created_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> notifications = jdbcTemplate.queryForList(selectSql, queryParams.toArray());

        Long unreadCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0",
                Long.class,
                userId
        );

        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", notifications,
                "unreadCount", unreadCount != null ? unreadCount : 0,
                "pagination", pagination
        );
    }

    @Transactional
    public void markAsRead(String id, String userId) {
        jdbcTemplate.update("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?", id, userId);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        jdbcTemplate.update("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }
}

