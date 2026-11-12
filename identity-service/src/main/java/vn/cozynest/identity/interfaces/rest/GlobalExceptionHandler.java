package vn.cozynest.identity.interfaces.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdentityDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(IdentityDomainException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "ERR_AUTH_INVALID_CREDENTIALS", "ERR_AUTH_INVALID_PASSWORD",
                 "ERR_AUTH_INVALID_TOKEN", "ERR_AUTH_TOKEN_EXPIRED", "ERR_INVALID_GOOGLE_TOKEN" -> HttpStatus.UNAUTHORIZED;
            case "ERR_AUTH_ACCOUNT_LOCKED", "ERR_AUTH_USER_LOCKED", "ERR_AUTH_TEMPORARY_LOCKED",
                 "ERR_FORBIDDEN", "ERR_USER_DELETED", "ERR_USER_SUSPENDED" -> HttpStatus.FORBIDDEN;
            case "ERR_AUTH_USER_NOT_FOUND", "ERR_KYC_NOT_FOUND", "ERR_KYC_IMAGE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ERR_AUTH_EMAIL_EXISTS", "ERR_AUTH_PHONE_EXISTS", "ERR_KYC_ID_NUMBER_DUPLICATE",
                 "ERR_PROFILE_VERSION_CONFLICT", "ERR_PROFILE_PHONE_DUPLICATE" -> HttpStatus.CONFLICT;
            case "ERR_KYC_RATE_LIMIT_EXCEEDED", "ERR_RATE_LIMIT_EXCEEDED", "ERR_RATE_LIMIT_COOLDOWN",
                 "ERR_LOGIN_RATE_LIMIT_EXCEEDED", "ERR_REGISTER_RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "ERR_GOOGLE_NOT_CONFIGURED", "ERR_AUTH_SERVICE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };

        HttpHeaders headers = new HttpHeaders();
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            String retryAfter = "ERR_LOGIN_RATE_LIMIT_EXCEEDED".equals(ex.getErrorCode()) ? "300" : "60";
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }

        return ResponseEntity.status(status)
                .headers(headers)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingException(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Hồ sơ đã được cập nhật bởi một phiên khác. Vui lòng tải lại trang.", "ERR_PROFILE_VERSION_CONFLICT"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        String message = errors.isEmpty() ? "Dữ liệu không hợp lệ" : errors.get(0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, "ERR_VALIDATION_FAILED", errors));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Không tìm thấy tài nguyên yêu cầu", "ERR_NOT_FOUND"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này", "ERR_FORBIDDEN"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã xảy ra lỗi hệ thống: " + ex.getMessage(), "ERR_INTERNAL_SERVER_ERROR"));
    }
}
