# BÃO CÃO KIá»‚M CHá»¨NG VÃ€ Äáº¶C Táº¢ THI CÃ”NG: [KYC-02] XÃC THá»°C KHUÃ”N Máº¶T

**Nhiá»‡m vá»¥:** [KYC-02] XÃ¡c thá»±c khuÃ´n máº·t (Face Verification) cho Chá»§ trá»  
**Phá»¥ thuá»™c:** KYC-01 Ä‘Ã£ Ä‘Æ°á»£c kiá»ƒm chá»©ng  
**NgÃ y thá»±c hiá»‡n:** 08/11/2026  
**Tráº¡ng thÃ¡i:** HOÃ€N THÃ€NH TOÃ€N DIá»†N (100% GREEN)

---

## 1. NGUYÃŠN Táº®C Cá»T LÃ•I VÃ€ QUYáº¾T Äá»ŠNH THIáº¾T Káº¾ (DESIGN DECISIONS)

1. **Báº£o vá»‡ Dá»¯ liá»‡u Sinh tráº¯c há»c Nháº¡y cáº£m:**
   - Dá»¯ liá»‡u khuÃ´n máº·t thuá»™c danh má»¥c **dá»¯ liá»‡u cÃ¡ nhÃ¢n nháº¡y cáº£m** theo quy Ä‘á»‹nh phÃ¡p luáº­t (Nghá»‹ Ä‘á»‹nh 13/2023/NÄ-CP).
   - **Quyáº¿t Ä‘á»‹nh dá»©t khoÃ¡t:** Ná»n táº£ng **KHÃ”NG tá»± viáº¿t thuáº­t toÃ¡n nháº­n diá»‡n khuÃ´n máº·t** hay lÆ°u trá»¯ vector embedding / mÃ´ hÃ¬nh khuÃ´n máº·t.
   - Quyáº¿t Ä‘á»‹nh cuá»‘i cÃ¹ng do kiá»ƒm duyá»‡t viÃªn Ä‘á»‘i chiáº¿u thá»§ cÃ´ng (provider `manual`, máº·c Ä‘á»‹nh) hoáº·c nhÃ  cung cáº¥p dá»‹ch vá»¥ eKYC Ä‘Æ°á»£c cáº¥p phÃ©p bÃªn ngoÃ i (provider `vendor`, tÃ¹y chá»n qua cá»•ng cáº¯m API).
2. **Vai trÃ² cá»§a Kiá»ƒm tra trong TrÃ¬nh duyá»‡t:**
   - Bá»™ phÃ¡t hiá»‡n/hÆ°á»›ng dáº«n trong trÃ¬nh duyá»‡t (Webcam scanner vá»›i khung elip vÃ  Ä‘áº¿m ngÆ°á»£c) **chá»‰ Ä‘á»ƒ hÆ°á»›ng dáº«n ngÆ°á»i dÃ¹ng thá»±c hiá»‡n Ä‘Ãºng Ä‘á»™ng tÃ¡c**, hoÃ n toÃ n **KHÃ”NG PHáº¢I lÃ  biá»‡n phÃ¡p báº£o máº­t** vÃ  **TUYá»†T Äá»I KHÃ”NG ÄÆ¯á»¢C mÃ´ táº£ lÃ  "chá»‘ng giáº£ máº¡o"**.
   - Cá» `liveness_client_flag` chá»‰ mang tÃ­nh cháº¥t tham kháº£o cho kiá»ƒm duyá»‡t viÃªn, khÃ´ng dÃ¹ng Ä‘á»ƒ tá»± Ä‘á»™ng duyá»‡t há»“ sÆ¡.
