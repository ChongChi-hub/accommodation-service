# Sprint 3 Audit & Remediation Execution Report (`Tổ Ấm / CozyNest`)

**Date:** 2026-11-12
**Branch (Both Repos):** `feat/sprint-3-audit-remediation`
**Scope:** 28 Sprint 3 Cards / 128 Story Points (per `index.md` & `action.txt`)
**Safety Gate:** `fintech.mode=SANDBOX_ONLY`, `fintech.escrow.live-custody-enabled=false`, `fintech.disbursement.live-enabled=false`
**Final Conclusion:** `SPRINT_3_NOT_READY_FOR_REAL_MONEY` (Sandbox technically verified; live money activation blocked by `LEGAL-02`)

---

## 1. Summary Overview

| Metric | Count / Value |
|---|---|
| Total Sprint 3 Cards | 28 (128 story points) |
| `VERIFIED_DONE` | 27 cards (125 pts; sandbox/pre-production mode) |
| `IMPLEMENTED_UNVERIFIED` | 0 |
| `PARTIAL` | 0 |
| `NOT_IMPLEMENTED` | 0 |
| `BLOCKED_LEGAL` | 1 (`LEGAL-02`, 3 pts) |
| `BLOCKED_EXTERNAL` | 0 (sandbox endpoints verified; live PSP partner onboarding gated by `LEGAL-02`) |

---

## 2. Card-by-Card Verification Matrix (28 Cards / 128 Points)

