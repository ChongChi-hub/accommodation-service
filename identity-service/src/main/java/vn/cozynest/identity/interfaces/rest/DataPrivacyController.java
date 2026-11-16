package vn.cozynest.identity.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.AnonymizeAccountRequest;
import vn.cozynest.identity.application.dto.ConsentHistoryDto;
import vn.cozynest.identity.application.dto.ConsentRequestDto;
import vn.cozynest.identity.application.dto.DataSubjectExportDto;
import vn.cozynest.identity.application.usecase.ConsentUseCase;
import vn.cozynest.identity.application.usecase.DataSubjectRightsUseCase;

import java.util.List;

@RestController
@RequestMapping({"/api/v1", "/api/v1/users/me"})
@Tag(name = "Data Subject Rights & Privacy (Nghị định 13)", description = "Quyền chủ thể dữ liệu cá nhân theo Nghị định 13/2023/NĐ-CP")
public class DataPrivacyController {

    private final DataSubjectRightsUseCase dataSubjectRightsUseCase;
    private final ConsentUseCase consentUseCase;

    public DataPrivacyController(
            DataSubjectRightsUseCase dataSubjectRightsUseCase,
            ConsentUseCase consentUseCase
    ) {
        this.dataSubjectRightsUseCase = dataSubjectRightsUseCase;
        this.consentUseCase = consentUseCase;
    }

    @GetMapping("/export-data")
    @Operation(summary = "Trích xuất toàn bộ dữ liệu cá nhân tải về dạng JSON (Điều 14, 15 NĐ 13)")
    public ResponseEntity<DataSubjectExportDto> exportData(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        DataSubjectExportDto export = dataSubjectRightsUseCase.exportPersonalData(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cozynest_personal_data_" + userId + ".json\"");

        return ResponseEntity.ok().headers(headers).body(export);
    }

    @PostMapping("/anonymize")
    @Operation(summary = "Yêu cầu xóa và ẩn danh hóa tài khoản (Điều 16, 17 NĐ 13)")
    public ResponseEntity<ApiResponse<Void>> anonymizeAccount(
            Authentication authentication,
            @Valid @RequestBody AnonymizeAccountRequest request,
            HttpServletRequest servletRequest
    ) {
        String userId = (String) authentication.getPrincipal();
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        dataSubjectRightsUseCase.anonymizeAccount(userId, request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.message(
                "Tài khoản và thông tin cá nhân đã được xóa và ẩn danh vĩnh viễn theo quy định Nghị định 13/2023/NĐ-CP."
        ));
    }

    @PostMapping("/consents")
    @Operation(summary = "Ghi nhận đồng thuận Điều khoản / Cookie / Sinh trắc học")
    public ResponseEntity<ApiResponse<ConsentHistoryDto>> recordConsent(
            Authentication authentication,
            @Valid @RequestBody ConsentRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String userId = (authentication != null && authentication.getPrincipal() instanceof String)
                ? (String) authentication.getPrincipal()
                : null;
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        ConsentHistoryDto dto = consentUseCase.recordConsent(userId, request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận đồng thuận thành công", dto));
    }

    @GetMapping("/consents/my-history")
    @Operation(summary = "Lấy lịch sử các lần đồng thuận của người dùng")
    public ResponseEntity<ApiResponse<List<ConsentHistoryDto>>> getMyConsentHistory(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<ConsentHistoryDto> list = consentUseCase.getConsentHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
