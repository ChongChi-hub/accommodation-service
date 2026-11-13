# KICH BAN DEMO SPRINT 3 — TO AM / COZYNEST

> Ngay: 13/11/2026 | Nhanh: `feat/saga-01-escrow-01`
> Thoi gian uoc luong: 20-25 phut (co du phong)

---

## TAI KHOAN SEED (chi moi truong dev/demo, KHONG dung o production)

| Vai trò | Email | Ghi chú |
|---|---|---|
| Người thuê (TENANT) | `tenant@cozynest.vn` | Đã xác minh email |
| Chủ trọ (LANDLORD) | `landlord@cozynest.vn` | KYC APPROVED, sở hữu phòng đăng cho thuê |
| Admin | `admin@cozynest.vn` | ROLE_ADMIN |
| Moderator | `moderator@cozynest.vn` | ROLE_MODERATOR |

> Mật khẩu seed demo: `password123` (cho toàn bộ tài khoản seed)

---

## CHUYEN BI MOI TRUONG TRUOC DEMO

```powershell
# 1. Khoi dong Backend (Docker Compose full)
Set-Location d:\RIKKEI\THUC_TAP\PROJECT\accommodation-service
docker compose --profile full up -d
# Kiem tra: docker compose ps (12 container, tat ca healthy/up)

# 2. Hoac chay FE rieng
Set-Location d:\RIKKEI\THUC_TAP\PROJECT\accommodation-web
npm run dev
# Truy cap: http://localhost:3000/vi
```

---

## BUOC DEMO

### Buoc 1: Khach tim phong va mo chi tiet
- **URL:** `http://localhost:3000/vi/rooms` (hoac `/phong-tro`)
- **Hanh dong:** Chon bo loc (tinh, gia, loai phong) > bam vao the phong > xem trang chi tiet phong
- **Ket qua mong doi:** Trang chi tiet hien thi day du thong tin: hinh anh, gia, dia chi 2 cap, tien ich, noi quy, ho so chu tro
- **Trang thai:** Trang nay lam viec tu Sprint 1-2, chi can xac nhan khong loi

### Buoc 2: Nguoi thue dang nhap va dat lich xem phong
- **URL:** Bam nut "Dat lich xem" tren trang chi tiet phong > redirect dang nhap
- **Tai khoan:** tenant01@cozynest.vn
- **Hanh dong:** Chon ngay gio, ghi chu, gui yeu cau
- **Ket qua mong doi:** Hien thong bao thanh cong, lich hen trang thai PENDING
- **API lien quan:** `POST /api/v1/booking/appointments` (booking-service)
- **Luu y:** Nut "Dat lich" co co chong bam dup (disabled sau click dau)

### Buoc 3: Chu tro duyet lich hen
- **Dang nhap:** landlord01@cozynest.vn
- **URL:** `/dashboard/landlord/appointments`
- **Hanh dong:** Tim lich hen tu tenant01, bam "Xac nhan"
- **Ket qua mong doi:** Trang thai chuyen PENDING > CONFIRMED, outbox event duoc tao
- **Muc Saga:** He thong kiem tra trung gio, neu co hien ERR_APPT_TIME_CONFLICT (409)

### Buoc 4: Nguoi thue gui yeu cau thue
- **Dang nhap:** tenant01@cozynest.vn
- **URL:** `/dashboard/tenant/rental-requests` hoac `/rooms/[slug]` > nut "Gui yeu cau thue"
- **Hanh dong:** Chon phong, dien thong tin, gui yeu cau
- **API:** `POST /api/v1/booking/rental-requests` (booking-service)
- **Ket qua mong doi:** Trang thai PENDING, hien thi so tien coc va chinh sach truoc khi xac nhan

### Buoc 5: Chu tro duyet yeu cau thue > Tao hop dong
- **Dang nhap:** landlord01@cozynest.vn
- **URL:** `/dashboard/landlord/rental-requests` > Duyet > `/dashboard/landlord/contracts`
- **Hanh dong:** Duyet yeu cau, he thong tao hop dong dien tu
- **Ket qua mong doi:** Hop dong trang thai DRAFT, hien day du dieu khoan, thong tin phong, so tien

### Buoc 6: Ky hop dong OTP — Chu tro ky truoc
- **URL:** `/dashboard/landlord/contracts/[id]`
- **Hanh dong:** Bam "Ky hop dong" > nhap ma OTP (lay tu Mailpit hoac console dev)
- **API:** `POST /api/v1/booking/contracts/{id}/sign-landlord`
- **Ket qua mong doi:** Hien hash SHA-256 cua ban chup hop dong, thoi diem ky (UTC)
- **Trang thai:** `SANDBOX` — OTP gui qua Mailpit (chua co SMS provider that)