| # | Card ID | Pts | Owner | Area | Final Status | Key Implementation & Verification Evidence |
|---:|---|---:|---|---|---|---|
| 1 | `LEDGER-01` | 8 | Cường | Fintech Core | `VERIFIED_DONE` | Double-entry `LedgerEntry`/`LedgerLine` integer `BIGINT` VND, ordered `SELECT ... FOR UPDATE`, MySQL triggers (`V6__sprint3_billing_audit_hardening.sql`), `LedgerImmutabilityTestcontainersTest` (MySQL 9.0 container), `Wave2FintechSecurityAndAuditTest`. |
| 2 | `PAY-01` | 8 | Cường | Fintech Core | `VERIFIED_DONE` | `PaymentService` & `PaymentController`: `X-Webhook-Signature` HMAC-SHA256 (`401` on missing/invalid signature verified live), IP whitelist (`403`), replay guard, isolated sandbox simulation `POST /api/v1/billing/payments/sandbox/simulate-webhook`. |
| 3 | `PAY-02` | 3 | Cường | Fintech Core | `VERIFIED_DONE` | `PaymentReconciliationService`, `ReconciliationReport`, CSV export (`<= 366` days / `5000` rows, UTF-8 BOM, formula neutralization), `ReconTab.tsx`, `ExportTab.tsx`. |
| 4 | `LEDGER-02` | 3 | Dev 2 | Fintech Core | `VERIFIED_DONE` | Two-person Maker-Checker approval workflow (`MakerCheckerController`, `RefundService`, `MakerCheckerAndRefundsTab.tsx`), ledger integrity & audit logs. |
| 5 | `SAGA-01` | 5 | Cường | Transaction Flow | `VERIFIED_DONE` | `RentalSagaOrchestrator`, `SagaInstance`, `SagaController`, cross-service room hold/release compensation + `MANUAL_INTERVENTION_REQUIRED` queue, `RentalSagaOrchestratorTest`. |
| 6 | `ESCROW-01` | 5 | Dev 2 | Transaction Flow | `VERIFIED_DONE` | `EscrowHold`, `EscrowController`, two-party move-in confirmation gate & dispute freeze; `live-custody-enabled=false` enforced until `LEGAL-02`. |
| 7 | `APPT-RENT-BE` | 5 | Dev 2 | Transaction Flow | `VERIFIED_DONE` | `ViewingAppointment`, `LandlordAppointmentController/Service`, `TenantAppointmentController/Service` (`GET/POST /api/v1/tenant/appointments`, `POST .../cancel`), `RentalRequestService`, `TenantAppointmentServiceTest`, `AppointmentConcurrencyTestcontainersTest`. |
| 8 | `FIN-RISK-01` | 5 | Dev 2 | Transaction Flow | `VERIFIED_DONE` | Transaction velocity/limit checks & risk guards in `PayoutService`, `RefundService`, `PaymentService`; `Wave2FintechSecurityAndAuditTest`. |
| 9 | `REFUND-01` | 3 | Dev 4 | Transaction Flow | `VERIFIED_DONE` | `RefundService`, `RefundController`, live OTP verification via `identity-service`, KYC name match, `cumulativeRefund <= settledPaidAmount`, `FAILED` payout retry queue. |
| 10 | `CONTRACT-BE` | 5 | Dev 2 | Contract | `VERIFIED_DONE` | `ContractService`, `ContractController`, `ContractPdfService`, SHA-256 immutable document hash, OTP signing audit log (IP, User-Agent, OTP, timestamp), `InternalContractQueryController`. |
| 11 | `CONTRACT-02` | 3 | Dev 2 | Contract | `VERIFIED_DONE` | `ContractAmendment`, `ContractOccupant`, renewals, extensions, co-occupants, amendment signing OTP, `ContractAndAmendmentHardeningTest`. |
| 12 | `CONTRACT-FE` | 5 | Dev 3 | Contract | `VERIFIED_DONE` | `ContractDetailWorkspace.tsx` (refactored), `ContractOccupantsSection.tsx`, `ContractAmendmentsSection.tsx` (OTP dialog), landlord/admin contract pages, PDF download. |
| 13 | `APPT-RENT-FE` | 5 | Dev 3 | Booking & Listing | `VERIFIED_DONE` | `RoomBookingModal.tsx` (`createAppointment`), `RoomSidebar.tsx` & `RoomMobileActionBar.tsx` (real chat navigation + `cozynest_role` auth check), `tenant/appointments/page.tsx` (cancel + `.ics` export), `landlord/appointments/page.tsx`. |
| 14 | `ROOM-03` | 3 | Dev 3 | Booking & Listing | `VERIFIED_DONE` | Extended listing attributes/amenities/utilities & validation in `listing-service` (`ListingSprint3HardeningTest`) + FE UI integration. |
| 15 | `KYC-03` | 3 | Dev 3 | Booking & Listing | `VERIFIED_DONE` | `PropertyOwnershipService`, `PropertyOwnershipController`, `OwnershipDocAccessLog`, `V17__sprint3_ownership_access_audit.sql`, `PropertyOwnershipKycSection.tsx`. |
| 16 | `DASH-FIX-1` | 5 | Dev 3 | FE Architecture | `VERIFIED_DONE` | Next.js 16 `src/proxy.ts` RS256 JWKS guard, `/api/auth/*` HttpOnly cookies (tokens stripped from JSON), `/api/v1/[...path]/route.ts` CSRF-protected BFF proxy injecting `Authorization: Bearer` server-side, zero `cozynest_token`/`refreshToken` in JS/localStorage, real `landlord/analytics/page.tsx`. |
| 17 | `PAY-VIP-FE` | 5 | Dev 4 | FE Architecture | `VERIFIED_DONE` | `tenant/deposits/page.tsx`, `tenant/payments/page.tsx`, `landlord/deposits/page.tsx`, `http-billing.repository.ts`, wired through BFF `/api/v1/billing/*`. |
| 18 | `METER-01` | 5 | Dev 4 | Finance | `VERIFIED_DONE` | `UtilityReadingService`, `UtilityReadingController`, `landlord/utilities/page.tsx`, `MeterInvoiceRefundDashboardTest`. |
| 19 | `INVOICE-01/02` | 5 | Dev 4 | Finance | `VERIFIED_DONE` | `InvoiceService`, `InvoiceController`, `InvoicePdfService`, `landlord/invoices/page.tsx`, `tenant/invoices/[id]/page.tsx` (with PDF receipt download), `MeterInvoiceRefundDashboardTest`. |
| 20 | `DASH-STATS-BE` | 5 | Dev 4 | Finance | `VERIFIED_DONE` | `BillingDashboardService`, `BillingDashboardController`, `DashboardStatsDto` (real monthly revenue + KPI aggregates & exports). |
| 21 | `HARDEN-01` | 5 | Cường | Infrastructure | `VERIFIED_DONE` | Internal service-key guards (`X-Internal-Service-Key`, live `401` verified on unauthenticated `/internal/*`), `GlobalExceptionHandler` error sanitization, `X-Request-ID`, `booking-service/Dockerfile` writable `/app/storage/booking-private`. |
| 22 | `DEVOPS-02` | 5 | Dev 5 | Infrastructure | `VERIFIED_DONE` | `docker-compose.yml` (`--log-bin-trust-function-creators=1`, all 11 containers healthy), `application-staging.yml` & `application-prod.yml` `SANDBOX_ONLY` + fail-fast guards. |
| 23 | `STORAGE-01` | 3 | Dev 3 | Infrastructure | `VERIFIED_DONE` | `EncryptedLocalStorageService`, `SignedDownloadTokenService`, `PrivateStorageController`, `StorageAccessLog`, `PrivateStorageSecurityTest`. |
| 24 | `BROKER-01` | 5 | Dev 5 | Infrastructure | `VERIFIED_DONE` | RabbitMQ `EventTopology` (`cozynest.events`, `cozynest.dlx`, `cozynest.communication.events.queue`, `cozynest.events.dlq`), `BillingOutboxPublisherScheduler`, `AppointmentOutboxPublisherScheduler`, `EventInbox` dedupe, `DeadLetterController`/`DeadLetterAdminService`. |
| 25 | `SEC-PROD-01` | 5 | Dev 5 | Infrastructure | `VERIFIED_DONE` | `TwoFactorAuthService`, `TwoFactorHardeningTest`, production secret fail-fast checks, secret/token hygiene scan passed. |
| 26 | `PHONE-OTP-01` | 3 | Dev 5 | Infrastructure | `VERIFIED_DONE` | Live phone/user OTP verification & rate-limiting in `identity-service`, wired to `booking-service` (`HttpIdentityVerificationAdapter`) & `billing-service`. |
| 27 | `QA-FINTECH` | 5 | Dev 5 | Quality | `VERIFIED_DONE` | `e2e/qa-fintech-sprint3.spec.ts`, `e2e/auth-rbac-middleware.spec.ts`, `e2e/auth-token-refresh.spec.ts`, `e2e/landlord-appointments-visual.spec.ts`, `e2e/smoke.spec.ts` (63/63 passed). |
| 28 | `LEGAL-02` | 3 | Cường | Legal | `BLOCKED_LEGAL` | Technical gates (`SANDBOX_ONLY`, `live-custody-enabled=false`, `live-enabled=false`) and ADR Section 16 documented in `docs/DECISIONS.md`; awaits external legal/NHNN/tax/audit sign-off. |

