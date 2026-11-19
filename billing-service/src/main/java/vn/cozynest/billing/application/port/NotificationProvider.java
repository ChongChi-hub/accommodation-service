package vn.cozynest.billing.application.port;

import java.time.Instant;
import java.util.Map;

public interface NotificationProvider {

    String getProviderName();

    boolean verifySignature(String rawBody, String signature, long timestampSeconds);

    boolean isTimestampWithinWindow(long timestampSeconds);

    ParsedBankNotification parseNotification(String rawBody, Map<String, String> headers);

    String sanitizePayload(String rawBody);

    String computeTestSignature(String rawBody, long timestampSeconds);

    record ParsedBankNotification(
            String provider,
            String providerTxId,
            String direction,
            long amount,
            String receiverAccountNumber,
            String transferContent,
            Instant occurredAt,
            String ExplicitPaymentCode
    ) {}
}

