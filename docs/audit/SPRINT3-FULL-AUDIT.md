# SPRINT 3 FULL AUDIT REPORT — TỔ ẤM (COZYNEST)

- **Audit Date:** 2026-11-11
- **Branches:** `feat/sprint-3-complete` (branched from `chore/s12-reconcile`) in both `accommodation-service` and `accommodation-web`
- **Source of Truth:** `PROJECT/index.md` (`KE_HOACH_TRELLO_V2`, 463 lines — 28 cards / 128 story points) + source code + live verification runs
- **Policy:** `AGENTS / NO FABRICATION / LEGAL-02 HUMAN GATE`

---

## 1. Executive Summary & Baseline Verification

### 1.1 Baseline Test & Build Metrics (Pre-Phase 2 Execution)
| Repository | Command | Result | Exact Baseline Count / Output |
|---|---|---|---|
| `accommodation-service` | `.\gradlew.bat test --rerun --console=plain` | PASS | `309` tests across `75` XML suites (`0` fail, `0` error, `0` skipped): `common` (16), `api-gateway` (7), `identity` (160), `listing` (68), `booking` (27), `billing` (13), `communication` (13), `admin` (5) |
| `accommodation-web` | `npm run type-check` | PASS | `0` TypeScript errors |
| `accommodation-web` | `npm run test` | PASS | `16` files / `73` Vitest unit tests passed; `1056` vi & `1056` en i18n keys (`100%` parity); `86` internal links verified; `224` images verified |
| `accommodation-web` | `npm run lint` | WARN | `0` errors, **`737` warnings** (`i18next/no-literal-string`) |
| `accommodation-web` | `npm run check:seed-text` | PASS | `100%` UTF-8, `0` `U+FFFD` |
| `accommodation-web` | `npm run build` | PASS (WARN) | `182` routes generated; deprecation warning: `The "middleware" file convention is deprecated. Please use "proxy" instead.` |

### 1.2 Critical Systemic Findings
1. **`LEGAL-02` vs Existing `billing-service` Code Conflict (CRITICAL BLOCKER):**
   - `ADR-008` (`docs/DECISIONS.md`) and `docs/legal/LEGAL-02-DECISION-MATRIX.md` explicitly **reject Model D** (in-house wallet custody / company holding deposits) until NHNN intermediary payment licensing or licensed PG Model B split, and set external counsel/accountant statuses as `PENDING_LAWYER` / `PENDING_ACCOUNTANT` (`PENDING_EXTERNAL_LEGAL_REVIEW`).
   - However, existing commit `82c4c49` / `d862d9d` implemented `2010-ESCROW`, `2020-WALLET-<landlordId>`, auto 5% fee split, and a hardcoded `img.vietqr.io/image/970422-0912345678-...accountName=TO%20AM%20ESCROW` inside `PaymentService.java`, `EscrowService.java`, and `PayoutService.java`, plus FE pages claim Ngh? d?nh 52/2024/ND-CP compliance while calling mocked `setTimeout` handlers.
   - **Resolution:** Mark live Model B/D activation `BLOCKED_HUMAN_REVIEW`; introduce sandbox/feature flags (`fintech.mode=SANDBOX_ONLY`, `fintech.escrow.live-custody-enabled=false`, `fintech.payout.live-disbursement-enabled=false`) and abstract `PaymentGatewayPort` / configurable settlement policy (`SANDBOX_P2P_VIETQR` vs future `LICENSED_PG_SPLIT`), keeping neutral double-entry ledger architecture safe without enabling live money custody.
2. **Sprint 3 Security Vulnerabilities (`billing-service`, `admin-service`, `common-security`, FE BFF):**
   - `JwksTokenValidator` exists in `common` but is **not wired** in any microservice `JwtAuthFilter` (`listing`, `booking`, `billing`, `communication`), and `admin-service` lacks `SecurityConfig` & `JwtAuthFilter` entirely.
   - `billing-service` controllers (`LedgerController`, `MakerCheckerController`, `BillingDashboardController`, `InvoiceController`, `UtilityReadingController`) lack `@PreAuthorize` role guards and resource ownership checks (**critical IDOR + privilege escalation** — any tenant can query `/api/v1/billing/ledger/export/general-ledger`, approve maker-checker requests, or cancel invoices).
   - `PayoutService.requestPayout` auto-completes payouts immediately without 2FA/OTP verification, KYC bank-holder matching, 24h bank-change lock, or Maker-Checker above threshold.
