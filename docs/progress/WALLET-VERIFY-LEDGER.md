# TIEN DO THUC HIEN [WALLET-VERIFY-LEDGER]

- **Nhanh git:** `feat/wallet-verify-ledger` (tao tu `feat/payment-harden` tren ca 2 repo `accommodation-service` va `accommodation-web`)
- **Sao luu DB truoc khi sua/test:** Da sao luu toan bo `cozynest_billing`, `cozynest_booking`, `cozynest_identity` (27,457,608 bytes).
- **Tai khoan san duy nhat:** Vietcombank (`VCB` / BIN `970436`), chu tai khoan `LE THANH CUONG`, so tai khoan `******1900` (doc tu cau hinh).

---

## 1. PHA 0: DOC VA BASELINE (CHUA SUA MA NGUON)

### 1.1. [CHAY LAI HOM NAY] Tai hien loi bien lai cu va ma giao dich bat ky
- **Hien trang:** Tai `PaymentService.submitIntent` (`POST /api/v1/billing/payments/intent/{paymentCode}/submit`), khi khach thue gui `note: "BANK_TX:...|HASH:..."` va `receiptUrl`, he thong truc tiep goi `intent.markPaid(intent.getAmount())` va `ledgerPoster.postLedgerForPaymentWithProof(...)`, cong tien vao `1010-GATEWAY` / `2010-ESCROW` va chuyen `EscrowHold` sang `HELD`.
- **Ca that da ghi nhan:** Bien lai tai len co noi dung chuyen khoan cu `PAY1795086207734C208` (luc 18:04), trong khi ma cua khoan thanh toan moi la `PAY-1795088282534-7604` (tao sau do), nhung he thong van bao `"Da kiem tra chong trung lap"` va cho phep cong tien ngay khi khach bam `"Toi da chuyen"`.
- **Nguyen nhan goc:** Kiem tra `SHA-256` tren byte tep chi chan khi tai len cung 1 tep nhi phan y het, hoan toan khong chan duoc anh chup man hinh lai (re-screenshot), anh cat cup, hoac anh bien lai cua giao dich khac chua tung luu hash; dong thoi client nhap ma giao dich bat ky chua tung xuat hien trong DB deu vuot qua. Bang chung khach tai len khong bao gio duoc phep la can cu cong tien.

### 1.2. [DOC TU MA] Bang liet ke MOI duong tao trang thai da xac nhan / cong escrow / ghi so

