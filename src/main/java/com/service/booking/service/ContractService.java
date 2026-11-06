package com.service.booking.service;

import com.service.booking.repository.ContractRepository;
import com.service.common.dto.PaginationResponse;
import com.service.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbcTemplate;

    public ContractService(ContractRepository contractRepository, JdbcTemplate jdbcTemplate) {
        this.contractRepository = contractRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> listContracts(String userId, String userRole, String status, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();

        StringBuilder whereClause = new StringBuilder();
        if ("landlord".equals(userRole)) {
            whereClause.append(" WHERE c.landlord_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        } else if ("admin".equals(userRole)) {
            whereClause.append(" WHERE 1=1");
        } else {
            whereClause.append(" WHERE c.tenant_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        }

        if (status != null && !status.isBlank()) {
            whereClause.append(" AND c.status = ?");
            countParams.add(status);
            queryParams.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM contracts c " + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT c.*,
                   r.title as room_title, r.address as room_address, r.price as room_price,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as room_image,
                   t.full_name as tenant_name, t.avatar_url as tenant_avatar, t.phone as tenant_phone,
                   l.full_name as landlord_name, l.avatar_url as landlord_avatar, l.phone as landlord_phone
            FROM contracts c
            JOIN rooms r ON c.room_id = r.id
            JOIN users t ON c.tenant_id = t.id
            JOIN users l ON c.landlord_id = l.id
        """ + whereClause + " ORDER BY c.created_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(selectSql, queryParams.toArray());
        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", list,
                "pagination", pagination
        );
    }

    public Map<String, Object> getContractById(String contractId, String userId, String userRole) {
        String sql = """
            SELECT c.*,
                   r.title as room_title, r.address as room_address, r.price as room_price, r.area as room_area,
                   t.full_name as tenant_name, t.email as tenant_email, t.phone as tenant_phone,
                   l.full_name as landlord_name, l.email as landlord_email, l.phone as landlord_phone
            FROM contracts c
            JOIN rooms r ON c.room_id = r.id
            JOIN users t ON c.tenant_id = t.id
            JOIN users l ON c.landlord_id = l.id
            WHERE c.id = ?
        """;
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(sql, contractId);
        if (contracts.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng");
        }

        Map<String, Object> contract = contracts.get(0);
        String tenantId = (String) contract.get("tenant_id");
        String landlordId = (String) contract.get("landlord_id");

        if (!"admin".equals(userRole) && !userId.equals(tenantId) && !userId.equals(landlordId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hợp đồng này");
        }

        return contract;
    }

    @Transactional
    public void signContract(String contractId, String userId) {
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList("SELECT * FROM contracts WHERE id = ?", contractId);
        if (contracts.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng");
        }
        Map<String, Object> contract = contracts.get(0);
        String tenantId = (String) contract.get("tenant_id");
        String landlordId = (String) contract.get("landlord_id");

        if (!userId.equals(tenantId) && !userId.equals(landlordId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền");
        }

        jdbcTemplate.update("UPDATE contracts SET status = 'active', signed_at = NOW() WHERE id = ?", contractId);
    }

    @Transactional
    public void terminateContract(String contractId, String userId, String userRole) {
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList("SELECT * FROM contracts WHERE id = ?", contractId);
        if (contracts.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng");
        }
        Map<String, Object> contract = contracts.get(0);
        String landlordId = (String) contract.get("landlord_id");
        String roomId = (String) contract.get("room_id");

        if (!"admin".equals(userRole) && !userId.equals(landlordId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền");
        }

        jdbcTemplate.update("UPDATE contracts SET status = 'terminated' WHERE id = ?", contractId);
        jdbcTemplate.update("UPDATE rooms SET status = 'available' WHERE id = ?", roomId);
    }
}

