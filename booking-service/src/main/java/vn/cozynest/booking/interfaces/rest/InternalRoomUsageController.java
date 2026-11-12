package vn.cozynest.booking.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataContractRepository;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataViewingAppointmentRepository;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/internal/rooms")
public class InternalRoomUsageController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final SpringDataContractRepository contractRepository;
    private final SpringDataViewingAppointmentRepository appointmentRepository;
    private final String expectedServiceKey;

    public InternalRoomUsageController(
            SpringDataContractRepository contractRepository,
            SpringDataViewingAppointmentRepository appointmentRepository,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env) {
        this.contractRepository = contractRepository;
        this.appointmentRepository = appointmentRepository;
        this.expectedServiceKey = expectedServiceKey;

        if (env != null && env.matchesProfiles("prod", "production")) {
            String envKey = System.getenv("INTERNAL_SERVICE_KEY");
            if (envKey == null || envKey.isBlank() || DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Production environment must explicitly configure INTERNAL_SERVICE_KEY without default dev key");
            }
        }
    }

    @GetMapping("/{roomId}/usage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomUsage(
            @PathVariable String roomId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cập nội bộ không hợp lệ", "ERR_UNAUTHORIZED"));
        }
        boolean hasActiveContract = contractRepository.findActiveByRoomId(roomId).isPresent();
        boolean hasUpcomingAppointment = appointmentRepository.countActiveAppointmentsByRoomId(roomId, Instant.now()) > 0;
        boolean inActiveUse = hasActiveContract || hasUpcomingAppointment;

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "roomId", roomId,
                "inActiveUse", inActiveUse,
                "hasActiveContract", hasActiveContract,
                "hasUpcomingAppointment", hasUpcomingAppointment
        )));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}

