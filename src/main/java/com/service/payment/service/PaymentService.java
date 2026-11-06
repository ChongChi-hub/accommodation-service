package com.service.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.common.dto.PaginationResponse;
import com.service.common.exception.AppException;
import com.service.payment.dto.CreateInvoiceRequest;
import com.service.payment.dto.CreatePaymentRequest;
import com.service.payment.dto.CreateUtilityReadingRequest;
import com.service.payment.repository.InvoiceRepository;
import com.service.payment.repository.PaymentRepository;
import com.service.payment.repository.UtilityReadingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PaymentService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            UtilityReadingRepository utilityReadingRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.utilityReadingRepository = utilityReadingRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String createInvoice(CreateInvoiceRequest request) {
        BigDecimal baseRent = request.getBaseRent() != null ? request.getBaseRent() : BigDecimal.ZERO;
        BigDecimal electricFee = request.getElectricFee() != null ? request.getElectricFee() : BigDecimal.ZERO;
        BigDecimal waterFee = request.getWaterFee() != null ? request.getWaterFee() : BigDecimal.ZERO;
        BigDecimal otherFees = request.getOtherFees() != null ? request.getOtherFees() : BigDecimal.ZERO;
        BigDecimal total = baseRent.add(electricFee).add(waterFee).add(otherFees);

        String invoiceId = UUID.randomUUID().toString();

        jdbcTemplate.update("""
            INSERT INTO invoices (id, contract_id, utility_reading_id, period_month, base_rent,
                                  electric_usage, water_usage, electric_fee, water_fee, other_fees, total, due_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'unpaid')
        """,
                invoiceId,
                request.getContractId(),
                request.getUtilityReadingId(),
                request.getPeriodMonth(),
                baseRent,
                request.getElectricUsage() != null ? request.getElectricUsage() : BigDecimal.ZERO,
                request.getWaterUsage() != null ? request.getWaterUsage() : BigDecimal.ZERO,
                electricFee,
                waterFee,
                otherFees,
                total,
                request.getDueDate()
        );

        // Notify tenant
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                "SELECT tenant_id FROM contracts WHERE id = ?", request.getContractId()
        );
        if (!contracts.isEmpty()) {
            String tenantId = (String) contracts.get(0).get("tenant_id");
            jdbcTemplate.update(
                    "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, 'invoice_due', 'Hóa đơn mới', 'Bạn có hóa đơn mới cần thanh toán', ?)",
                    UUID.randomUUID().toString(),
                    tenantId,
                    invoiceId
            );
        }

        return invoiceId;
    }

    public Map<String, Object> listInvoices(String userId, String userRole, String status, String contractId, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if ("tenant".equals(userRole)) {
            conditions.add("c.tenant_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        } else if ("landlord".equals(userRole)) {
            conditions.add("c.landlord_id = ?");
            countParams.add(userId);
            queryParams.add(userId);
        }

        if (status != null && !status.isBlank()) {
            conditions.add("i.status = ?");
            countParams.add(status);
            queryParams.add(status);
        }

        if (contractId != null && !contractId.isBlank()) {
            conditions.add("i.contract_id = ?");
            countParams.add(contractId);
            queryParams.add(contractId);
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM invoices i JOIN contracts c ON i.contract_id = c.id" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT i.*, c.room_id, r.title as room_title, r.address as room_address,
                   t.full_name as tenant_name, l.full_name as landlord_name
            FROM invoices i
            JOIN contracts c ON i.contract_id = c.id
            JOIN rooms r ON c.room_id = r.id
            JOIN users t ON c.tenant_id = t.id
            JOIN users l ON c.landlord_id = l.id
        """ + whereClause + " ORDER BY i.created_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> invoices = jdbcTemplate.queryForList(selectSql, queryParams.toArray());
        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", invoices,
                "pagination", pagination
        );
    }

    public Map<String, Object> getInvoiceById(String id) {
        String sql = """
            SELECT i.*, c.room_id, c.tenant_id, c.landlord_id,
                   r.title as room_title, r.address as room_address,
                   t.full_name as tenant_name, t.phone as tenant_phone,
                   l.full_name as landlord_name, l.phone as landlord_phone
            FROM invoices i
            JOIN contracts c ON i.contract_id = c.id
            JOIN rooms r ON c.room_id = r.id
            JOIN users t ON c.tenant_id = t.id
            JOIN users l ON c.landlord_id = l.id
            WHERE i.id = ?
        """;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, id);
        if (list.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn");
        }
        return list.get(0);
    }

    @Transactional
    public Map<String, String> createPayment(String tenantId, CreatePaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();
        String transactionId = "TXN-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();

        jdbcTemplate.update("""
            INSERT INTO payments (id, invoice_id, tenant_id, amount, method, transaction_id, status)
            VALUES (?, ?, ?, ?, ?, ?, 'success')
        """,
                paymentId,
                request.getInvoiceId(),
                tenantId,
                request.getAmount(),
                request.getMethod(),
                transactionId
        );

        jdbcTemplate.update("UPDATE invoices SET status = 'paid', paid_at = NOW() WHERE id = ?", request.getInvoiceId());

        return Map.of(
                "id", paymentId,
                "transactionId", transactionId
        );
    }

    public Map<String, Object> listPayments(String userId, String userRole, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();

        String whereClause;
        if ("landlord".equals(userRole)) {
            whereClause = " WHERE c.landlord_id = ?";
            countParams.add(userId);
            queryParams.add(userId);
        } else {
            whereClause = " WHERE p.tenant_id = ?";
            countParams.add(userId);
            queryParams.add(userId);
        }

        String countSql = "SELECT COUNT(*) FROM payments p JOIN invoices i ON p.invoice_id = i.id JOIN contracts c ON i.contract_id = c.id" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0;

        String selectSql = """
            SELECT p.*, i.period_month, i.total as invoice_total,
                   r.title as room_title
            FROM payments p
            JOIN invoices i ON p.invoice_id = i.id
            JOIN contracts c ON i.contract_id = c.id
            JOIN rooms r ON c.room_id = r.id
        """ + whereClause + " ORDER BY p.paid_at DESC LIMIT ? OFFSET ?";

        queryParams.add(limit);
        queryParams.add(offset);

        List<Map<String, Object>> payments = jdbcTemplate.queryForList(selectSql, queryParams.toArray());
        PaginationResponse pagination = PaginationResponse.of(page, limit, totalCount);

        return Map.of(
                "data", payments,
                "pagination", pagination
        );
    }

    @Transactional
    public String createUtilityReading(String recordedBy, CreateUtilityReadingRequest request) {
        String readingId = UUID.randomUUID().toString();
        String imagesJson = null;
        if (request.getReadingImages() != null && !request.getReadingImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.getReadingImages());
            } catch (JsonProcessingException e) {
                imagesJson = "[]";
            }
        }

        jdbcTemplate.update("""
            INSERT INTO utility_readings (id, contract_id, period_month, electric_prev, electric_curr,
                                          water_prev, water_curr, reading_images, recorded_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                readingId,
                request.getContractId(),
                request.getPeriodMonth(),
                request.getElectricPrev() != null ? request.getElectricPrev() : BigDecimal.ZERO,
                request.getElectricCurr() != null ? request.getElectricCurr() : BigDecimal.ZERO,
                request.getWaterPrev() != null ? request.getWaterPrev() : BigDecimal.ZERO,
                request.getWaterCurr() != null ? request.getWaterCurr() : BigDecimal.ZERO,
                imagesJson,
                recordedBy
        );

        return readingId;
    }

    public List<Map<String, Object>> listUtilityReadings(String contractId) {
        String sql = """
            SELECT ur.*, u.full_name as recorded_by_name
            FROM utility_readings ur
            JOIN users u ON ur.recorded_by = u.id
            WHERE ur.contract_id = ?
            ORDER BY ur.period_month DESC
        """;
        return jdbcTemplate.queryForList(sql, contractId);
    }
}