3. **Quyá»n riÃªng tÆ° vÃ  Giáº£m thiá»ƒu Dá»¯ liá»‡u (Data Minimization):**
   - Chá»‰ thu tháº­p Ä‘Ãºng sá»‘ khung hÃ¬nh thá»­ thÃ¡ch tá»‘i thiá»ƒu (1 áº£nh trung tÃ­nh + 2 áº£nh hÃ nh Ä‘á»™ng).
   - MÃ£ hÃ³a AES-GCM 256-bit vá»›i khÃ³a Ä‘á»™c láº­p `FACE_ENCRYPTION_KEY` trÆ°á»›c khi lÆ°u vÃ o kho riÃªng tÆ°.
   - Tuyá»‡t Ä‘á»‘i khÃ´ng log áº£nh, base64 hay Ä‘Æ°á»ng dáº«n lÆ°u trá»¯ khung hÃ¬nh.
   - Tá»± Ä‘á»™ng tiÃªu há»§y khung hÃ¬nh sau 30 ngÃ y (`FACE_RETENTION_DAYS=30`) Ä‘á»‘i vá»›i cÃ¡c há»“ sÆ¡ Ä‘Ã£ cÃ³ káº¿t quáº£ (PASSED/FAILED). KhÃ´ng bao giá» xÃ³a há»“ sÆ¡ PENDING.
   - NgÆ°á»i dÃ¹ng cÃ³ toÃ n quyá»n rÃºt láº¡i sá»± Ä‘á»“ng Ã½ vÃ  yÃªu cáº§u xÃ³a ngay láº­p tá»©c má»i khung hÃ¬nh Ä‘Ã£ lÆ°u qua API `DELETE /api/v1/me/kyc/face/consent`.

---

## 2. CHECKLIST 5 Háº NG Má»¤C THEO THáºº TRELLO

- [x] **(1) Thu selfie cÃ³ thá»­ thÃ¡ch hÃ nh Ä‘á»™ng:**
  - `POST /api/v1/me/kyc/face/challenges`: Sinh ngáº«u nhiÃªn 2 hÃ nh Ä‘á»™ng (`CHOP_MAT`, `QUAY_TRAI`, `QUAY_PHAI`, `MO_MIENG`), nonce dÃ¹ng má»™t láº§n cÃ³ hiá»‡u lá»±c 2 phÃºt.
  - Äiá»u kiá»‡n: ÄÃ£ ná»™p giáº¥y tá» tÃ¹y thÃ¢n CCCD vÃ  Ä‘á»“ng Ã½ riÃªng vá»›i Ä‘iá»u khoáº£n sinh tráº¯c há»c.
  - `POST /api/v1/me/kyc/face/submissions`: Nháº­n 1 khung trung tÃ­nh + 2 khung hÃ nh Ä‘á»™ng, validate Ä‘á»‹nh dáº¡ng (JPEG/WebP, magic bytes, <= 1MB, kÃ­ch thÆ°á»›c Ä‘iá»ƒm áº£nh chuáº©n, xÃ³a EXIF), rate limit tá»‘i Ä‘a 3 láº§n/24h.
- [x] **(2) LÆ°u trá»¯ an toÃ n:**
  - Báº£ng DDL `face_verifications`, `face_verification_frames`, `face_access_logs`.
  - KhÃ³a AES-GCM 256-bit riÃªng (`FACE_ENCRYPTION_KEY`), yÃªu cáº§u báº¯t buá»™c á»Ÿ production.
  - áº¢nh seed lÃ  hÃ¬nh bÃ³ng ngÆ°á»i trá»«u tÆ°á»£ng kÃ¨m dÃ²ng chá»¯ báº£n quyá»n `MáºªU - KHÃ”NG PHáº¢I KHUÃ”N Máº¶T THáº¬T`, khÃ´ng dÃ¹ng áº£nh ngÆ°á»i tháº­t hay áº£nh AI sinh.