| # | Duong / Endpoint / Job | File & Phuong thuc | Dieu kien hien tai | Van de / Huong xu ly Pha 1-3 |
| :- | :--- | :--- | :--- | :--- |
| 1 | `POST /api/v1/billing/payments/intent/{code}/submit` | `PaymentController.submitIntent` -> `PaymentService.submitIntent` | `isAuthenticated()` la `payerId`, gui `note` + `receiptUrl` | **LO HONG:** Khach tu chuyen sang `PAID` va cong Escrow. **Sua:** Chi chuyen sang `SUBMITTED` (`Cho xac minh tien ve`), luu bang chung ho tro, KHONG cong tien, KHONG mo ban giao. |
| 2 | `POST /api/v1/billing/payments/intent/{code}/confirm` | `PaymentController.confirmPayment` -> `PaymentService.confirmPaymentByLandlordOrAdmin` | `hasAnyRole('LANDLORD', 'ADMIN')` | **LO HONG:** Cho phep ca `LANDLORD` tu xac nhan tien ve tai khoan san. **Sua:** Chi `ADMIN` duoc xac nhan thu cong du phong kem `bankTxCode` UNIQUE trong registry chung + Maker-Checker tu nguong cau hinh. |
| 3 | `POST /api/v1/billing/payments/webhook` | `PaymentController.handleWebhook` -> `PaymentWebhookProcessor.processWebhook` | Public + `X-Signature` HMAC + `X-Timestamp` (300s) | **HAN CHE:** Chua co `NotificationProvider` (`SePay`/`Casso`/`payOS`/`FAKE`), chua kiem tra chieu `IN`, STK san `******1900`, `providerTxId` UNIQUE, va hang doi `"Tien ve chua khop"`. |
| 4 | `POST /api/v1/billing/payments/sandbox/simulate-webhook` | `PaymentController.simulateSandboxWebhook` | `hasRole('ADMIN')` va profile khac `prod` | **HAN CHE:** Phai khoa chat chi ton tai/chay khi `fintech.mode=SANDBOX_ONLY`, chan tuyet doi o profile real. |
| 5 | `POST /api/v1/billing/invoices/{id}/payments` | `InvoiceController.recordPayment` -> `InvoiceService.recordPayment` | `isAuthenticated()` (tenant/landlord/admin) | **LO HONG:** Cho phep goi thang API danh dau hoa don `PAID` khong qua webhook/admin va khong ghi so cai. **Sua:** Khoa chi cho Admin/he thong noi bo hoac bat buoc qua luong webhook/admin confirm. |
| 6 | `@Scheduled autoReleaseMaturedHolds` | `EscrowService.autoReleaseMaturedHolds` | Cron `0 15 * * * ?` khi `inspection_ends_at < NOW()` | **LO HONG:** Tu giai ngan 100% coc du chua du 2 xac nhan. **Sua:** Tat tu dong giai ngan khi chua du 2 xac nhan; chi giai phong 50% khi du 2 xac nhan va chuyen `LANDLORD_PENDING -> LANDLORD_AVAILABLE` sau `withdrawal.hold.days`. |
| 7 | `POST /api/v1/billing/escrow/{contractId}/release` | `EscrowController.releaseDeposit` -> `EscrowService.disburseDeposit` | `hasRole('ADMIN')` | **HAN CHE:** Dang giai ngan 100% coc. **Sua:** Khi du 2 xac nhan (ban giao + nhan phong) tu dong giai phong `deposit.release.percent` (50%) vao vi chu tro, giu 50% den het hop dong. |
| 8 | `POST /api/v1/billing/payouts` | `PayoutController.requestPayout` -> `PayoutService.requestPayout` | `hasAnyRole('LANDLORD', 'ADMIN')` | **LO HONG:** Khi `<= 10,000,000 VND`, tu dong ghi `DR WALLET / CR GATEWAY` va `COMPLETED` ngay. **Sua:** Ghi `DR LANDLORD_AVAILABLE / CR WITHDRAWAL_HOLD` (`REQUESTED`), cho Admin chuyen khoan that va bam xac nhan kem `bankTxCode` UNIQUE. |

### 1.3. [DOC TU MA] & [CHAY LAI HOM NAY] Hien trang so cai, ma tham chieu va hoa don
- **Tai khoan so cai hien co (`ledger_accounts`):**
  - `1010-GATEWAY` (`ASSET`, so du DB hien tai: `4,065,000 VND`)
  - `2010-ESCROW` (`LIABILITY`, so du DB hien tai: `4,065,000 VND`)
  - `2020-WALLET-{userId}` (`LIABILITY`, so du kha dung chu tro)
  - `4010-REVENUE` (`REVENUE`, doanh thu hoa hong san, so du `0 VND`)
  - `5010-EXPENSE` (`EXPENSE`, chi phi van hanh, so du `0 VND`)
  - **Con thieu:** `ACC_ESCROW` phan theo hop dong (`2010-ESCROW-{contractId}`), `ACC_LANDLORD_PENDING` (`2025-PENDING-{landlordId}`), `ACC_WITHDRAWAL_HOLD` (`2030-WDHOLD-{landlordId}`).
- **Cach tinh so du (`LedgerAccount.applyPosting`):** `ASSET`/`EXPENSE` tang khi `DEBIT`, giam khi `CREDIT`; `LIABILITY`/`EQUITY`/`REVENUE` tang khi `CREDIT`, giam khi `DEBIT`. Khoa dong `SELECT ... FOR UPDATE` theo thu tu tu dien `account_number`, trigger MySQL V3 chan `UPDATE`/`DELETE` tren `journal_entries` va `ledger_postings`.
- **Ma noi dung chuyen khoan hien tai:** Sinh theo dinh dang `PAY-{millis}-{4hex}` (22 ky tu, co 2 dau gach ngang `-`). Nhieu ngan hang tu luoc bo dau gach (thanh `PAY1795086207734C208`) hoac cat ngan noi dung chuyen khoan lien ngan hang, gay kho khop tu dong.
- **Hoa don thang & tien dien (`invoices` & `PaymentLedgerPoster.postInvoice`):** Da co bang `invoices` (`base_rent`, `electricity_amount`, `water_amount`, `service_amount`, `other_amount`, `total_amount`), nhung `postInvoice` dang tinh 5% tren `baseRent` thay vi 5% (`commission.percent`, lam tron nua len `HALF_UP`) tren **TONG** (`totalAmount`).

