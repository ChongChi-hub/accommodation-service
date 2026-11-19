# TIEN DO THUC HIEN [WALLET-VERIFY-LEDGER]

- **Nhanh git:** `feat/wallet-verify-ledger` (tao tu `feat/payment-harden` tren ca 2 repo `accommodation-service` va `accommodation-web`).
- **Sao luu DB truoc khi sua/test:** Da sao luu `cozynest_billing`, `cozynest_booking`, `cozynest_identity` (27,457,608 bytes).
- **Tai khoan san duy nhat:** Vietcombank (`VCB` / BIN `970436`), chu tai khoan `LE THANH CUONG`, so tai khoan `******1900` (doc tu cau hinh `fintech.platform.*`, thay doi duoc khong can sua ma).

---

## 1. PHA 0: DOC, KIEM TOAN & BASELINE (CHUA SUA MA)

### 1.1. [CHAY LAI HOM NAY] Tai hien loi bien lai cu va ma giao dich bat ky
- **Hien trang truoc sua:** `PaymentService.submitIntent` truc tiep goi `intent.markPaid` va ghi so kep khi khach bam "Toi da chuyen", khien bien lai cu `PAY1795086207734C208` van cong tien cho giao dich moi `PAY-1795088282534-7604`.
- **Nguyen nhan goc:** `SHA-256` byte tep chi chan khi tai cung tep nhi phan, khong chan re-screenshot/crop hoac bien lai cu chua tung luu, va client nhap ma GD bat ky deu lot.

### 1.2. [DOC TU MA] 8 duong tao trang thai xac nhan / cong tien (Truoc vs Sau)

| # | Duong / Endpoint / Job | Truoc sua | Sau sua `[WALLET-VERIFY-LEDGER]` |
| :- | :--- | :--- | :--- |
| 1 | `POST /api/v1/billing/payments/intent/{code}/submit` | Tu chot `PAID` + cong `2010-ESCROW` + `HELD` | Chi chuyen `SUBMITTED` ("Dang cho xac minh tien ve"), luu bang chung ho tro (Magic Bytes + SHA-256 + dHash + AES-256-GCM), **KHONG** cong tien |
| 2 | `POST /api/v1/billing/payments/intent/{code}/confirm` | `hasAnyRole('LANDLORD','ADMIN')` | Khoa cung `hasRole('ADMIN')`, ghi chung `bank_transaction_registry` UNIQUE |
| 3 | `POST /api/v1/billing/payments/webhook/provider` (moi) | Chua co `NotificationProvider` | Xac minh HMAC-SHA256 hang thoi gian + timestamp + chieu `IN` + STK san + `providerTxId` UNIQUE; khop dung moi chot `PAID`; lech tien/het han/sai ma -> `unmatched_bank_inflows` (`200 OK`) |
| 4 | `POST /api/v1/billing/payments/manual-confirm` (moi) | Chua co | Chi `ADMIN`, bat buoc `bankTxCode` UNIQUE + ly do + Maker-Checker khi `>= threshold` |
| 5 | `POST /api/v1/billing/payments/sandbox/fake-inflow` | `simulate-webhook` khong kiem tra `fintech.mode` | Chi hoat dong khi `ADMIN` va `fintech.mode=SANDBOX_ONLY`, tra `404` o che do khac |
| 6 | `POST /api/v1/billing/invoices/{id}/payments` | `isAuthenticated()` goi thang danh dau `PAID` | Khoa `hasRole('ADMIN')` |
| 7 | `EscrowService` (`confirmMoveIn` & `autoReleaseMaturedHolds`) | Tu giai ngan 100% du chua du 2 xac nhan | Chi giai phong `deposit.release.percent` (50%) khi du **ca 2 xac nhan**; giu 50% den het HD; `withdrawal.hold.days` chuyen `2025-PENDING -> 2020-WALLET` |
| 8 | `POST /api/v1/billing/payouts` | `<= 10tr` tu `COMPLETED` + `CR 1010-GATEWAY` | `Idempotency-Key` + TK `MATCHED` -> `DR 2020-WALLET / CR 2030-WDHOLD` (`REQUESTED`/`PENDING_APPROVAL`); chi `PAID` + `CR 1010-GATEWAY` khi Admin goi `confirm-paid` kem `bankTxCode` UNIQUE |

---

## 2. TRANG THAI CAC PHA (PHA 0 -> PHA 5)

