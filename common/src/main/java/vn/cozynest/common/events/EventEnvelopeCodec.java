package vn.cozynest.common.events;

import tools.jackson.databind.ObjectMapper;

/**
 * Codec serializing/deserializing versioned DomainEventEnvelope and DeadLetterEnvelope messages.
 */
public final class EventEnvelopeCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventEnvelopeCodec() {
    }

    public static String encode(DomainEventEnvelope envelope) {
        return MAPPER.writeValueAsString(envelope);
    }

    public static DomainEventEnvelope decode(String json) {
        return MAPPER.readValue(json, DomainEventEnvelope.class);
    }

    public static String encodeDeadLetter(DeadLetterEnvelope dlq) {
        return MAPPER.writeValueAsString(dlq);
    }

    public static DeadLetterEnvelope decodeDeadLetter(String json) {
        return MAPPER.readValue(json, DeadLetterEnvelope.class);
    }
}

