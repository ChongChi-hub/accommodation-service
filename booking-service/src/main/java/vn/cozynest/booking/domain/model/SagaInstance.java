package vn.cozynest.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstance {
    private String id;
    private String sagaId;
    private String sagaType;
    private String currentStep;
    private String compensationStep;
    private String payloadJson;
    private String errorMessage;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public static SagaInstance start(String sagaId, String sagaType, String initialStep, String payloadJson) {
        return SagaInstance.builder()
                .id(UUID.randomUUID().toString())
                .sagaId(sagaId)
                .sagaType(sagaType)
                .currentStep(initialStep)
                .payloadJson(payloadJson)
                .status("IN_PROGRESS")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void advance(String nextStep, String updatedPayload) {
        this.currentStep = nextStep;
        if (updatedPayload != null) this.payloadJson = updatedPayload;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.currentStep = "COMPLETED";
        this.status = "COMPLETED";
        this.updatedAt = Instant.now();
    }

    public void fail(String error) {
        this.errorMessage = error;
        this.status = "FAILED";
        this.updatedAt = Instant.now();
    }

    public void compensate(String compensationStep, String reason) {
        this.compensationStep = compensationStep;
        this.errorMessage = reason;
        this.status = "COMPENSATED";
        this.updatedAt = Instant.now();
    }

    public boolean isTerminal() {
        return "COMPLETED".equals(this.status) || "COMPENSATED".equals(this.status);
    }
}

