package vn.cozynest.booking.interfaces.rest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.cozynest.booking.application.exception.AppointmentNotFoundException;
import vn.cozynest.booking.application.exception.ContractAlreadyExistsException;
import vn.cozynest.booking.application.exception.ContractNotFoundException;
import vn.cozynest.booking.application.exception.OptimisticLockConflictException;
import vn.cozynest.booking.application.exception.RentalRequestInvalidStateException;
import vn.cozynest.booking.application.exception.RentalRequestNotFoundException;
import vn.cozynest.booking.application.exception.TimeConflictException;
import vn.cozynest.booking.application.exception.VerificationRequirementException;
import vn.cozynest.booking.domain.exception.AppointmentNotStartedException;
import vn.cozynest.booking.domain.exception.InvalidAppointmentStateException;
import vn.cozynest.common.dto.ApiResponse;

import java.util.Map;

@RestControllerAdvice
public class AppointmentExceptionHandler {

    @ExceptionHandler(VerificationRequirementException.class)
    public ResponseEntity<ApiResponse<Void>> handleVerificationRequirement(VerificationRequirementException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(InvalidAppointmentStateException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidState(InvalidAppointmentStateException ex) {
        Map<String, Object> data = Map.of(
                "currentStatus", ex.getCurrentStatus() != null ? ex.getCurrentStatus().name() : "",
                "targetStatus", ex.getTargetStatus() != null ? ex.getTargetStatus().name() : ""
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .errorCode("ERR_INVALID_APPOINTMENT_STATE")
                .message(ex.getMessage())
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AppointmentNotStartedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppointmentNotStarted(AppointmentNotStartedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "ERR_APPOINTMENT_NOT_STARTED"));
    }

    @ExceptionHandler(RentalRequestNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRentalRequestNotFound(RentalRequestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "ERR_RENTAL_REQUEST_NOT_FOUND"));
    }

    @ExceptionHandler(RentalRequestInvalidStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleRentalRequestInvalidState(RentalRequestInvalidStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "ERR_RENTAL_REQUEST_INVALID_STATE"));
    }

    @ExceptionHandler(ContractAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleContractAlreadyExists(ContractAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "ERR_CONTRACT_ALREADY_EXISTS"));
    }

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleContractNotFound(ContractNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "ERR_CONTRACT_NOT_FOUND"));
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(AppointmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "APPOINTMENT_NOT_FOUND"));
    }

    @ExceptionHandler(TimeConflictException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTimeConflict(TimeConflictException ex) {
        Map<String, Object> data = Map.of(
                "conflictingAppointmentId", ex.getConflictingAppointmentId(),
                "conflictingRoomTitle", ex.getConflictingRoomTitle(),
                "conflictingStart", ex.getConflictingStart().toString(),
                "conflictingEnd", ex.getConflictingEnd().toString()
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .errorCode("TIME_CONFLICT")
                .message(ex.getMessage())
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "VERSION_CONFLICT"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), "FORBIDDEN"));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : fe.getField() + " khong hop le")
                .findFirst()
                .orElse("Du lieu gui len khong hop le");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dinh dang ngay gio hoac du lieu khong hop le.", "INVALID_PAYLOAD"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = "Du lieu bi trung lap hoac vi pham rang buoc he thong.";
        String lower = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (lower.contains("appointment") || lower.contains("viewing")) {
            msg = "Du lieu bi trung lap hoac da ton tai yeu cau cho khung gio nay.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(msg, "DATA_CONFLICT"));
    }
}
