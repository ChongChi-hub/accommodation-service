package vn.cozynest.common.events;

/**
 * Canonical RabbitMQ exchange, queue, and dead-letter topology constants plus PII sanitization helpers.
 */
public final class EventTopology {

    public static final String EVENTS_EXCHANGE = "cozynest.events";
    public static final String DLX_EXCHANGE = "cozynest.dlx";
    public static final String COMMUNICATION_QUEUE = "cozynest.communication.events.queue";
    public static final String DLQ_QUEUE = "cozynest.events.dlq";
    public static final String DLQ_ROUTING_KEY = "cozynest.dlq";
    public static final int SUPPORTED_MAX_SCHEMA_VERSION = 1;

    private EventTopology() {
    }

    public static String sanitizePiiForDlq(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "{}";
        }
        String sanitized = rawJson;
        sanitized = sanitized.replaceAll("(\"password\"|\"otp\"|\"otpCode\"|\"token\"|\"refreshToken\"|\"secret\")\\s*:\\s*\"[^\"]*\"", "$1:\"***REDACTED***\"");
        sanitized = sanitized.replaceAll("(\"idCardNumber\"|\"accountNumber\"|\"phoneNumber\"|\"phone\")\\s*:\\s*\"([^\"]{0,3})[^\"]*([^\"]{2})\"", "$1:\"$2***$3\"");
        return sanitized;
    }
}