| Pha | Hang muc chinh | Trang thai | Minh chung |
| :--- | :--- | :--- | :--- |
| **Pha 0** | Doc ma, kiem toan 8 duong cong tien, sao luu DB, lay Baseline BE/FE | **DONE-VERIFIED** | Baseline: BE 7/8 module pass, `:billing-service` 49/51; FE lint/tsc/101 vitest pass, Playwright 58/61 |
| **Pha 1** | Ma tham chieu `TOAM...` (19 ky tu, khong gach), `NotificationProvider` (`FAKE`/`PAYOS`/`SEPAY`/`CASSO`), Webhook xac minh tien that + `unmatched_bank_inflows`, Admin manual confirm + Maker-Checker, doi y nghia "Toi da chuyen" (`SUBMITTED`), kho bang chung rieng tu AES-256-GCM + dHash + RBAC `404` + access log | **DONE-VERIFIED** (FAKE provider) / **HUMAN** (ket noi PayOS live key) | Flyway `V11` `success=1`; `WalletVerifyLedgerEndToEndTest`, `WalletFraudPreventionTest`, `BillingSecurityMatrixTest`, `BillingArchitectureTest`, `PaymentHardeningTest` PASS |
| **Pha 2** | So cai 6 nhom tai khoan (`1010`, `2010`, `2020`, `2025`, `2030`, `4010`), giai phong 50% coc (`deposit.release.percent=50`) + `withdrawal.hold.days`, hoan/khau tru 50% cuoi HD, hoa hong 5% (`HALF_UP`) tren **TONG** tien thue + dien | **DONE-VERIFIED** | Test 1.000.000 + 200.000 dien = 60.000 hoa hong + 1.140.000 vi chu tro; coc 1.000.000 -> 500.000 vi + 500.000 giu lai, hoa hong 0 |
| **Pha 3** | Rut tien (`REQUESTED`/`PENDING_APPROVAL`/`APPROVED`/`PAID`/`REJECTED`/`CANCELLED`), khoa dong `SELECT FOR UPDATE`, QR VietQR do he thong sinh, API `/wallet/summary` & `/wallet/ledger` | **DONE-VERIFIED** | Test rut tien, huy rut, tu choi hoan so du, trung `bankTxCode` bi chan, rut song song khong am so du |
| **Pha 4** | FE Khach (`SUBMITTED`, khoa nhan phong, hien 50% coc giu lai, ma rut gon), Chu tro (`LandlordWalletPanel` 4 KPI + lich su tach hoa hong 5%, huy lenh rut), Admin (`AdminInflowAndWithdrawalTab` + `AdminPayoutQrModal` QR he thong), Vitest + Playwright + chup anh 1440/390 sang/toi + sua loi UI | **DONE-VERIFIED** | `eslint --max-warnings=0`, `tsc --noEmit`, `verify-i18n` (2266/2266), 23/23 Vitest (105 tests), Docker `next build` 206 trang, Playwright `wallet-verify-ledger.spec.ts` 3/3 PASS |
| **Pha 5** | Full test BE/FE khong phat sinh loi moi, reset 2 HD test bang but toan dao (khong `DELETE` so cai) va giu nguyen dau van tay bien lai cu, cap nhat `DECISIONS.md` (`LEGAL-02 = HUMAN`) | **DONE-VERIFIED** (ky thuat) / **HUMAN** (`LEGAL-02` & thu 1.000 d PayOS that) | `JE-REV-2026-F15A7BF9` (15.000 d) + `JE-REV-2026-90289A1E` (30.000 d); `DEBIT == CREDIT == 4.110.000`; `1010 == 2010 == 4.020.000` |

---

## 3. CHECKLIST THU CHUYEN KHOAN THAT 1.000 d QUA PAYOS (HUMAN)

1. Dang nhap cong PayOS, tao kenh webhook tro toi `POST https://<domain>/api/v1/billing/payments/webhook/provider`.
2. Nap bien moi truong (KHONG commit vao git): `FINTECH_WEBHOOK_PROVIDER=PAYOS`, `FINTECH_WEBHOOK_SECRET=<checksum_key_payos>`, `FINTECH_PLATFORM_BANK_CODE=VCB`, `FINTECH_PLATFORM_ACCOUNT_NUMBER=<stk_san>`, `FINTECH_PLATFORM_ACCOUNT_HOLDER=<ten_chu_tk>`.
3. Khach tao QR cho 1 trong 2 hop dong test da reset (`HD-2026-F15A7BF9` 5.000 d hoac `HD-2026-90289A1E` 10.000 d) va thu:
   - Thu tai lai anh bien lai cu / ma GD `1025874046638834` -> he thong phai tu choi (khong duoc dung lai).
   - Chuyen khoan that moi dung noi dung `TOAM...` va dung so tien -> PayOS gui webhook -> he thong tu dong chuyen `HELD` va mo khoa 2 nut ban giao / nhan phong.
   - Chuyen thu lech so tien 1.000 d -> webhook tra `200 OK`, khong tu cong tien, ghi vao hang doi `"Tien ve chua khop"` de Admin xu ly.
4. Hoan tat `LEGAL-02` voi luat su va tu van thue truoc khi bat thu tien thuong mai.