### 1.4. [CHAY LAI HOM NAY] Ket qua Baseline truoc khi sua
- **Backend (`$env:CI='true'; .\gradlew.bat clean test --no-build-cache --continue`):**
  - `:common:test`, `:api-gateway:test`, `:identity-service:test`, `:listing-service:test`, `:booking-service:test`, `:communication-service:test`, `:admin-service:test`: **PASSED 100%**.
  - `:billing-service:test`: 51 tests run, **49 passed, 2 failed** (`PaymentHardeningTest` va `BillingArchitectureTest` do `BankAccountService` phu thuoc truc tiep `infrastructure`). Se khac phuc triet de trong Pha 1.
- **Frontend (`accommodation-web`):**
  - `npx eslint . --max-warnings=0`: **PASS (0 errors, 0 warnings)**.
  - `npx tsc --noEmit`: **PASS (0 errors)**.
  - `npm test -- --run`: **PASS (22/22 files, 101/101 tests, i18n parity 2266/2266 keys vi/en, 98/98 links, 224/224 images)**.
  - `npm run build` (qua Docker image build): **PASS (206 static/dynamic pages)**.
  - `npx playwright test e2e/smoke.spec.ts e2e/auth-rbac-middleware.spec.ts e2e/qa-fintech-sprint3.spec.ts`: **58 passed**, 3 failed (do mock cu o `qa-fintech-sprint3.spec.ts`).

---

## 2. KE HOACH Va TIEN DO CAC PHA

| Pha | Hang muc chinh | Trang thai | Ghi chu / Minh chung |
| :--- | :--- | :--- | :--- |
| **Pha 0** | Doc ma, tai hien loi bien lai cu, liet ke duong cong tien, lay Baseline BE/FE | **DONE-VERIFIED** | `[CHAY LAI HOM NAY]` & `[DOC TU MA]` day du |
| **Pha 1** | Ma tham chieu `TOAM...` (<=23 ky tu, khong gach), Webhook `NotificationProvider` (`FAKE`/`PayOS`/`SePay`/`Casso`), Admin confirm + Maker-Checker, Doi y nghia "Toi da chuyen" (`SUBMITTED`), Kho bang chung rieng tu + dHash + Registry UNIQUE, Doi soat hang ngay | **IN-PROGRESS** | Provider that chua co khoa: `HUMAN`, test bang `FAKE` provider |
| **Pha 2** | So cai 6 nhom tai khoan, Coc giu 50% (`deposit.release.percent=50`) & cho `withdrawal.hold.days`, Hoan coc cuoi HD, Hoa hong 5% (`commission.percent=5`, `HALF_UP`) tren tong tien tro + dien | **NOT-DONE** | Tat ca tham so doc tu cau hinh |
| **Pha 3** | Rut tien (`REQUESTED -> APPROVED -> PAID / REJECTED / CANCELLED`), `DR AVAILABLE / CR WITHDRAWAL_HOLD`, QR he thong tu sinh, Admin confirm kem `bankTxCode` UNIQUE | **NOT-DONE** | Khoa dong `SELECT FOR UPDATE` + idempotency |
| **Pha 4** | Frontend Khach / Chu tro (Trang Vi, Rut tien, Bang chung) / Admin (Hang doi chua khop, Duyet rut + QR, Maker-Checker, Doi soat), i18n vi/en 100%, responsive & screenshots | **NOT-DONE** | 1440 & 390, sang & toi, tu phe binh 3 diem |
| **Pha 5** | Nghiem thu E2E (provider FAKE), Kich ban gian lan, Checklist 1.000 d (`HUMAN`), Cap nhat `DECISIONS.md` (`LEGAL-02 = HUMAN`), Reset 2 HD test bang but toan dao | **NOT-DONE** | Bao cao cuoi & ve sinh git/DB |