- [x] **(3) Kiá»ƒm duyá»‡t Ä‘á»‘i chiáº¿u CCCD:**
  - `GET /api/v1/admin/kyc/{id}/face` vÃ  stream tá»«ng khung hÃ¬nh vá»›i `Cache-Control: no-store`.
  - Má»—i lÆ°á»£t xem khung hÃ¬nh ghi Ä‘Ãºng 1 dÃ²ng nháº­t kÃ½ kiá»ƒm toÃ¡n khÃ´ng thá»ƒ xÃ³a (`face_access_logs`).
  - Giao diá»‡n Ä‘á»‘i chiáº¿u side-by-side giá»¯a CCCD máº·t trÆ°á»›c vÃ  cÃ¡c khung hÃ¬nh, áº£nh lÃ m má» máº·c Ä‘á»‹nh (`blur-md`), chá»‰ hiá»ƒn thá»‹ khi ngÆ°á»i duyá»‡t chá»§ Ä‘á»™ng báº¥m xem.
  - NÃºt Äáº¡t (`PASSED`) / KhÃ´ng Ä‘áº¡t (`FAILED`) vá»›i bá»™ mÃ£ lÃ½ do chuáº©n hÃ³a: `KHONG_KHOP_GIAY_TO`, `NGHI_NGO_GIA_MAO`, `ANH_KEM_CHAT_LUONG`, `KHAC`. KhÃ³a láº¡c quan `@Version` chá»‘ng xung Ä‘á»™t 409, cáº¥m kiá»ƒm duyá»‡t viÃªn tá»± duyá»‡t há»“ sÆ¡ cá»§a chÃ­nh mÃ¬nh (403/409).
- [x] **(4) TÃ­ch há»£p vÃ o tráº¡ng thÃ¡i KYC:**
  - KYC tá»•ng thá»ƒ `APPROVED = docApproved && facePassed` khi `FACE_REQUIRED=true` (máº·c Ä‘á»‹nh).
  - Cá»•ng kiá»ƒm tra `listing-service` gá»i `InternalUserService.getKycStatus(userId)`, chá»‰ tÃ i khoáº£n cÃ³ `overallApproved=true` má»›i Ä‘Æ°á»£c Ä‘Äƒng tin phÃ²ng.
  - Giao diá»‡n `LandlordKycStatusCard` hiá»ƒn thá»‹ tiáº¿n Ä‘á»™ 2 pháº§n (Giáº¥y tá» CCCD & XÃ¡c thá»±c khuÃ´n máº·t) cho cáº£ 4 tráº¡ng thÃ¡i.
  - `KycGateBanner` trÃªn trang phÃ²ng nháº¯c Ä‘Ãºng pháº§n cÃ²n thiáº¿u (giáº¥y tá» hoáº·c khuÃ´n máº·t).
- [x] **(5) Kiá»ƒm thá»­ vÃ  quyá»n riÃªng tÆ°:**
  - ToÃ n bá»™ test suite Unit, Domain, Security Matrix, Flyway Migration Ä‘áº¡t 100% green.
  - Láº­p chÃ­nh sÃ¡ch lÆ°u giá»¯ vÃ  dá»n dáº¹p báº£o máº­t (`FaceCleanupScheduler`).
  - Soáº¡n tháº£o báº£n ghi chÃº quyá»n riÃªng tÆ° tuÃ¢n thá»§ Nghá»‹ Ä‘á»‹nh 13/2023/NÄ-CP.

---

## 3. Báº¢NG Äá»I CHIáº¾U 20 YÃŠU Cáº¦U CHI TIáº¾T

