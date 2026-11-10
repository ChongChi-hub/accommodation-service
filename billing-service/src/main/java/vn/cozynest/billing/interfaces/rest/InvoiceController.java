package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.InvoiceDto;
import vn.cozynest.billing.application.service.InvoiceService;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDto.InvoiceResponse>> generateInvoice(
            @Valid @RequestBody InvoiceDto.GenerateInvoiceCommand command) {
        InvoiceDto.InvoiceResponse response = invoiceService.generateInvoice(command);
        return ResponseEntity.ok(ApiResponse.success("Phát hành hóa đơn thành công", response));
    }

    @GetMapping("/tenant")
    public ResponseEntity<ApiResponse<List<InvoiceDto.InvoiceResponse>>> getTenantInvoices(Authentication authentication) {
        String tenantId = (String) authentication.getPrincipal();
        List<InvoiceDto.InvoiceResponse> list = invoiceService.getInvoicesForTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/landlord")
    public ResponseEntity<ApiResponse<List<InvoiceDto.InvoiceResponse>>> getLandlordInvoices(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        List<InvoiceDto.InvoiceResponse> list = invoiceService.getInvoicesForLandlord(landlordId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto.InvoiceResponse>> getInvoice(@PathVariable String id) {
        try {
            InvoiceDto.InvoiceResponse invoice = invoiceService.getInvoiceById(id);
            return ResponseEntity.ok(ApiResponse.success(invoice));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy hóa đơn", "ERR_INVOICE_NOT_FOUND"));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelInvoice(@PathVariable String id) {
        try {
            invoiceService.cancelInvoice(id);
            return ResponseEntity.ok(ApiResponse.message("Đã hủy hóa đơn"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_INVOICE_CANCEL_FAILED"));
        }
    }
}

