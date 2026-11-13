# DAN SLIDE SPRINT 3 REVIEW — TO AM / COZYNEST

> 10 slides, uoc luong 20-25 phut tong cong
> Nguoi noi goi y: Cuong (Tech Lead) chinh, cac dev ho tro theo phan viec

---

## Slide 1: Muc tieu Sprint 3 va thay doi pham vi (2 phut)
**Tieu de:** Sprint 3 — Giao dich, Hop dong, Tien
**Y chinh:**
- Sprint Goal: khep kin chu trinh dat xem > dat thue > ky hop dong > coc > hoa don > thanh toan
- Doi chieu: 12 the goc (34 SP) > 28 the thuc te (128 SP) — tang 3.8x
- Ly do: doi stack, yeu cau tien that va du lieu ca nhan that, phap ly VN
- Doi stack: React+Vite+Node sang Next.js 16.2 + Spring Boot 4.1 microservices
**Nguoi noi:** Cuong
**Thoi luong:** 2 phut

---

## Slide 2: Kien truc he thong (2 phut)
**Tieu de:** Kien truc Microservices Sprint 3
**Y chinh:**
- 7 service + API Gateway + RabbitMQ + Redis + MySQL 8.4 LTS (6 schema co lap)
- BFF HttpOnly Cookie (token KHONG xuat hien o JS trinh duyet)
- Saga orchestrator + Transactional Outbox Pattern
- So do Mermaid (xem SPRINT3-REVIEW.md muc B)
**Nguoi noi:** Cuong
**Thoi luong:** 2 phut

---

## Slide 3: Demo luong chinh — Dat lich + Dat thue (3 phut)
**Tieu de:** Demo: Khach tim phong > Dat lich > Dat thue
**Y chinh:**
- Tim phong, bo loc, trang chi tiet
- Dang nhap nguoi thue > dat lich xem phong
- Chu tro duyet lich hen (PENDING > CONFIRMED)
- Nguoi thue gui yeu cau thue
**Anh chup:** screenshots tu buoc 1-4 cua demo
**Nguoi noi:** Dev 3 (FE) hoac Cuong
**Thoi luong:** 3 phut

---

## Slide 4: Demo luong chinh — Hop dong + Ky OTP (3 phut)
**Tieu de:** Demo: Hop dong dien tu + Ky OTP + Hash SHA-256
**Y chinh:**
- Chu tro tao hop dong tu yeu cau thue
- Ky OTP 2 ben (6 so, gui qua email Mailpit)
- Hien thi hash SHA-256 cua ban chup bat bien
- Tai PDF, ban in ro rang
**Anh chup:** screenshots buoc 5-7
**Nguoi noi:** Dev 2 (BE Contract) hoac Dev 3 (FE)
**Thoi luong:** 3 phut

---

## Slide 5: Demo luong chinh — Thanh toan + Hoa don (3 phut)
**Tieu de:** Demo: Coc (SANDBOX) + Chi so dien nuoc + Hoa don
**Y chinh:**
- Thanh toan coc qua sandbox (VietQR + webhook simulate)
- Chu tro nhap chi so dien nuoc > sinh hoa don
- Nguoi thue xem va thanh toan hoa don
- Nhan ro: SANDBOX_ONLY, chua tien that
**Anh chup:** screenshots buoc 8-10
**Nguoi noi:** Dev 4 (Finance UI) hoac Cuong
**Thoi luong:** 3 phut

---

## Slide 6: Dashboard + So cai + Bao mat (2 phut)
**Tieu de:** Dashboard, So cai ke toan kep, Bao mat phien
**Y chinh:**
- Dashboard chu tro (doanh thu, cong no)
- Dashboard nguoi thue (hop dong, hoa don)
- Admin: so cai but toan No = Co, doi soat
- Chung minh: document.cookie va localStorage KHONG co token
**Anh chup:** screenshots buoc 11-14
**Nguoi noi:** Dev 4 + Dev 5
**Thoi luong:** 2 phut

---

## Slide 7: Bao mat va Kiem thu (2 phut)
**Tieu de:** Bao mat va Kiem thu Sprint 3
**Y chinh:**
- 396 test BE (0 fail), 77 test FE vitest, CI 6/6 SUCCESS
- JWT RS256 + JWKS + kid xoay khoa, BCrypt 12, chong timing attack
- KYC: AES-256-GCM, HMAC-SHA256, kho rieng tu, audit log bat bien
- Ma hoa OTP (chi luu hash), rate limit Redis, FAIL-CLOSED
**Nguoi noi:** Dev 5 (QA/Security)
**Thoi luong:** 2 phut

---

## Slide 8: So lieu Sprint 3 (1 phut)
**Tieu de:** So lieu cong viec Sprint 3
**Y chinh:**
- 38 commits (22 BE + 16 FE), +28,213 dong ma moi
- 48 controllers, 103 test classes, 40 Flyway migrations
- 98 trang FE, 1936 khoa i18n VI/EN (100% parity)
- 464 files BE thay doi, 204 files FE thay doi
**Nguoi noi:** Cuong
**Thoi luong:** 1 phut

---

## Slide 9: Trung thuc — Phan chua xong (2 phut)
**Tieu de:** Trung thuc: Nhung gi CHUA hoan thanh
**Y chinh (dung dung so lieu):**
- 22 the PARTIAL (104 diem): co ma, test pass, chua kiem chung het checklist
- 5 the SANDBOX (21 diem): chay duoc nhung mo phong, chua tien that
- 1 the HUMAN (3 diem): LEGAL-02 cho luat su
- 0 the DEMO-OK (chua co demo tren trinh duyet that va chup anh)
- Playwright E2E: 71/246 FAIL (auth helper cu, can cap nhat Sprint 4)
- Chi 1 tac gia git (catim07) — cac dev khac can nhan code Sprint 4
**Nguoi noi:** Cuong
**Thoi luong:** 2 phut

---

## Slide 10: Sprint 4 va viec can lam tiep (1 phut)
**Tieu de:** Tiep theo: Sprint 4 — Tang truong, Van hanh, Ra mat
**Y chinh:**
- Sprint 4: 31 the, 130 diem
- Carry-over tu Sprint 3: LEGAL-02 (gap luat su), sua Playwright, nang cap RabbitMQ
- Thu tu Sprint 4: ADMIN-BE/FE > CHAT > LEGAL-01 > QA-K6 > SRE-01 > LAUNCH-01
- Muc tieu: beta kin voi chu tro that, Go/No-Go tai chinh
**Nguoi noi:** Cuong
**Thoi luong:** 1 phut

---

## Tong thoi luong uoc tinh: 21 phut + 4 phut Q&A = 25 phut