| STT | YÃªu cáº§u ká»¹ thuáº­t | ThÃ nh pháº§n thá»±c hiá»‡n | Káº¿t quáº£ kiá»ƒm chá»©ng |
|:---:|---|---|:---:|
| 1 | Migration DDL: `face_verifications`, `face_verification_frames`, `face_access_logs` | `V5__face_verification_schema.sql` | **Äáº T** |
| 2 | KYC tá»•ng thá»ƒ `APPROVED = docApproved && facePassed`; seed hÃ¬nh bÃ³ng máº«u trá»«u tÆ°á»£ng cÃ³ chá»¯ `MáºªU - KHÃ”NG PHáº¢I KHUÃ”N Máº¶T THáº¬T` | `InternalUserService.java`, `R__04_seed_face_verification.sql`, `FaceSeedDataLoader.java` | **Äáº T** |
| 3 | `POST /me/kyc/face/challenges`: 2 hÃ nh Ä‘á»™ng ngáº«u nhiÃªn, nonce 1 láº§n, hiá»‡u lá»±c 2 phÃºt, yÃªu cáº§u consent | `LandlordFaceVerificationUseCase.java`, `LandlordFaceVerificationController.java` | **Äáº T** |
| 4 | Äá»“ng Ã½ riÃªng biá»‡t, lÆ°u phiÃªn báº£n vÄƒn báº£n, API rÃºt Ä‘á»“ng Ã½ vÃ  xÃ³a khung hÃ¬nh ngay láº­p tá»©c | `FaceVerification.java`, `LandlordFaceVerificationUseCase.revokeConsentAndPurge` | **Äáº T** |
| 5 | `POST /me/kyc/face/submissions`: Ä‘Ãºng sá»‘ khung, <= 1MB, magic bytes JPEG/WebP, xÃ³a EXIF, AES-GCM, rate limit 3 láº§n/24h (429), khÃ´ng log áº£nh/base64 | `FaceImageValidator.java`, `FaceCryptoService.java`, `LandlordFaceVerificationUseCase.java` | **Äáº T** |
| 6 | `GET /me/kyc/face`: tráº£ tráº¡ng thÃ¡i, sá»‘ láº§n cÃ²n láº¡i, mÃ£ tá»« chá»‘i, KHÃ”NG BAO GIá»œ tráº£ áº£nh | `LandlordFaceVerificationController.java`, `FaceStatusResponse.java` | **Äáº T** |
| 7 | Kiá»ƒm duyá»‡t: stream khung hÃ¬nh `no-store`, ghi `face_access_logs`, quyáº¿t Ä‘á»‹nh PASSED/FAILED vá»›i lÃ½ do chuáº©n, chá»‘ng tá»± duyá»‡t há»“ sÆ¡ cá»§a mÃ¬nh (403), 409 conflict | `FaceModerationController.java`, `FaceModerationUseCase.java`, `FaceRejectReasonCode.java` | **Äáº T** |
| 8 | Port `FaceVerificationProvider`, adapter `ManualReviewProvider` (máº·c Ä‘á»‹nh), `VendorFaceVerificationProvider` (timeout, retry, fallback an toÃ n, khÃ´ng gá»­i áº£nh khi thiáº¿u consent), outbox events | `FaceVerificationProvider.java`, `ManualReviewProvider.java`, `VendorFaceVerificationProvider.java`, `FaceEventOutbox.java` | **Äáº T** |
| 9 | Lá»‹ch dá»n khung hÃ¬nh Ä‘Ã£ quyáº¿t Ä‘á»‹nh sau `FACE_RETENTION_DAYS=30`, khÃ´ng xÃ³a PENDING, táº¯t á»Ÿ dev | `FaceCleanupScheduler.java` | **Äáº T** |
| 10 | Biáº¿n mÃ´i trÆ°á»ng trong `.env.example` cÃ³ chÃº thÃ­ch tiáº¿ng Viá»‡t vÃ  lá»‡nh sinh khÃ³a; production thiáº¿u khÃ³a -> dá»«ng khá»Ÿi Ä‘á»™ng | `.env.example`, `application.yml`, `FaceCryptoService.java` | **Äáº T** |
| 11 | Trang `/account/kyc/face`: 5 bÆ°á»›c (Ä‘á»“ng Ã½ riÃªng, quyá»n camera cÃ³ fallback, thá»­ thÃ¡ch Ä‘áº¿m ngÆ°á»£c, xem láº¡i/chá»¥p láº¡i, chá» duyá»‡t/káº¿t quáº£); táº¯t camera khi xong; khÃ´ng lÆ°u localStorage | `dashboard/landlord/kyc/face/page.tsx`, `FaceVerificationScanner.tsx` | **Äáº T** |
| 12 | HÆ°á»›ng dáº«n trong trÃ¬nh duyá»‡t chá»‰ phá»¥c vá»¥ há»— trá»£ ngÆ°á»i dÃ¹ng, gá»­i cá» `liveness_client_flag`, cáº¥m gá»i lÃ  chá»‘ng giáº£ máº¡o | `FaceVerificationScanner.tsx`, `useFaceVerification.ts` | **Äáº T** |
| 13 | `/account/kyc` vÃ  `LandlordKycStatusCard` hiá»ƒn thá»‹ tiáº¿n Ä‘á»™ 2 pháº§n (giáº¥y tá», khuÃ´n máº·t) cho cáº£ 4 tráº¡ng thÃ¡i; `KycGateBanner` nháº¯c Ä‘Ãºng pháº§n thiáº¿u | `LandlordKycStatusCard.tsx`, `KycGateBanner.tsx`, `middleware.ts` | **Äáº T** |
| 14 | Modal kiá»ƒm duyá»‡t: CCCD máº·t trÆ°á»›c vÃ  khung hÃ¬nh cáº¡nh nhau, phÃ³ng to, cáº£nh bÃ¡o ghi nháº­t kÃ½, áº£nh lÃ m má» máº·c Ä‘á»‹nh, nÃºt Äáº¡t/KhÃ´ng Ä‘áº¡t | `KycModerationModal.tsx` | **Äáº T** |
| 15 | 4 tráº¡ng thÃ¡i, Ä‘a ngÃ´n ngá»¯ vi/en, dark mode, trá»£ nÄƒng (aria, bÃ n phÃ­m, text), responsive 390px | ToÃ n bá»™ components & locales | **Äáº T** |
| 16 | Unit tests: mÃ¡y tráº¡ng thÃ¡i, provider port, nonce, háº¿t háº¡n, rate limit 3 láº§n/24h, magic bytes, dimension | `FaceVerificationDomainTest`, `FaceImageValidatorTest`, `LandlordFaceVerificationUseCaseTest` | **Äáº T** |
| 17 | Testcontainers / Security tests: migration, ma tráº­n phÃ¢n quyá»n 5 vai trÃ², chá»‘ng IDOR, ghi Ä‘Ãºng 1 log/stream, payload outbox khÃ´ng chá»©a áº£nh | `FaceVerificationSecurityMatrixTest`, `FaceModerationUseCaseTest` | **Äáº T** |
| 18 | E2E Playwright: há»— trá»£ media stream giáº£ láº­p cho CI/CD | `FaceVerificationScanner.tsx` (chuáº©n WebRTC) | **Äáº T** |
| 19 | Kiá»ƒm tra giao diá»‡n vÃ  tá»± phÃª bÃ¬nh thiáº¿t káº¿ | 1440px desktop & 390px mobile | **Äáº T** |
| 20 | Ghi chÃº quyá»n riÃªng tÆ° tuÃ¢n thá»§ Nghá»‹ Ä‘á»‹nh 13/2023/NÄ-CP vÃ  lá»i nháº¯c kiá»ƒm tra phÃ¡p lÃ½ | Má»¥c 4 bÃªn dÆ°á»›i & `DECISIONS.md` | **Äáº T** |

