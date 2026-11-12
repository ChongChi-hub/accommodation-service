package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.domain.model.RiskProfile;
import vn.cozynest.billing.domain.repository.RiskProfileRepository;
import vn.cozynest.common.dto.ApiResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/risk")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRiskController {

    private final RiskProfileRepository riskProfileRepository;

    public AdminRiskController(RiskProfileRepository riskProfileRepository) {
        this.riskProfileRepository = riskProfileRepository;
    }

    @GetMapping("/profiles")
    public ResponseEntity<ApiResponse<List<RiskProfile>>> listProfiles() {
        return ResponseEntity.ok(ApiResponse.success(riskProfileRepository.findAll()));
    }

    @PostMapping("/{userId}/freeze")
    public ResponseEntity<ApiResponse<RiskProfile>> freeze(
            @PathVariable String userId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Dong bang rui ro tai chinh") : "Dong bang rui ro tai chinh";
        RiskProfile profile = riskProfileRepository.findByUserId(userId)
                .orElseGet(() -> RiskProfile.createDefault(userId));
        profile.freeze(reason);
        return ResponseEntity.ok(ApiResponse.success("Da dong bang tai khoan rut tien", riskProfileRepository.save(profile)));
    }

    @PostMapping("/{userId}/unfreeze")
    public ResponseEntity<ApiResponse<RiskProfile>> unfreeze(@PathVariable String userId) {
        RiskProfile profile = riskProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so rui ro: " + userId));
        profile.unfreeze();
        return ResponseEntity.ok(ApiResponse.success("Da mo khoa tai khoan", riskProfileRepository.save(profile)));
    }

    @PostMapping("/{userId}/blacklist")
    public ResponseEntity<ApiResponse<RiskProfile>> blacklist(
            @PathVariable String userId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Gian lan tai chinh") : "Gian lan tai chinh";
        RiskProfile profile = riskProfileRepository.findByUserId(userId)
                .orElseGet(() -> RiskProfile.createDefault(userId));
        profile.blacklist(reason);
        return ResponseEntity.ok(ApiResponse.success("Da dua tai khoan vao blacklist", riskProfileRepository.save(profile)));
    }

    @PostMapping("/{userId}/bank-changed")
    public ResponseEntity<ApiResponse<RiskProfile>> markBankChanged(@PathVariable String userId) {
        RiskProfile profile = riskProfileRepository.findByUserId(userId)
                .orElseGet(() -> RiskProfile.createDefault(userId));
        profile.markBankChanged(Instant.now());
        return ResponseEntity.ok(ApiResponse.success("Da kich hoat khoa 24h doi ngan hang", riskProfileRepository.save(profile)));
    }
}

