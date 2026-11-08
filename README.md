# To Am - Backend Microservices Architecture (`accommodation-service`)

He thong Backend Microservices cho nen tang thue tro **To Am**, thiet ke theo nguyen ly **Domain-Driven Design (DDD)** tren nen **Java 25 LTS & Spring Boot 4.1.2**.

---

## Danh Sach Microservices & Cong Port

| Service | Port | Database Schema | Chuc Nang Chinh |
|---|---|---|---|
| **`api-gateway`** | `8080` | - | Dinh tuyen trung tam, CORS, Rate Limit, gop OpenAPI `/docs` |
| **`identity-service`** | `8081` | `cozynest_identity` | Tai khoan, vai tro (RBAC), Dang ky, Dang nhap, JWT, Refresh Token, KYC |
| **`listing-service`** | `8082` | `cozynest_listing` | Phong tro, loai phong, dia gioi DVHCVN, tien nghi, anh, danh gia |
| **`booking-service`** | `8083` | `cozynest_booking` | Lich hen xem phong, yeu cau thue, hop dong dien tu |
| **`billing-service`** | `8084` | `cozynest_billing` | Chi so dien nuoc (cong to), hoa don hang thang, thanh toan |
| **`communication-service`** | `8085` | `cozynest_communication` | Tin nhan hoi thoai thoi gian thuc, thong bao in-app |
| **`admin-service`** | `8086` | `cozynest_admin` | Thong ke bang dieu khien, kiem duyet tin/KYC, bao cao vi pham |

---

## Tai Khoan Demo (Moi truong Dev)

| Vai tro | Email | Mat khau |
|---|---|---|
| **Nguoi thue (Tenant)** | `tenant@cozynest.vn` | `password123` |
| **Chu tro (Landlord)** | `landlord@cozynest.vn` | `password123` |
| **Kiem duyet vien (Moderator)** | `moderator@cozynest.vn` | `password123` |
| **Quan tri vien (Admin)** | `admin@cozynest.vn` | `password123` |

---

## Huong Dan Cai Dat & Khoi Chay

### 1. Yeu cau he thong
* **Java:** Java 25 LTS (Gradle Toolchain se tu dong nhan dien hoac tai Temurin 25).
* **MySQL:** 9.7 LTS hoac 8.0+ (Port `3306`).
* **Redis:** 7.x (Port `6379`).

### 2. Bien dich & Kiem thu kien truc DDD
```bash
# Bien dich toan bo cac service
./gradlew clean build -x test

# Chay kiem thu tu dong (Unit Test & ArchUnit DDD)
./gradlew test
```

### 3. Chay tung service doc lap
```bash
# Chay API Gateway
./gradlew :api-gateway:bootRun

# Chay Identity Service
./gradlew :identity-service:bootRun

# Chay Listing Service
./gradlew :listing-service:bootRun
```

### 4. Khoi chay bang Docker Compose (Huong dan trong <= 10 phut)
Cac dich vu ho tro 3 profile linh hoat:

```bash
# 1. Chi chay ha tang (MySQL, Redis, Mailpit)
docker compose --profile infra up -d

# 2. Chay cac dich vu cot loi (Infra + Gateway, Identity, Listing, Booking)
docker compose --profile core up -d

# 3. Chay toan bo he thong (Infra + 6 Microservices + Gateway + Next.js Web)
docker compose --profile full up -d

# 4. Kiem tra trang thai suc khoe toan bo microservices
node scripts/wait-for-health.js
```

---

## Tai Lieu Ky Thuat
* **Bao cao chuyen doi Node.js sang Spring Boot:** Xem tai [`docs/SERVICE-01_MIGRATION.md`](./docs/SERVICE-01_MIGRATION.md).
* **Quy chuan phat trien:** Xem file [`AGENT.md`](./AGENT.md).
* **Tai lieu dac ta (SRS):** Xem tai [`docs/SRS.md`](./docs/SRS.md).
* **Quyet dinh kien truc (ADR):** Xem tai [`docs/DECISIONS.md`](./docs/DECISIONS.md).
* **Schema cu tham chieu:** Xem tai [`docs/legacy/schema.sql`](./docs/legacy/schema.sql).