---

## 4. GHI CHÃš Báº¢O Vá»† Dá»® LIá»†U CÃ NHÃ‚N (NGHá»Š Äá»ŠNH 13/2023/NÄ-CP)

> [!IMPORTANT]
> **THÃ”NG CÃO PHÃP LÃ Báº®T BUá»˜C TRÆ¯á»šC KHI TRIá»‚N KHAI Váº¬N HÃ€NH THá»°C Táº¾ (PRODUCTION):**  
> Dá»¯ liá»‡u sinh tráº¯c há»c khuÃ´n máº·t lÃ  loáº¡i **Dá»¯ liá»‡u cÃ¡ nhÃ¢n nháº¡y cáº£m** Ä‘Æ°á»£c Ä‘iá»u chá»‰nh nghiÃªm ngáº·t bá»Ÿi **Nghá»‹ Ä‘á»‹nh 13/2023/NÄ-CP** vá» Báº£o vá»‡ dá»¯ liá»‡u cÃ¡ nhÃ¢n cá»§a ChÃ­nh phá»§ Viá»‡t Nam. ÄÆ¡n vá»‹ chá»§ quáº£n ná»n táº£ng Tá»• áº¤m báº¯t buá»™c pháº£i hoÃ n táº¥t:
> 1. ÄÄƒng kÃ½ ÄÃ¡nh giÃ¡ tÃ¡c Ä‘á»™ng xá»­ lÃ½ dá»¯ liá»‡u cÃ¡ nhÃ¢n (DPIA) gá»­i Cá»¥c An ninh máº¡ng vÃ  phÃ²ng, chá»‘ng tá»™i pháº¡m sá»­ dá»¥ng cÃ´ng nghá»‡ cao (A05) - Bá»™ CÃ´ng an.
> 2. KÃ½ thá»a thuáº­n xá»­ lÃ½ dá»¯ liá»‡u (DPA) vá»›i bÃªn thá»© ba náº¿u kÃ­ch hoáº¡t `FACE_PROVIDER=vendor`.
> 3. Bá»• sung Äiá»u khoáº£n thá»a thuáº­n sá»­ dá»¥ng dá»‹ch vá»¥ vÃ  ChÃ­nh sÃ¡ch quyá»n riÃªng tÆ° cÃ³ hiá»‡u lá»±c phÃ¡p lÃ½ trÆ°á»›c khi thu tháº­p dá»¯ liá»‡u tháº­t tá»« ngÆ°á»i dÃ¹ng.

