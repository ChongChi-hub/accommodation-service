# LEGAL-02 Implementation & Feature Gate Notice

- **Date:** 2026-11-11
- **Status:** `BLOCKED_HUMAN_REVIEW` (External lawyer, tax accountant & licensed intermediary bank/PG sign-off pending)
- **Related:** `ADR-008`, `ADR-010`, [LEGAL-02-DECISION-MATRIX.md](./LEGAL-02-DECISION-MATRIX.md)

## Discrepancy Found During Sprint 3 Audit
Commit `82c4c49` / `d862d9d` introduced Model D double-entry accounts (`2010-ESCROW`, `2020-WALLET-<landlordId>`), automatic 5% fee split postings, auto-completing `PayoutService`, and a hardcoded VietQR image referencing `TO AM ESCROW` inside `billing-service`. Meanwhile, FE pages claim compliance with Decree 52/2024/ND-CP on mocked data.

Per `ADR-008`, **Model D (company holding tenant deposits / in-house wallet custody without NHNN Payment Intermediary license)** is **REJECTED** for live money operations.

## Mandatory Engineering Guardrails Implemented in Sprint 3
1. **Default Safe Mode (`fintech.mode=SANDBOX_ONLY`):**
   - `fintech.escrow.live-custody-enabled=false`
   - `fintech.payout.live-disbursement-enabled=false`
   - `fintech.settlement-model=SANDBOX_P2P_VIETQR` (or `LICENSED_PG_SPLIT` once licensed PG contract + counsel sign-off are provided).
2. **No Hardcoded Live/Escrow Bank Details:** Remove hardcoded `TO AM ESCROW` bank info; route QR generation through `PaymentGatewayPort` adapter driven by configuration.
3. **Neutral Ledger Accounting Architecture:** Keep `LEDGER-01` / `LEDGER-02` as an internal reconciliation & sandbox simulation engine using integer `BIGINT` VND and double-entry invariants. Live payout disbursement and automated custody release remain disabled in `prod` profile until human sign-off (`LEGAL-02`).
4. **External Human Gates (`BLOCKED_HUMAN_REVIEW`):**
   - Lawyer legal opinion on Decree 52/2024/ND-CP, Circular 40/2021/TT-BTC, Electronic Transactions Law 2023.
   - Licensed Payment Gateway / Collection Partner contract (Model B/A) and Tax E-Invoice decision (`TAX-EINV-01` in Sprint 4).