3. **Frontend Sprint 3 Mocked Data Bypass (CRITICAL):**
   - Almost all FE Sprint 3 pages (`tenant/contracts/[id]`, `tenant/payments`, `tenant/deposits`, `tenant/invoices/[id]`, `landlord/contracts`, `landlord/invoices`, `landlord/utilities`, `landlord/deposits`) contain hardcoded mock arrays and `setTimeout` toast simulations rather than real API calls through Clean Architecture repositories.
   - `container.ts` still wires `StaticAdminRepository`, `StaticSupportRepository`, `StaticMaintenanceRepository`, `StaticDisputeRepository`, `StaticTransactionRepository`, `StaticBadgeRepository`.
   - `src/middleware.ts` is still present instead of Next.js 16 `src/proxy.ts`, reading non-HttpOnly `cozynest_token` directly instead of BFF HttpOnly cookie auth (`/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`).
4. **Architecture & File-Size Violations (`AGENT.md`):**
   - `LedgerService.java` = `455` lines (hard limit `250` lines).
   - `admin/transactions/page.tsx` = `796` lines.
   - `BillingArchitectureTest`, `ListingArchitectureTest`, `AdminArchitectureTest` omit `domainMustNotDependOnJakartaPersistence` and `domainMustNotDependOnSpringFramework` (e.g., `Room.java` & `PropertyOwnershipDocument.java` put `@Entity` in `domain.model`).

---

## 2. Subtask-Level Audit of All 28 Sprint 3 Cards (128 Story Points)