### Buoc 7: Ky hop dong OTP — Nguoi thue ky
- **Dang nhap:** tenant01@cozynest.vn
- **URL:** `/dashboard/tenant/contracts/[id]`
- **Hanh dong:** Xem hop dong > Ky > Nhap OTP
- **API:** `POST /api/v1/booking/contracts/{id}/sign-tenant`
- **Ket qua mong doi:** Hop dong SIGNED, hash SHA-256, ca 2 ben ky xong, PDF co the tai
- **Kiem tra them:** Bam nut "Tai PDF" > kiem tra ban in (@media print)

### Buoc 8: Thanh toan coc (SANDBOX)
- **URL:** `/dashboard/tenant/deposits` hoac modal thanh toan
- **Hanh dong:** He thong hien ma VietQR (sandbox), nguoi dung "thanh toan" qua sandbox simulate
- **API:** `POST /api/v1/billing/payments/sandbox/simulate-webhook`
- **Ket qua mong doi:** Trang thai thanh toan COMPLETED, but toan so cai duoc tao
- **NHAN:** `SANDBOX` — dung co `fintech.mode=SANDBOX_ONLY`, KHONG xu ly tien that
- **Du phong:** Neu gateway loi, demo bang anh chup quy trinh thanh toan

### Buoc 9: Chu tro nhap chi so dien nuoc
- **Dang nhap:** landlord01@cozynest.vn
- **URL:** `/dashboard/landlord/invoices` > tab "Chi so"
- **Hanh dong:** Chon phong, nhap chi so ky moi (dien cu/moi, nuoc cu/moi)
- **API:** `POST /api/v1/billing/utility-readings`
- **Ket qua mong doi:** Chi so duoc luu, canh bao neu tang dot bien

### Buoc 10: Sinh hoa don va nguoi thue thanh toan
- **Hanh dong:** Chu tro bam "Sinh hoa don" > hoa don trang thai ISSUED
- **Dang nhap tenant:** `/dashboard/tenant/invoices/[id]` > xem chi tiet > thanh toan (SANDBOX)
- **Ket qua mong doi:** Hoa don PAID, but toan so cai ghi nhan

### Buoc 11: Dashboard chu tro (doanh thu, cong no)
- **URL:** `/dashboard/landlord` (trang chinh) va `/dashboard/landlord/analytics`
- **Ket qua mong doi:** Hien thi so lieu doanh thu, cong no, phong dang thue, hop dong

### Buoc 12: Dashboard nguoi thue
- **URL:** `/dashboard/tenant`
- **Ket qua mong doi:** Hien thi hop dong hien tai, hoa don can tra, lich su thanh toan

### Buoc 13: Admin — So cai, doi soat, hoan tien
- **Dang nhap:** admin@cozynest.vn
- **URL:** `/dashboard/admin/transactions` > tab so cai
- **Hanh dong:** Xem so cai, chay doi soat, xem danh sach hoan tien
- **Ket qua mong doi:** Bang so cai hien but toan, but toan No = Co

### Buoc 14: Bao mat phien (BFF HttpOnly Cookie)
- **Hanh dong (tren trinh duyet dev tools):**
  1. Mo Console > go `document.cookie` > ket qua: KHONG thay `cozynest_token` hay `cozynest_refresh_token`
  2. Mo Console > go `localStorage` > ket qua: KHONG thay token nao
  3. Mo Network > goi API > kiem tra Response body: KHONG co truong `accessToken` / `refreshToken`
- **Ket qua mong doi:** Token chi ton tai trong HttpOnly cookie, JS khong doc duoc
- **NHAN:** Can kiem tra bang tay hoac Playwright e2e (hien co test nhung chua chay duoc tat ca)

---

## DU PHONG KHI BUOC LOI

| Buoc | Loi co the gap | Phuong an du phong |
|---|---|---|
| 2-3 | API 401/403 | Kiem tra token het han, refresh, hoac login lai |
| 6-7 | OTP khong den | Kiem tra Mailpit `http://localhost:8025`, hoac dung log console |
| 8, 10 | Sandbox webhook loi | Demo bang API call truc tiep qua Postman/curl |
| 13 | So cai rong | Chay buoc 8, 10 truoc de tao du lieu |

---

## PHUONG AN DAT LAI DU LIEU DEMO

```powershell
# Xoa toan bo volume va khoi dong lai
Set-Location d:\RIKKEI\THUC_TAP\PROJECT\accommodation-service
docker compose --profile full down -v --remove-orphans
docker compose --profile full up -d --build
# Doi 2-3 phut cho Flyway chay xong seed
```

---

## GHI CHU QUAN TRONG

- Toan bo thanh toan chay o che do `SANDBOX_ONLY` (`fintech.mode=SANDBOX_ONLY`)
- OTP gui qua Mailpit (email catcher dev), KHONG gui SMS that
- Module Escrow giu coc bi khoa (`money.custody.enabled=false`) theo ADR-008 Model A
- Can luat su duyet truoc khi bat tien that (LEGAL-02 = HUMAN)

