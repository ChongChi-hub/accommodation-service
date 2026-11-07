# Sơ Đồ Quan Hệ Tổng Quan Giữa Các Microservices (Cross-Service ERD)
**Dự án:** Nền tảng Thuê trọ "Tổ Ấm"  
**Quy tắc:** Bounded Context độc lập, không dùng Foreign Key vật lý liên schema; mọi quan hệ liên service hoàn toàn thông qua UUID tham chiếu có đánh Index.

```mermaid
erDiagram
    IDENTITY_USERS ||--o{ LISTING_ROOMS : "landlord_id (UUID)"
    IDENTITY_USERS ||--o{ BOOKING_APPOINTMENTS : "tenant_id / landlord_id"
    IDENTITY_USERS ||--o{ BOOKING_RENTAL_REQUESTS : "tenant_id / landlord_id"
    IDENTITY_USERS ||--o{ BOOKING_CONTRACTS : "tenant_id / landlord_id"
    IDENTITY_USERS ||--o{ BILLING_INVOICES : "tenant_id / landlord_id"
    IDENTITY_USERS ||--o{ BILLING_PAYMENTS : "payer_id"
    IDENTITY_USERS ||--o{ COMMUNICATION_NOTIFICATIONS : "user_id"
    IDENTITY_USERS ||--o{ COMMUNICATION_MESSAGES : "sender_id"
    IDENTITY_USERS ||--o{ ADMIN_REPORTS : "reporter_id"
    IDENTITY_USERS ||--o{ ADMIN_DISPUTES : "complainant_id / defendant_id"

    LISTING_ROOMS ||--o{ BOOKING_APPOINTMENTS : "room_id (UUID)"
    LISTING_ROOMS ||--o{ BOOKING_RENTAL_REQUESTS : "room_id (UUID)"
    LISTING_ROOMS ||--o{ BOOKING_CONTRACTS : "room_id (UUID)"
    LISTING_ROOMS ||--o{ BOOKING_MAINTENANCE : "room_id (UUID)"
    LISTING_ROOMS ||--o{ COMMUNICATION_CONVERSATIONS : "room_id (UUID)"

    BOOKING_CONTRACTS ||--|| BOOKING_ROOM_SNAPSHOTS : "contract_id (1:1)"
    BOOKING_CONTRACTS ||--o{ BOOKING_MAINTENANCE : "contract_id"
    BOOKING_CONTRACTS ||--o{ BILLING_INVOICES : "contract_id"
    BOOKING_CONTRACTS ||--|| BILLING_DEPOSITS : "contract_id (1:1)"
    BOOKING_CONTRACTS ||--o{ ADMIN_DISPUTES : "contract_id"
```
