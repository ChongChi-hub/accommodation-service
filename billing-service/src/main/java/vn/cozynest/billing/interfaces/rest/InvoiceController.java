package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceDto.InvoiceResponse>> generateInvoice(
            @Valid @RequestBody InvoiceDto.GenerateInvoiceCommand command) {
        return ResponseEntity.ok(ApiResponse.success("Phát hành hóa đơn thành công",
                invoiceService.generateInvoice(command)));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceDto.InvoiceResponse>>> generateBatch(
            @Valid @RequestBody InvoiceDto.BatchGenerateCommand command,
            Authentication auth) {
        String actorId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Phát hành hóa đơn hàng loạt thành công",
                invoiceService.generateBatch(command.items(), actorId, isAdmin(auth))));
    }

    @PostMapping("/{id}/credit-notes")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceDto.InvoiceResponse>> issueCreditNote(
            @PathVariable String id,
            @Valid @RequestBody InvoiceDto.CreditNoteCommand command,
            Authentication auth) {
        String actorId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Lập phiếu điều chỉnh giảm thành công",
                invoiceService.issueCreditNote(id, command, actorId, isAdmin(auth))));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvoiceDto.InvoiceResponse>> recordPayment(
            @PathVariable String id,
            @Valid @RequestBody InvoiceDto.RecordPaymentCommand command,
            Authentication auth) {
        String actorId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận thanh toán hóa đơn thành công",
                invoiceService.recordPayment(id, command.amount(), actorId, isAdmin(auth))));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceDto.InvoiceResponse>>> getTenantInvoices(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoicesForTenant((String) auth.getPrincipal())));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceDto.InvoiceResponse>>> getLandlordInvoices(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoicesForLandlord((String) auth.getPrincipal())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvoiceDto.InvoiceResponse>> getInvoice(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.getInvoiceById(id, (String) auth.getPrincipal(), isAdmin(auth))));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelInvoice(@PathVariable String id, Authentication auth) {
        invoiceService.cancelInvoice(id, (String) auth.getPrincipal(), isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.message("Đã hủy hóa đơn"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
    }
}
