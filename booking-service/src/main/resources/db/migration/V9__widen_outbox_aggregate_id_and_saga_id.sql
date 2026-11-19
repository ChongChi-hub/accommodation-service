-- BUG-03: Widen aggregate_id and saga_id to support "saga-" + 36-char UUID (41 chars)
ALTER TABLE appointment_events_outbox
    MODIFY COLUMN aggregate_id VARCHAR(64) NOT NULL;

ALTER TABLE saga_instances
    MODIFY COLUMN saga_id VARCHAR(64) NOT NULL;

