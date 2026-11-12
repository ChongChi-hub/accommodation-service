# SPRINT 3 MASTER TRACKER — TỔ ẤM (COZYNEST)

- **Branch:** `feat/sprint-3-complete`
- **Plan Source:** `PROJECT/index.md` (28 Cards / 128 Story Points)
- **Audit Report:** [SPRINT3-FULL-AUDIT.md](../audit/SPRINT3-FULL-AUDIT.md)
- **Last Updated:** 2026-11-11

---

## Card Execution Tracker (28 Cards / 128 Points)

| Wave | Card ID | Owner | Pts | Prio | Audit Baseline Status | Current Status | Verification Evidence |
|---|---|---|---|---|---|---|---|
| 1 | `LEGAL-02` | Dev 1 | 3 | P0-GO-LIVE | Docs written; Model D code conflict | `BLOCKED_HUMAN_REVIEW` | External counsel/accountant/bank pending; live Model B/D gated via flags |
| 1 | `HARDEN-01` | Dev 1 | 5 | P0-GO-LIVE | Debts verified open | `NOT_STARTED` | Pending Wave 1 implementation & tests |
| 1 | `DEVOPS-02` | Dev 5 | 5 | P0-GO-LIVE | Missing staging/prod configs & flags | `NOT_STARTED` | Pending Wave 1 implementation |
| 1 | `BROKER-01` | Dev 5 | 5 | P0 | No relay/DLQ/catalog/inbox | `NOT_STARTED` | Pending Wave 1 implementation & tests |
| 1 | `PHONE-OTP-01` | Dev 5 | 3 | P0-GO-LIVE | Basic stub only | `NOT_STARTED` | Pending Wave 1 hardening & tests |
| 1 | `STORAGE-01` | Dev 3 | 3 | P0-GO-LIVE | Partial face/media only | `NOT_STARTED` | Pending Wave 1 port + signed URL + tests |
| 2 | `LEDGER-01` | Dev 1 | 8 | P0-GO-LIVE | Oversized (455L), unlocked | `NOT_STARTED` | Pending Wave 2 refactor + Testcontainers |
| 2 | `LEDGER-02` | Dev 2 | 3 | P0-GO-LIVE | Missing RBAC/snapshots | `NOT_STARTED` | Pending Wave 2 RBAC + snapshots + tests |
| 2 | `PAY-01` | Dev 1 | 8 | P0-GO-LIVE | Hardcoded VietQR, no logs/port | `NOT_STARTED` | Pending Wave 2 port + webhook + tests |
| 2 | `ESCROW-01` | Dev 2 | 5 | P0-GO-LIVE | Model D stub vs LEGAL-02 | `BLOCKED_HUMAN_REVIEW (Policy)` | Policy engine & sandbox gate scheduled |
| 2 | `FIN-RISK-01` | Dev 2 | 5 | P0-GO-LIVE | Basic limit only | `NOT_STARTED` | Pending Wave 2 velocity/lock/freeze + tests |
| 2 | `PAY-02` | Dev 1 | 3 | P0 | Not started | `NOT_STARTED` | Pending Wave 2 gateway recon + SLA alerts |
| 3 | `APPT-RENT-BE` | Dev 2 | 5 | P0 | Basic CRUD only | `NOT_STARTED` | Pending Wave 3 guards + lock + tests |
| 3 | `CONTRACT-BE` | Dev 2 | 5 | P0-GO-LIVE | No real OTP/PDF/audit/tests | `NOT_STARTED` | Pending Wave 3 OpenPDF + OTP + hash + tests |
| 3 | `SAGA-01` | Dev 1 | 5 | P0-GO-LIVE | Isolated entity only | `NOT_STARTED` | Pending Wave 3 orchestration + chaos tests |
| 3 | `CONTRACT-02` | Dev 2 | 3 | P1 | Missing apply/scheduler/tests | `NOT_STARTED` | Pending Wave 3 amendment apply + cron + tests |
| 3 | `ROOM-03` | Dev 3 | 3 | P1 | Partial fields, incomplete lock | `NOT_STARTED` | Pending Wave 3 validation + RENTED lock + tests |
| 3 | `KYC-03` | Dev 3 | 3 | P1 | Public URL, no expiry/IDOR guard | `NOT_STARTED` | Pending Wave 3 private storage + review + tests |
| 4 | `METER-01` | Dev 4 | 5 | P0 | Basic single reading only | `NOT_STARTED` | Pending Wave 4 tiered tariff + history + tests |
| 4 | `INVOICE-01/02` | Dev 4 | 5 | P0 | Single stub only | `NOT_STARTED` | Pending Wave 4 batch + credit note + cron + tests |
| 4 | `REFUND-01` | Dev 4 | 3 | P0 | Auto-complete payout, no Refund flow | `NOT_STARTED` | Pending Wave 4 refund + payout OTP + MakerChecker |
| 4 | `DASH-STATS-BE` | Dev 4 | 5 | P0 | Missing tenant/occupancy/RBAC/cache | `NOT_STARTED` | Pending Wave 4 stats + BOM CSV + security tests |
| 4 | `DASH-FIX-1` | Dev 3 | 5 | P0-GO-LIVE | middleware.ts + static mocks + 737 i18n warns | `NOT_STARTED` | Pending Wave 4 `proxy.ts` + BFF + repo wiring |
| 4 | `APPT-RENT-FE` | Dev 3 | 5 | P0 | Incomplete/mocked | `NOT_STARTED` | Pending Wave 4 real API + idempotency UI |
| 4 | `CONTRACT-FE` | Dev 3 | 5 | P0 | 100% hardcoded mock UI | `NOT_STARTED` | Pending Wave 4 API + OTP + PDF + amendments |
| 4 | `PAY-FE` | Dev 4 | 5 | P0 | 100% hardcoded mock UI | `NOT_STARTED` | Pending Wave 4 VietQR/invoice/meter/refund UI |
| 4 | `SEC-PROD-01` | Dev 5 | 5 | P0-GO-LIVE | Partial TOTP, missing recovery/CI | `NOT_STARTED` | Pending Wave 4 2-admin recovery + rate limit + CI |
| 4 | `QA-FINTECH` | Dev 5 | 5 | P0 | Not started | `NOT_STARTED` | Pending Wave 4 E2E + concurrency + go/no-go |

---

## Story Points Summary
- **Total Planned:** 28 Cards / 128 Story Points (`Dev 1: 32`, `Dev 2: 26`, `Dev 3: 24`, `Dev 4: 23`, `Dev 5: 23`).
- **Completed & Verified in Phase 2:** `0 / 28` cards (`Phase 1 Audit Complete`).