---

## 3. Verified Execution & Test Results

### Backend (`accommodation-service`)
- `.\gradlew.bat clean test bootJar --no-daemon --no-build-cache --console=plain`: **EXIT 0 (`BUILD SUCCESSFUL in 6m 48s`, 56 tasks executed)**.
- JUnit / Testcontainers XML summary across all 8 backend modules: **109 test suites, 396 tests, 0 skipped, 0 failures, 0 errors**.
- Testcontainers (`MySQL 9.0` Docker containers): all 8 Testcontainers suites (`LedgerImmutabilityTestcontainersTest`, `AppointmentConcurrencyTestcontainersTest`, `KycAndProfileTxTestcontainersTest`, `RefreshTokenReuseTestcontainersTest`, `DatabaseIsolationTestcontainersTest`, `LandlordProfilePersistenceTestcontainersTest`, `ProductionFlywayMigrationTestcontainersTest`, `RoomSearchTestcontainersTest`) started real `mysql:9.0` containers and passed (`skipped=0`, `failures=0`).
- Docker Compose (`docker compose --profile full ps`): **11/11 containers running and healthy** (`cozynest_mysql`, `cozynest_redis`, `cozynest_mailpit`, `cozynest_rabbitmq`, `cozynest_gateway`, `cozynest_identity`, `cozynest_listing`, `cozynest_booking`, `cozynest_billing`, `cozynest_communication`, `cozynest_admin`).
- RabbitMQ live topology (`rabbitmqctl`): `cozynest.events` (`topic`), `cozynest.dlx` (`direct`), `cozynest.communication.events.queue` (1 consumer), `cozynest.events.dlq` (1 consumer).
- MySQL live schemas (`cozynest_mysql`): all 6 service databases (`cozynest_admin`, `cozynest_billing`, `cozynest_booking`, `cozynest_communication`, `cozynest_identity`, `cozynest_listing`) initialized.
- Live HTTP security & health checks:
  - `http://localhost:8080/actuator/health`, `:8081`–`:8086` `/actuator/health`: HTTP `200 UP`.
  - Unauthenticated `POST http://localhost:8080/api/v1/billing/payments/webhook`: HTTP `401 Unauthorized`.
  - Unauthenticated `POST http://localhost:8082/internal/rooms/1/hold` & `GET http://localhost:8083/internal/contracts/c-1/effective-rent`: HTTP `401`.
  - Unauthenticated `POST http://localhost:8080/api/v1/tenant/appointments`: HTTP `401 Unauthorized`.
  - Live `booking-service` startup issue discovered during Docker verification (`BeanCreationException` on `./storage/booking-private` directory permissions under non-root `appuser`) was fixed in `booking-service/Dockerfile`, rebuilt, and verified healthy.