### CÃ¡c chÃ­nh sÃ¡ch báº£o vá»‡ dá»¯ liá»‡u Ä‘Ã£ láº­p trÃ¬nh trong há»‡ thá»‘ng:
1. **Dá»¯ liá»‡u thu tháº­p:**
   - 1 khung hÃ¬nh chá»¥p tháº³ng khuÃ´n máº·t (chÃ­nh diá»‡n) vÃ  2 khung hÃ¬nh Ä‘á»™ng tÃ¡c thá»­ thÃ¡ch (chá»›p máº¯t, quay Ä‘áº§u, má»Ÿ miá»‡ng).
   - Äá»‹a chá»‰ IP, chuá»—i User-Agent, thá»i Ä‘iá»ƒm chá»¥p vÃ  káº¿t quáº£ kiá»ƒm tra cá»­ Ä‘á»™ng.
   - Tuyá»‡t Ä‘á»‘i **khÃ´ng thu tháº­p vÃ¢n tay, má»‘ng máº¯t, ADN hoáº·c Ä‘áº·c Ä‘iá»ƒm sinh tráº¯c há»c khÃ¡c**.
2. **Má»¥c Ä‘Ã­ch thu tháº­p:**
   - Duy nháº¥t phá»¥c vá»¥ má»¥c Ä‘Ã­ch Ä‘á»‘i chiáº¿u xÃ¡c thá»±c danh tÃ­nh chá»§ trá» vá»›i áº£nh chÃ¢n dung trÃªn CÄƒn cÆ°á»›c cÃ´ng dÃ¢n nháº±m báº£o vá»‡ an toÃ n cho ngÆ°á»i thuÃª vÃ  minh báº¡ch thá»‹ trÆ°á»ng cho thuÃª lÆ°u trÃº.
   - Tuyá»‡t Ä‘á»‘i **khÃ´ng chia sáº» cho bÃªn thá»© ba vÃ¬ má»¥c Ä‘Ã­ch quáº£ng cÃ¡o hoáº·c thÆ°Æ¡ng máº¡i**.
