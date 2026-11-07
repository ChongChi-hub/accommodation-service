package vn.toam.identity.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.toam.common.dto.ApiResponse;
import vn.toam.identity.domain.exception.IdentityDomainException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdentityDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(IdentityDomainException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "ERR_AUTH_INVALID_CREDENTIALS", "ERR_AUTH_INVALID_PASSWORD" -> HttpStatus.UNAUTHORIZED;
            case "ERR_AUTH_ACCOUNT_LOCKED", "ERR_FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "ERR_AUTH_USER_NOT_FOUND", "ERR_KYC_NOT_FOUND", "ERR_KYC_IMAGE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ERR_AUTH_EMAIL_EXISTS", "ERR_AUTH_PHONE_EXISTS", "ERR_KYC_ID_NUMBER_DUPLICATE",
                 "ERR_PROFILE_VERSION_CONFLICT", "ERR_PROFILE_PHONE_DUPLICATE" -> HttpStatus.CONFLICT;
            case "ERR_KYC_RATE_LIMIT_EXCEEDED", "ERR_RATE_LIMIT_EXCEEDED", "ERR_RATE_LIMIT_COOLDOWN" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã xảy ra lỗi hệ thống: " + ex.getMessage(), "ERR_INTERNAL_SERVER_ERROR"));
    }
}

