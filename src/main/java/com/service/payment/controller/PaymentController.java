package com.service.payment.controller;

import com.service.common.dto.ApiResponse;
import com.service.payment.dto.CreateInvoiceRequest;
import com.service.payment.dto.CreatePaymentRequest;
import com.service.payment.dto.CreateUtilityReadingRequest;
import com.service.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // --- Invoices ---

    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<Map<String, String>>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {
        String invoiceId = paymentService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo hóa đơn thành công", Map.of("id", invoiceId)));
    }

    @GetMapping("/invoices")
    public ResponseEntity<Map<String, Object>> listInvoices(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contractId
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        Map<String, Object> result = paymentService.listInvoices(userId, role, status, contractId, page, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoiceById(@PathVariable String id) {
        Map<String, Object> invoice = paymentService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    // --- Payments ---

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        String tenantId = (String) authentication.getPrincipal();
        Map<String, String> result = paymentService.createPayment(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thanh toán thành công", result));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listPayments(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        Map<String, Object> result = paymentService.listPayments(userId, role, page, limit);
        return ResponseEntity.ok(result);
    }

    // --- Utility Readings ---

    @PostMapping("/utility-readings")
    public ResponseEntity<ApiResponse<Map<String, String>>> createUtilityReading(
            Authentication authentication,
            @Valid @RequestBody CreateUtilityReadingRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        String readingId = paymentService.createUtilityReading(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ghi chỉ số thành công", Map.of("id", readingId)));
    }

    @GetMapping("/utility-readings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUtilityReadings(
            @RequestParam String contractId
    ) {
        List<Map<String, Object>> readings = paymentService.listUtilityReadings(contractId);
        return ResponseEntity.ok(ApiResponse.success(readings));
    }
}