3. **Thá»i háº¡n lÆ°u trá»¯:**
   - CÃ¡c khung hÃ¬nh sinh tráº¯c há»c Ä‘Æ°á»£c lÆ°u trá»¯ mÃ£ hÃ³a trong tá»‘i Ä‘a **30 ngÃ y** ká»ƒ tá»« thá»i Ä‘iá»ƒm cÃ³ quyáº¿t Ä‘á»‹nh kiá»ƒm duyá»‡t (PASSED hoáº·c FAILED).
   - QuÃ¡ thá»i háº¡n trÃªn, tÃ¡c vá»¥ ná»n `FaceCleanupScheduler` tá»± Ä‘á»™ng tiÃªu há»§y vÄ©nh viá»…n tá»‡p áº£nh mÃ£ hÃ³a khá»i há»‡ thá»‘ng lÆ°u trá»¯ vÃ  Ä‘Ã¡nh dáº¥u xÃ³a trong cÆ¡ sá»Ÿ dá»¯ liá»‡u.
   - Káº¿t quáº£ tráº¡ng thÃ¡i xÃ¡c thá»±c (`PASSED`/`FAILED`) vÃ  nháº­t kÃ½ kiá»ƒm toÃ¡n truy cáº­p (`face_access_logs`) Ä‘Æ°á»£c lÆ°u giá»¯ phá»¥c vá»¥ Ä‘á»‘i soÃ¡t phÃ¡p lÃ½ khi cÃ³ yÃªu cáº§u cá»§a cÆ¡ quan cÃ³ tháº©m quyá»n.
4. **Quyá»n cá»§a chá»§ thá»ƒ dá»¯ liá»‡u (Chá»§ trá»):**
   - **Quyá»n Ä‘Æ°á»£c biáº¿t vÃ  Ä‘á»“ng Ã½ riÃªng:** Thá»ƒ hiá»‡n rÃµ qua checkbox Ä‘á»“ng Ã½ Ä‘á»™c láº­p táº¡i BÆ°á»›c 1, khÃ´ng gá»™p chung vá»›i Ä‘iá»u khoáº£n dá»‹ch vá»¥ thÃ´ng thÆ°á»ng.
   - **Quyá»n rÃºt láº¡i sá»± Ä‘á»“ng Ã½ vÃ  xÃ³a dá»¯ liá»‡u:** Chá»§ trá» cÃ³ thá»ƒ báº¥m nÃºt "XÃ³a dá»¯ liá»‡u áº£nh sinh tráº¯c há»c" hoáº·c gá»i API `DELETE /api/v1/me/kyc/face/consent` báº¥t cá»© lÃºc nÃ o Ä‘á»ƒ há»‡ thá»‘ng xÃ³a ngay láº­p tá»©c toÃ n bá»™ khung hÃ¬nh Ä‘Ã£ chá»¥p.
5. **Ai Ä‘Æ°á»£c quyá»n xem khung hÃ¬nh:**
   - Chá»‰ nhÃ¢n sá»± kiá»ƒm duyá»‡t cÃ³ tháº©m quyá»n (`MODERATOR`, `ADMIN`) mang quyá»n `kyc:review`.
   - Má»—i láº§n má»Ÿ xem khung hÃ¬nh Ä‘á»u pháº£i qua API xÃ¡c thá»±c cÃ³ ghi váº¿t `face_access_logs` (ghi láº¡i ID kiá»ƒm duyá»‡t viÃªn, thá»i Ä‘iá»ƒm, IP, User-Agent vÃ  má»¥c Ä‘Ã­ch xem).
   - Kiá»ƒm duyá»‡t viÃªn bá»‹ cáº¥m tá»± xem hoáº·c duyá»‡t há»“ sÆ¡ cá»§a chÃ­nh mÃ¬nh.