### Frontend (`accommodation-web`)
- `npx tsc --noEmit`: **EXIT 0** (0 errors).
- `npm run verify:i18n`: **EXIT 0** (VI `1936` keys, EN `1936` keys, 100% parity).
- `npm run verify:links`: **EXIT 0** (384 files scanned, 86 unique internal links valid).
- `npm run verify:manifest`: **EXIT 0** (224 raster images valid, Hero #1 `246.4 KB` within `250 KB` budget).
- `npm run check:seed-text`: **EXIT 0** (100% valid UTF-8, zero `U+FFFD`, zero `??`).
- `npx vitest run`: **EXIT 0** (`17` test files, `77` tests passed).
- `npm run lint -- --max-warnings=0`: **EXIT 0** (0 errors, 0 warnings).
- `npm run build`: **EXIT 0** (`Compiled successfully in 6.4s`, `Finished TypeScript in 8.4s`, `188` routes + `/api/*` BFF routes built).
- `GET http://localhost:3000/api/auth/session`: HTTP `200 {"authenticated":false,"role":null}`.
- Playwright E2E against production server (`http://localhost:3000`):
  - `npx playwright test e2e/auth-rbac-middleware.spec.ts e2e/auth-token-refresh.spec.ts e2e/qa-fintech-sprint3.spec.ts e2e/landlord-appointments-visual.spec.ts --reporter=list`: **10 passed** (`22.2s`).
  - `npx playwright test e2e/smoke.spec.ts --reporter=list`: **53 passed** (`44.3s`).
  - Total Playwright E2E executed & passing: **63/63 tests passed**.

---

## 4. `LEGAL-02` Production Financial Blockers & Go-Live Status

- **Conclusion:** `SPRINT_3_NOT_READY_FOR_REAL_MONEY`
- Live custody & disbursement remain hard-disabled (`fintech.mode=SANDBOX_ONLY`, `live-custody-enabled=false`, `live-enabled=false`).
- Activating real money requires external human approvals for `LEGAL-02`:
  1. Licensed NHNN payment intermediary agreement & model determination (Decree 52/2024/ND-CP).
  2. External tax/accounting sign-off on landlord withholding & e-invoice obligations (Law 56/2024/QH15).
  3. Legal audit acceptance of OTP e-contract & amendment enforceability (Civil Code 2015 & Electronic Transactions Law 2023).
  4. Production payment gateway merchant credentials, KMS/Vault key provisioning, webhook domain/TLS, and second-engineer fintech code review sign-off.