| # | Card ID | Pts | Priority | Current State | Key Verified Gaps & Missing Subtasks |
|---|---|---|---|---|---|
| 1 | `LEGAL-02` | 3 | P0-GO-LIVE | `BLOCKED_HUMAN_REVIEW` (Docs partial) | Docs exist (`LEGAL-02-*.md`, `ADR-008`, `ADR-010`), but codebase (`PaymentService`/`EscrowService`/`PayoutService`) contradicts ADR-008 by hardcoding Model D `2010-ESCROW` / `2020-WALLET` & `TO AM ESCROW`. Lawyer/accountant/bank sign-offs remain `PENDING`. Must gate Model D & update `ADR-008`/audit docs. |
| 2 | `HARDEN-01` | 5 | P0-GO-LIVE | `PARTIAL / DEBTS OPEN` | `JwksTokenValidator` unwired across 5 services; `admin-service` lacks SecurityConfig; `RefreshTokenUseCase` missing `noRollbackFor`; `AuthController.logout` non-transactional; `LandlordAppointmentService` calls `findOverlappingConfirmed` instead of `ForUpdate`; `IdentityApplication` missing `@EnableScheduling`; no Resilience4j; FE `RoomCard`/`AreaDropdown`/`hosts/[id]`/`RoomFilterBar` debts + `listing` strict ArchUnit. |
| 3 | `DEVOPS-02` | 5 | P0-GO-LIVE | `NOT_STARTED` | Missing `application-staging.yml`, `application-prod.yml`, `.env.staging.example`, DB least-privilege script wiring, `fintech.*` feature flags, Compose service healthchecks, expand/contract migration & rollback runbook, email SPF/DKIM/DMARC operational checklist. |
| 4 | `BROKER-01` | 5 | P0 | `NOT_STARTED` | No event catalog (`docs/events/`), no schema versioning, no transactional outbox relay/publisher across services, no `inbox_messages` idempotent consumer, no `dead_letter_messages` DLQ + replay API, no duplicate/out-of-order integration tests. |
| 5 | `PHONE-OTP-01` | 3 | P0-GO-LIVE | `PARTIAL` (52-line service) | Basic OTP send/verify exists; missing disposable/virtual phone check, verify old+new when changing phone, `phone_verified` enforcement, PII masking in logs, SMS provider cost/quota cap, and unit/security tests. |
| 6 | `STORAGE-01` | 2 → 3 | P0-GO-LIVE | `PARTIAL` | Face encryption exists in `identity` and upload exists in `listing`, but no unified `ObjectStoragePort`, no private signed short-TTL URL endpoint for ownership docs/contracts, no magic-bytes + size + AV hook, no access audit log or IDOR tests. |
| 7 | `LEDGER-01` | 8 | P0-GO-LIVE | `PARTIAL / NEEDS REFACTOR` | `LedgerService.java` is `455` lines (>250 limit); `postEntry()` lacks ordered `SELECT ... FOR UPDATE` pessimistic lock, `idempotency_key`, reversal-only adjustments, daily balance snapshot table, ArchUnit direct-balance-write guard, and concurrency Testcontainers test. |
| 8 | `LEDGER-02` | 3 | P0-GO-LIVE | `PARTIAL / INSECURE` | Daily reconciliation & MakerChecker exist, but endpoints lack `@PreAuthorize("hasRole('ADMIN')")` & fail-open fallback principals; missing DB SQL permission script / snapshot reconciliation & scheduled job test. |
| 9 | `PAY-01` | 8 | P0-GO-LIVE | `PARTIAL / NEEDS REFACTOR` | Hardcoded VietQR URL to `TO AM ESCROW`; no `PaymentGatewayPort` adapter pattern; no `webhook_logs`; no cancel/expire intent; no underpayment/overpayment/late-payment/`MISMATCH` handling; no kill switch/IP allowlist/HMAC rotation tests. |
| 10 | `ESCROW-01` | 5 | P0-GO-LIVE | `BLOCKED_HUMAN_REVIEW (Policy) / PARTIAL` | `EscrowService` is a 42-line stub moving `2010-ESCROW` -> `2020-WALLET`; missing configurable hold/refund/penalty policy, dispute-freeze check, auto-release cron gated by `fintech.escrow.live-custody-enabled` (`LEGAL-02`), and tests. |
| 11 | `FIN-RISK-01` | 5 | P0-GO-LIVE | `PARTIAL` | `RiskProfile` only checks basic daily withdrawal limit; missing velocity alert queue, bank-change 24h cooldown lock, blacklist, Admin freeze/unfreeze REST endpoints & audit, and unit/security tests. |
| 12 | `REFUND-01` | 3 | P0 | `PARTIAL / UNSAFE` | `PayoutService` auto-completes payouts immediately; dedicated bank `RefundRequest` (`REQUESTED -> PROCESSING -> DONE|FAILED`) is missing; no OTP check, no Maker-Checker above threshold, no bank ownership match. |
| 13 | `PAY-02` | 3 | P0 | `NOT_STARTED` | `LedgerReconciliationScheduler` only checks internal ledger accounts; gateway transaction settlement reconciliation (`webhook_logs` vs `payment_intents` vs provider report), `MISMATCH` status SLA escalation, and accounting report export missing. |
| 14 | `APPT-RENT-BE` | 5 | P0 | `PARTIAL` | Basic CRUD/approve/reject exists; missing phone-verified guard, per-user rate limit, room availability check & pessimistic hold lock, scheduled hold-expiration release, outbox events, and tests. |
| 15 | `CONTRACT-BE` | 5 | P0-GO-LIVE | `PARTIAL` | `ContractService` does NOT verify real OTP (only checks `length == 6`), does NOT record IP/User-Agent/OTP audit log, does NOT generate PDF with OpenPDF + VN fonts + QR, does NOT prevent unauthorized `getContract`/`signContract`, and has 0 tests. |
| 16 | `SAGA-01` | 5 | P0-GO-LIVE | `PARTIAL` | `RentalSagaOrchestrator` is an isolated 88-line CRUD helper without HTTP client / outbox coordination, idempotent replay guard, timeout/stuck-saga detector scheduler, or failure compensation integration tests. |
| 17 | `CONTRACT-02` | 3 | P1 | `PARTIAL` | `ContractAmendment` & `ContractOccupant` schemas exist, but `signAmendment` lacks ownership check, does not apply rent/endDate changes to active `Contract` when both signed, no 30/15/7-day expiry reminder scheduler, and 0 tests. |
| 18 | `ROOM-03` | 3 | P1 | `PARTIAL` | `availableDate` & `minLeaseMonths` added in V14 + basic `criticalChanged` check, but lacks validation on `minLeaseMonths`/`availableDate`, blocks missing on other key fields while `RENTED`, `occupancy_status` API sync tests. |
| 19 | `KYC-03` | 3 | P1 | `PARTIAL` | `PropertyOwnershipDocument` exists, but stores raw `fileUrl` publicly instead of private encrypted storage + signed URL, lacks expiry date & badge revocation on expiry/rejection, lacks IDOR check on `getDocs(roomId)`, and 0 tests. |
| 20 | `METER-01` | 5 | P0 | `PARTIAL` | Only simple `UtilityReadingService` recording prev/curr; missing 3 pricing models (EVN tiered, fixed, per-person), immutable edit/cancel with reason history, lock when invoice `ISSUED`, anomaly surge warning, and tests. |
| 21 | `INVOICE-01/02` | 5 | P0 | `PARTIAL` | Only single `generateInvoice` stub; missing idempotent batch generation across `ACTIVE` contracts (`unique contract_id + period_month`), sequential invoice numbers, immutable `PAID` + credit-note adjustments, partial/overpayment carry-forward, `@Scheduled` overdue alerts, and tests. |
| 22 | `DASH-STATS-BE` | 5 | P0 | `PARTIAL / IDOR` | Uses `journalEntryRepository.findAll().size()` in-memory, missing tenant financial stats, landlord occupancy/revenue series, Redis/in-memory cache with invalidation, CSV UTF-8 BOM export, `@PreAuthorize`, and security tests. |
| 23 | `DASH-FIX-1` | 5 | P0-GO-LIVE | `NOT_STARTED / DEBTS OPEN` | Still uses `src/middleware.ts`, client-readable cookies instead of BFF HttpOnly routes + CSRF Origin check, `Static*Repository` still wired in `container.ts`, hardcoded strings (`737` eslint warnings). |
| 24 | `APPT-RENT-FE` | 5 | P0 | `PARTIAL / MOCKED` | Missing `HttpRentalRequestRepository`; FE rental request & landlord approval views use mock arrays or incomplete API wiring; missing double-submit guard & cancellation policy display. |
| 25 | `CONTRACT-FE` | 5 | P0 | `MOCKED` | `tenant/contracts/[id]/page.tsx` and `landlord/contracts/page.tsx` are 100% hardcoded static mock UI + `setTimeout`; missing API repo, OTP flow, hash/timestamp audit display, PDF download, amendment/occupant UI. |
| 26 | `PAY-FE` | 5 | P0 | `MOCKED` | `tenant/invoices/[id]`, `tenant/payments`, `tenant/deposits`, `landlord/deposits`, `landlord/invoices`, `landlord/utilities` are 100% hardcoded mock UI; missing VietQR polling, real invoice/meter/refund/payout integration; `admin/transactions/page.tsx` needs decomposition (<300 lines) & i18n cleanup. |
| 27 | `SEC-PROD-01` | 5 | P0-GO-LIVE | `PARTIAL` | `TwoFactorAuthService` exists in `identity` with `TotpHelper`, missing TOTP attempt rate limit, backup code consumption verification, 2-admin 2FA recovery flow, CI security scan workflow/scripts, and tests. |
| 28 | `QA-FINTECH` | 5 | P0 | `NOT_STARTED` | 0 Playwright specs for rental-request -> deposit/contract -> invoice -> payment; 0 concurrency double-spend/double-approve tests; 0 saga crash/compensation E2E; no Go/No-Go sign-off checklist. |

---

## 3. Recommended Execution Order (Dependency-Safe Waves)
1. **Wave 1 (Governance & Foundation):** `LEGAL-02` -> `HARDEN-01` -> `DEVOPS-02` -> `BROKER-01` -> `PHONE-OTP-01` -> `STORAGE-01`
2. **Wave 2 (Fintech Core):** `LEDGER-01` -> `LEDGER-02` -> `PAY-01` -> `ESCROW-01` -> `FIN-RISK-01` -> `PAY-02`
3. **Wave 3 (Booking, Contracts & Listing Extensions):** `APPT-RENT-BE` -> `CONTRACT-BE` -> `SAGA-01` -> `CONTRACT-02` -> `ROOM-03` -> `KYC-03`
4. **Wave 4 (Billing, Analytics, Frontend, Security & QA):** `METER-01` -> `INVOICE-01/02` -> `REFUND-01` -> `DASH-STATS-BE` -> `DASH-FIX-1` -> `APPT-RENT-FE` -> `CONTRACT-FE` -> `PAY-FE` -> `SEC-PROD-01` -> `QA-FINTECH`

