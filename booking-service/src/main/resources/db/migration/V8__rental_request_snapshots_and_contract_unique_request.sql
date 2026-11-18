-- V8: Additive snapshot columns on rental_requests + safe UNIQUE constraint on contracts(request_id)

ALTER TABLE rental_requests
    ADD COLUMN tenant_name VARCHAR(150) NULL AFTER tenant_id,
    ADD COLUMN room_title VARCHAR(255) NULL AFTER room_id,
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER status;

-- Backfill room_title and tenant_name from same-schema viewing_appointments where available (no cross-schema access)
UPDATE rental_requests rr
JOIN (
    SELECT room_id, MAX(room_title) AS room_title
    FROM viewing_appointments
    WHERE room_title IS NOT NULL AND TRIM(room_title) <> ''
    GROUP BY room_id
) va_room ON va_room.room_id = rr.room_id
SET rr.room_title = va_room.room_title
WHERE rr.room_title IS NULL;

UPDATE rental_requests rr
JOIN (
    SELECT tenant_id, MAX(contact_name) AS contact_name
    FROM viewing_appointments
    WHERE contact_name IS NOT NULL AND TRIM(contact_name) <> ''
    GROUP BY tenant_id
) va_tenant ON va_tenant.tenant_id = rr.tenant_id
SET rr.tenant_name = va_tenant.contact_name
WHERE rr.tenant_name IS NULL;

-- Safely detach non-primary duplicate contracts referencing the same request_id (zero rows deleted)
UPDATE contracts c
JOIN (
    SELECT request_id, MIN(created_at) AS first_created_at
    FROM contracts
    WHERE request_id IS NOT NULL
    GROUP BY request_id
    HAVING COUNT(*) > 1
) dup ON dup.request_id = c.request_id
LEFT JOIN rental_requests rr ON rr.id = c.request_id
SET c.request_id = NULL
WHERE (rr.contract_id IS NOT NULL AND c.id <> rr.contract_id)
   OR (rr.contract_id IS NULL AND c.created_at > dup.first_created_at);

ALTER TABLE contracts
    ADD CONSTRAINT uq_contracts_request_id UNIQUE (request_id);

