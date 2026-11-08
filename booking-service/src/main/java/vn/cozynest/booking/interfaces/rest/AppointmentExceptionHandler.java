package vn.cozynest.booking.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.cozynest.booking.application.exception.AppointmentNotFoundException;
import vn.cozynest.booking.application.exception.OptimisticLockConflictException;
import vn.cozynest.booking.application.exception.TimeConflictException;
import vn.cozynest.booking.domain.exception.InvalidAppointmentStateException;
import vn.cozynest.common.dto.ApiResponse;

import java.util.Map;

@RestControllerAdvice
public class AppointmentExceptionHandler {

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

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "BAD_REQUEST"));
    }
}
