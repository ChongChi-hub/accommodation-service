package vn.cozynest.booking.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.service.RentalSagaOrchestrator;
import vn.cozynest.booking.domain.model.SagaInstance;
import vn.cozynest.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/booking/sagas")
public class SagaController {

    private final RentalSagaOrchestrator sagaOrchestrator;

    public SagaController(RentalSagaOrchestrator sagaOrchestrator) {
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SagaInstance>> startSaga(@RequestParam String rentalRequestId) {
        SagaInstance saga = sagaOrchestrator.startRentalSaga(rentalRequestId);
        return ResponseEntity.ok(ApiResponse.success(saga));
    }

    @PostMapping("/{sagaId}/deposit-paid")
    public ResponseEntity<ApiResponse<SagaInstance>> onDepositPaid(
            @PathVariable String sagaId, @RequestParam String contractId) {
        SagaInstance saga = sagaOrchestrator.onDepositPaid(sagaId, contractId);
        return ResponseEntity.ok(ApiResponse.success(saga));
    }

    @PostMapping("/{sagaId}/contract-signed")
    public ResponseEntity<ApiResponse<SagaInstance>> onContractSigned(
            @PathVariable String sagaId, @RequestParam String contractId) {
        SagaInstance saga = sagaOrchestrator.onContractFullySigned(sagaId, contractId);
        return ResponseEntity.ok(ApiResponse.success(saga));
    }

    @PostMapping("/{sagaId}/compensate")
    public ResponseEntity<ApiResponse<SagaInstance>> compensateSaga(
            @PathVariable String sagaId, @RequestParam(defaultValue = "Giao dich bi huy") String reason) {
        SagaInstance saga = sagaOrchestrator.compensate(sagaId, reason);
        return ResponseEntity.ok(ApiResponse.success(saga));
    }
}

