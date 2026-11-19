package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.SaveBankAccountRequest;
import vn.cozynest.billing.application.dto.UserBankAccountDto;
import vn.cozynest.billing.application.service.BankAccountService;
import vn.cozynest.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/billing/bank-account")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserBankAccountDto>> getMyBankAccount(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        UserBankAccountDto dto = bankAccountService.getByUserId(userId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserBankAccountDto>> updateMyBankAccount(
            Authentication auth,
            @Valid @RequestBody SaveBankAccountRequest request) {
        String userId = (String) auth.getPrincipal();
        UserBankAccountDto saved = bankAccountService.saveOrUpdate(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Luu thong tin tai khoan ngan hang va ma QR thanh cong", saved));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserBankAccountDto>> getUserBankAccount(@PathVariable String userId) {
        UserBankAccountDto dto = bankAccountService.getByUserId(userId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}

