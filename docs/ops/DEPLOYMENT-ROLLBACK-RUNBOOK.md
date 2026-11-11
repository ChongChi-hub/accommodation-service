# Deployment & Rollback Runbook — Tổ Ấm (CozyNest)

## 1. Environment Profile Separation
- `dev` / `demo`: Seed migrations (`classpath:db/seed`) and `SeedAssetLoader` active only in non-production profiles.
- `staging` / `prod`: Strictly `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.locations=classpath:db/migration`, Swagger UI disabled (`springdoc.swagger-ui.enabled=false`), `management.endpoint.health.show-details=never` in `prod`.
- Fintech kill switches & legal gate (`LEGAL-02`):
  - `FINTECH_MODE=SANDBOX_ONLY`
  - `FINTECH_LIVE_CUSTODY_ENABLED=false`
  - `FINTECH_LIVE_DISBURSEMENT_ENABLED=false`
  - `PAYMENT_GATEWAY_KILL_SWITCH=false` (set `true` immediately during gateway incident)

## 2. Pre-Deployment Checklist
1. Apply MySQL backups (`mysqldump --single-transaction --routines --triggers`) for `cozynest_identity`, `cozynest_listing`, `cozynest_booking`, `cozynest_billing`, `cozynest_comm`, `cozynest_admin`.
2. Run Flyway migration dry-run via dedicated `svc_migrator` credential against staging mirror.
3. Verify secrets in environment:
   - `INTERNAL_SERVICE_KEY`, `STORAGE_HMAC_SECRET`, `STORAGE_AES_KEY_BASE64`, `PAYMENT_WEBHOOK_SECRET`, RSA keypair (`cozynest-identity-key-1`).
4. Verify no dev/demo seed scripts are present in `classpath:db/migration`.
5. Run smoke suite (`/actuator/health`, `/.well-known/jwks.json`, auth login/refresh, room catalog, ledger health).

## 3. Blue/Green & Rolling Upgrade Order
1. `identity-service` -> 2. `listing-service` -> 3. `booking-service` -> 4. `billing-service` -> 5. `communication-service` -> 6. `admin-service` -> 7. `api-gateway` -> 8. `accommodation-web`.
Always preserve backward-compatible JSON response fields (`ApiResponse<T>`) across N-1 deployments.

## 4. Incident Rollback Procedure (RTO < 15 minutes)
1. **Immediate Financial Halt (if payment/ledger anomaly):**
   - Set `PAYMENT_GATEWAY_KILL_SWITCH=true` on `billing-service` and reload.
   - Do **NOT** delete or mutate `journal_entries` / `journal_lines`; issue reversing journal entries only.
2. **Application Rollback:**
   - Re-deploy previous immutable container digest tag.
   - Wait for `/actuator/health` `UP` across all services.
3. **Database Rollback:**
   - Never drop tables or delete Flyway history rows in `prod`.
   - Deploy forward-fix compensating SQL migration (`V<next>__...`) or restore PITR snapshot if schema corruption occurred prior to live traffic.

## 5. Post-Rollback Verification
- Run ledger zero-sum invariant check (`SUM(debit) == SUM(credit)`).
- Inspect `dead_letter_messages` for failed broker events during deploy window and replay via admin DLQ endpoint.

