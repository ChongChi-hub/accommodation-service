package vn.cozynest.booking.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.service.RentalSagaOrchestrator;
import vn.cozynest.booking.domain.model.SagaInstance;
import vn.cozynest.common.dto.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/booking/sagas")
@PreAuthorize("hasRole('ADMIN')")
public class SagaController {

    private final RentalSagaOrchestrator orchestrator;

    public SagaController(RentalSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SagaInstance>> startSaga(@RequestBody Map<String, String> body) {
        String sagaId = body.get("sagaId");
        String payload = body.getOrDefault("payloadJson", "{}");
        return ResponseEntity.ok(ApiResponse.success(orchestrator.startRentalSaga(sagaId, payload)));
    }

    @PostMapping("/{sagaId}/deposit-paid")
    public ResponseEntity<ApiResponse<SagaInstance>> markDepositPaid(
            @PathVariable String sagaId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String payload = body != null ? body.get("payloadJson") : null;
        return ResponseEntity.ok(ApiResponse.success(orchestrator.advanceToDepositPaid(sagaId, payload)));
    }

    @PostMapping("/{sagaId}/contract-signed")
    public ResponseEntity<ApiResponse<SagaInstance>> completeSaga(@PathVariable String sagaId) {
        return ResponseEntity.ok(ApiResponse.success(orchestrator.completeSaga(sagaId)));
    }

    @PostMapping("/{sagaId}/compensate")
    public ResponseEntity<ApiResponse<SagaInstance>> compensateSaga(
            @PathVariable String sagaId,
            @RequestBody Map<String, String> body
    ) {
        String reason = body.getOrDefault("reason", "Manual compensation");
        return ResponseEntity.ok(ApiResponse.success(orchestrator.compensateSaga(sagaId, reason)));
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<ApiResponse<SagaInstance>> getSaga(@PathVariable String sagaId) {
        return ResponseEntity.ok(ApiResponse.success(orchestrator.getSaga(sagaId)));
    }

    @GetMapping("/manual-queue")
    public ResponseEntity<ApiResponse<java.util.List<SagaInstance>>> getManualQueue() {
        return ResponseEntity.ok(ApiResponse.success(orchestrator.getManualInterventionSagas()));
    }

    @PostMapping("/{sagaId}/resolve-manual")
    public ResponseEntity<ApiResponse<SagaInstance>> resolveManual(
            @PathVariable String sagaId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = body != null ? body.get("resolutionNote") : "Resolved by operator";
        return ResponseEntity.ok(ApiResponse.success(orchestrator.resolveManualSaga(sagaId, note)));
    }
}
