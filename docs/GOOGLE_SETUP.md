# Huong Dan Cau Hinh Google Identity Services (GIS) & OAuth 2.0

Tai lieu huong dan quan tri vien va lap trinh vien thiet lap tich hop Google Identity Services tren he thong To Am (Cozynest).

---

## 1. Tong Quan Kien Truc GIS & Bao Mat (ADR-011)

- **Mo hinh xac thuc**: Google Identity Services (GIS) One-Tap / Button dang ky/dang nhap bang ID Token (JWT RS256).
- **Khong dung Client Secret**: Toan bo luong xac thuc ID Token thuc hien phia may chu bang JWKS (`https://www.googleapis.com/oauth2/v3/certs`) qua thu vien Nimbus/Spring Security OAuth2. Khong co bat ky noi nao luu hoac yeu cau `Client Secret`.
- **Chong Replay Attack**: Server cap `nonce` (TTL 5 phut luu Redis), client truyen vao GIS callback, backend xac minh va tieu thu `nonce` nguyen tu dung 1 lan duy nhat.
- **BFF Cookie HttpOnly**: Toan bo token he thong sau dang nhap duoc BFF set qua cookie HttpOnly `cozynest_token`, khong luu o localStorage.

---

## 2. Thiet Lap Google Cloud Console

1. Truy cap [Google Cloud Console](https://console.cloud.google.com/) -> Tao Project moi hoac chon Project san co.
2. Vao **APIs & Services** -> **OAuth consent screen**:
   - Chon loai User Type: **External**.
   - App name: `To Am - Cozynest`.
   - User support email & Developer contact: email quan tri vien.
   - Scopes: Chi chon 3 quyen co ban: `openid`, `.../auth/userinfo.email`, `.../auth/userinfo.profile`.
   - Test users (neu consent screen o trang thai **Testing**): Them cac email can test (vi du: `cuong.le@example.com`).
   - Khi release chinh thuc: Chuyen Publishing status sang **In production** va cap nhat URL Chinh sach quyen rieng tu.
3. Vao **Credentials** -> **Create Credentials** -> **OAuth client ID**:
   - Application type: **Web application**.
   - Name: `To Am Web Client`.
   - **Authorized JavaScript origins**:
     - `http://localhost:3000`
     - `http://127.0.0.1:3000`
     - `http://localhost` (du phong neu gap loi `origin_mismatch`)
     - Domain san pham: `https://toam.vn` (khi trien khai production).
   - **Authorized redirect URIs**: De trong! Luong GIS Button su dung JavaScript Callback tren cung origin, khong su dung Redirect URI truyen thong.
4. Copy **Client ID** (dang `865044247606-xxxxxxxxxxxxxxxx.apps.googleusercontent.com`).

---

## 3. Cau Hinh Bien Moi Truong

### Backend (`accommodation-service/.env`)
```bash
GOOGLE_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID=865044247606-e93b5l98l4mp66eqkfdlatvks5rsn8r4.apps.googleusercontent.com
GOOGLE_ALLOW_STAFF=false
```

### Frontend (`accommodation-web/.env.local`)
```bash
NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED=true
NEXT_PUBLIC_GOOGLE_CLIENT_ID=865044247606-e93b5l98l4mp66eqkfdlatvks5rsn8r4.apps.googleusercontent.com
```

### Kiem tra dong bo bang Script Chan Doan
Chay script:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/google-check.ps1
```
Script se kiem tra su ton tai cua bien, so sanh ma SHA-256 fingerprint (8 ky tu) giua FE va BE, va kiem tra ket noi toi JWKS Google.

---

## 4. Danh Sach Kiem Tra Bang Tay (HUMAN)

Do Google ap dung co che chan tu dong hoa (anti-bot / headless browser), cac buoc sau can con nguoi thao tac kiem chung:

| STT | Noi dung kiem tra | Ket qua mong doi |
|---|---|---|
| 1 | Bam "Tiep tuc voi Google" tai `/login` bang tai khoan Google moi | Tao user TENANT, chuyen huong vao `/dashboard/tenant`, hien toast chao mung |
| 2 | Chon vai tro Chu tro tai `/register`, bam "Dang ky voi Google" | Tao user TENANT, tao ho So Landlord (AWAITING_KYC), chuyen den trang KYC |
| 3 | Dang nhap lai bang Google voi tai khoan da dang ky truoc do | Vao thang dashboard tuong ung voi vai tro |
| 4 | Lien ket Google tai trang Ho so -> Bao mat (`/profile`) | Danh sach tai khoan lien ket hien Google badge xanh |
| 5 | Huy lien ket Google khi tai khoan da co mat khau | Modal xac nhan xuat hien, huy lien ket thanh cong |
| 6 | Huy lien ket Google khi tai khoan CHI co Google (chua co mat khau) | Nut huy bi chan, hien nut "Dat mat khau" |
| 7 | Thu dang nhap Google bang email trung voi email ADMIN / MODERATOR | Bi tu choi HTTP 403 voi thong bao chan tai khoan nhan su |
