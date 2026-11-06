package com.service.admin.service;

import com.service.admin.dto.UpdateUserRoleRequest;
import com.service.common.dto.PaginationResponse;
import com.service.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AdminService {

    private final JdbcTemplate jdbcTemplate;

    public AdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getStats() {
        Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        Long totalRooms = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Long.class);
        Long activeContracts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contracts WHERE status = 'active'", Long.class);
        Long pendingRequests = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rental_requests WHERE status = 'pending'", Long.class);
        Long pendingKyc = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_verifications WHERE status = 'pending'", Long.class);
        Long pendingReports = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reports WHERE status = 'pending'", Long.class);
        BigDecimal totalRevenue = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'success'", BigDecimal.class);

        List<Map<String, Object>> recentUsersRaw = jdbcTemplate.queryForList(
                "SELECT id, full_name, email, role, created_at FROM users ORDER BY created_at DESC LIMIT 5"
        );
        List<Map<String, Object>> recentUsers = new ArrayList<>();
        for (Map<String, Object> u : recentUsersRaw) {
            recentUsers.add(Map.of(
                    "id", u.get("id"),
                    "fullName", u.get("full_name"),
                    "email", u.get("email"),
                    "role", u.get("role"),
                    "createdAt", u.get("created_at")
            ));
        }

        List<Map<String, Object>> recentRoomsRaw = jdbcTemplate.queryForList(
                "SELECT id, title, price, status, created_at FROM rooms ORDER BY created_at DESC LIMIT 5"
        );
        List<Map<String, Object>> recentRooms = new ArrayList<>();
        for (Map<String, Object> r : recentRoomsRaw) {
            recentRooms.add(Map.of(
                    "id", r.get("id"),
                    "title", r.get("title"),
                    "price", r.get("price") != null ? ((BigDecimal) r.get("price")).doubleValue() : 0,
                    "status", r.get("status"),
                    "createdAt", r.get("created_at")
            ));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers != null ? totalUsers : 0);
        stats.put("totalRooms", totalRooms != null ? totalRooms : 0);
        stats.put("activeContracts", activeContracts != null ? activeContracts : 0);
        stats.put("pendingRequests", pendingRequests != null ? pendingRequests : 0);
        stats.put("pendingKyc", pendingKyc != null ? pendingKyc : 0);
        stats.put("pendingReports", pendingReports != null ? pendingReports : 0);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.put("recentUsers", recentUsers);
        stats.put("recentRooms", recentRooms);

        return stats;
    }

    public Map<String, Object> listAllRooms(String status, String search, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            conditions.add("r.status = ?");
            countParams.add(status);
            queryParams.add(status);
        }

        if (search != null && !search.isBlank()) {
            conditions.add("(r.title LIKE ? OR r.address LIKE ?)");
            String s = "%" + search + "%";
            countParams.add(s);
            countParams.add(s);
            queryParams.add(s);
            queryParams.add(s);
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM rooms r" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT r.id, r.title, r.price, r.area, r.address, r.status, r.created_at,
                   u.full_name as landlord_name, rt.name as room_type_name
            FROM rooms r
            JOIN users u ON r.landlord_id = u.id
            JOIN room_types rt ON r.room_type_id = rt.id
        """ + whereClause + " ORDER BY r.created_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(selectSql, queryParams.toArray());
        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", rooms,
                "pagination", pagination
        );
    }

    public Map<String, Object> listAllUsers(String role, String search, String kycStatus, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (role != null && !role.isBlank()) {
            conditions.add("role = ?");
            countParams.add(role);
            queryParams.add(role);
        }

        if (kycStatus != null && !kycStatus.isBlank() && !"all".equalsIgnoreCase(kycStatus)) {
            conditions.add("kyc_status = ?");
            countParams.add(kycStatus);
            queryParams.add(kycStatus);
        }

        if (search != null && !search.isBlank()) {
            conditions.add("(full_name LIKE ? OR email LIKE ? OR phone LIKE ?)");
            String s = "%" + search + "%";
            countParams.add(s);
            countParams.add(s);
            countParams.add(s);
            queryParams.add(s);
            queryParams.add(s);
            queryParams.add(s);
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM users" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT id, full_name, email, phone, avatar_url, role, is_verified, kyc_status, created_at
            FROM users
        """ + whereClause + " ORDER BY created_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> usersRaw = jdbcTemplate.queryForList(selectSql, queryParams.toArray());
        List<Map<String, Object>> users = new ArrayList<>();
        for (Map<String, Object> u : usersRaw) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", u.get("id"));
            userMap.put("fullName", u.get("full_name"));
            userMap.put("email", u.get("email"));
            userMap.put("phone", u.get("phone"));
            userMap.put("avatar", u.get("avatar_url"));
            userMap.put("role", u.get("role"));
            userMap.put("isVerified", u.get("is_verified"));
            userMap.put("kycStatus", u.get("kyc_status"));
            userMap.put("createdAt", u.get("created_at"));
            users.add(userMap);
        }

        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", users,
                "pagination", pagination
        );
    }

    @Transactional
    public void updateUserRole(String id, String role) {
        if (!List.of("tenant", "landlord", "admin").contains(role)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ");
        }
        int updated = jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role, id);
        if (updated == 0) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }
    }

    public Map<String, Object> getUserVerification(String userId) {
        String sql = """
            SELECT uv.*, u.full_name, u.email, u.phone, u.role
            FROM user_verifications uv
            JOIN users u ON uv.user_id = u.id
            WHERE uv.user_id = ?
        """;
        List<Map<String, Object>> verifications = jdbcTemplate.queryForList(sql, userId);
        if (verifications.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ xác thực của người dùng này");
        }
        return verifications.get(0);
    }

    public Object getReportTarget(String type, String id) {
        if ("room".equals(type)) {
            String sql = """
                SELECT r.id, r.title, r.price, r.address, u.full_name as landlord_name
                FROM rooms r
                JOIN users u ON r.landlord_id = u.id
                WHERE r.id = ?
            """;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, id);
            return list.isEmpty() ? null : list.get(0);
        } else if ("user".equals(type)) {
            String sql = "SELECT id, full_name, email, role, phone, avatar_url, kyc_status FROM users WHERE id = ?";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, id);
            if (list.isEmpty()) return null;
            Map<String, Object> u = list.get(0);
            return Map.of(
                    "id", u.get("id"),
                    "fullName", u.get("full_name"),
                    "email", u.get("email"),
                    "role", u.get("role"),
                    "phone", u.get("phone") != null ? u.get("phone") : "",
                    "avatar", u.get("avatar_url") != null ? u.get("avatar_url") : "",
                    "kycStatus", u.get("kyc_status")
            );
        } else if ("review".equals(type)) {
            String sql = """
                SELECT rv.id, rv.rating, rv.comment, rv.created_at,
                       u.full_name as tenant_name, r.title as room_title
                FROM reviews rv
                JOIN users u ON rv.tenant_id = u.id
                JOIN rooms r ON rv.room_id = r.id
                WHERE rv.id = ?
            """;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, id);
            return list.isEmpty() ? null : list.get(0);
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "Loại đối tượng không hợp lệ");
    }
}

