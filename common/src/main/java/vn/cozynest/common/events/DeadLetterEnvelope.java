package vn.cozynest.common.events;

/**
 * Sanitized dead-letter envelope transported over RabbitMQ DLX/DLQ into admin-service dead_letter_messages.
 */
public record DeadLetterEnvelope(
        DomainEventEnvelope envelope,
        String sourceService,
        String failureReason,
        int retryCount
) {
}

