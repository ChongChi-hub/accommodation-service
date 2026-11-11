# Event Catalog — Tổ Ấm (CozyNest) (BROKER-01)

All asynchronous cross-service events use the versioned `DomainEventEnvelope` contract (`vn.cozynest.common.events.DomainEventEnvelope`) with transactional Outbox persistence, idempotent Inbox deduplication, monotonic `occurredAt` ordering check, and Dead Letter Queue (`dead_letter_messages`) fallback.

| Event Type | Schema Version | Producer | Consumers | Ordering Key | Payload Highlights |
|---|---:|---|---|---|---|
| `AppointmentConfirmedEvent` | 1 | `booking-service` | `communication-service`, `admin-service` | `appointmentId` | `appointmentId`, `roomId`, `tenantId`, `landlordId`, `scheduledStart` |
| `RentalRequestApprovedEvent` | 1 | `booking-service` | `billing-service`, `listing-service` | `rentalRequestId` | `rentalRequestId`, `roomId`, `tenantId`, `landlordId`, `depositAmountVnd` |
| `ContractSignedEvent` | 1 | `booking-service` | `billing-service`, `listing-service`, `communication-service` | `contractId` | `contractId`, `contractCode`, `roomId`, `monthlyRentVnd`, `depositVnd`, `documentHash` |
| `PaymentConfirmedEvent` | 1 | `billing-service` | `booking-service`, `communication-service` | `orderId` | `paymentCode`, `orderType`, `orderId`, `paidAmountVnd`, `journalEntryId` |
| `EscrowReleasedEvent` | 1 | `billing-service` | `communication-service`, `admin-service` | `escrowHoldId` | `escrowHoldId`, `contractId`, `landlordId`, `netPayoutVnd`, `feeVnd` |
| `KycStatusChangedEvent` | 1 | `identity-service` | `listing-service` | `userId` | `userId`, `kycStatus`, `verifiedAt`, `expiresAt` |
| `OwnershipDocReviewedEvent` | 1 | `listing-service` | `communication-service`, `admin-service` | `roomId` | `docId`, `roomId`, `landlordId`, `status`, `expiresAt` |

## Retry, Backoff & DLQ Policy
- Max automatic delivery attempts before quarantine: `5` with exponential backoff (`5s, 20s, 80s, 320s, 1280s`).
- Poison / failing payloads move atomically to `dead_letter_messages` with `status='PENDING_REPLAY'`.
- Replay is exposed exclusively to `ADMIN` via `POST /api/v1/admin/dlq/{id}/replay`.

