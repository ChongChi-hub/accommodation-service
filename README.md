# PhongTro Service - Spring Boot & Gradle

Hệ thống Backend Service cho dự án **PhongTro Online** (Hệ thống Thuê Phòng Trọ) được xây dựng theo chuẩn **Spring Boot 3.3.5**, **Java 21 LTS** và **Gradle 9.8.2**.

---

## 🏛️ Cấu trúc dự án (`com.service`)

```text
service/src/main/java/com/service/
├── ServiceApplication.java  # Entry point chính
│
├── common/                  # DTO chung (ApiResponse, PaginationResponse) & Exception Handler
│   ├── dto/
│   └── exception/
│
├── security/                # Spring Security, JWT (JJWT), CORS & Filters
│   ├── JwtUtils.java
│   ├── JwtAuthFilter.java
│   └── SecurityConfig.java
│
└── auth/                    # Module Xác thực, Người dùng & KYC
    ├── controller/          # AuthController (/api/v1/auth/**), UserController (/api/v1/users/**)
    ├── dto/                 # RegisterRequest, LoginRequest, UserDto, KycRequest...
    ├── entity/              # UserEntity, UserVerificationEntity
    ├── repository/          # UserRepository, UserVerificationRepository
    └── service/             # AuthService
```

---

## 🚀 Hướng dẫn Build & Khởi Chạy

### 1. Thiết lập biến môi trường
```bash
export JAVA_HOME="/Users/trongtri/Library/Java/JavaVirtualMachines/temurin-21.0.11/Contents/Home"
```

### 2. Biên dịch dự án
```bash
cd service
./gradlew build -x test
```

### 3. Khởi chạy Server
```bash
./gradlew bootRun
```
> Server mặc định khởi chạy tại cổng **8080** (hoặc cấu hình qua biến `PORT`).
> API Base: **`http://localhost:8080/api/v1`**

---

## 🧪 Kiểm thử API đã hoàn thiện

* **Đăng nhập**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@phongtro.com","password":"admin123"}'
  ```

* **Lấy thông tin cá nhân (Profile)**:
  ```bash
  curl http://localhost:8080/api/v1/auth/profile \
    -H "Authorization: Bearer <TOKEN_NHẬN_ĐƯỢC>"
  ```

