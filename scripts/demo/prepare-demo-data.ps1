# Script chuẩn bị dữ liệu Demo qua REST API (không sửa trực tiếp DB)
# Sử dụng: .\prepare-demo-data.ps1 [-GatewayUrl "http://localhost:8080/api/v1"]

param(
    [string]$GatewayUrl = "http://localhost:8080/api/v1",
    [string]$LandlordEmail = "landlord@cozynest.vn",
    [string]$TenantEmail = "tenant@cozynest.vn",
    [string]$Password = "password123"
)

Write-Host "=== CHUẨN BỊ DỮ LIỆU DEMO SPRINT 3 TỔ ẤM ===" -ForegroundColor Cyan
Write-Host "Gateway API: $GatewayUrl"

# 1. Đăng nhập Tenant để lấy access token
Write-Host "`n1. Đăng nhập tài khoản Người thuê ($TenantEmail)..."
$tenantLoginBody = @{
    email = $TenantEmail
    password = $Password
} | ConvertTo-Json

try {
    $tenantResp = Invoke-RestMethod -Uri "$GatewayUrl/auth/login" -Method Post -Body $tenantLoginBody -ContentType "application/json"
    $tenantToken = $tenantResp.data.accessToken
    Write-Host "-> Đăng nhập Tenant thành công." -ForegroundColor Green
} catch {
    Write-Warning "Không kết nối được Gateway hoặc sai mật khẩu: $($_.Exception.Message)"
    Write-Host "Lưu ý: Nếu chạy qua BFF Next.js, hệ thống dùng HttpOnly cookie."
}

# 2. Đăng nhập Landlord để lấy access token
Write-Host "`n2. Đăng nhập tài khoản Chủ trọ ($LandlordEmail)..."
$landlordLoginBody = @{
    email = $LandlordEmail
    password = $Password
} | ConvertTo-Json

try {
    $landlordResp = Invoke-RestMethod -Uri "$GatewayUrl/auth/login" -Method Post -Body $landlordLoginBody -ContentType "application/json"
    $landlordToken = $landlordResp.data.accessToken
    Write-Host "-> Đăng nhập Landlord thành công." -ForegroundColor Green
} catch {
    Write-Warning "Không kết nối được Gateway hoặc sai mật khẩu: $($_.Exception.Message)"
}

# 3. Kiểm tra danh sách phòng khả dụng
Write-Host "`n3. Kiểm tra phòng cho thuê khả dụng..."
try {
    $roomsResp = Invoke-RestMethod -Uri "$GatewayUrl/rooms?limit=5" -Method Get
    $rooms = $roomsResp.data.items
    Write-Host "-> Tìm thấy $($rooms.Count) phòng mẫu từ seed data." -ForegroundColor Green
    if ($rooms.Count -gt 0) {
        $firstRoom = $rooms[0]
        Write-Host "   Phòng mẫu: $($firstRoom.title) - ID: $($firstRoom.id) / UUID: $($firstRoom.roomUuid) - Giá: $($firstRoom.price) VND"
    }
} catch {
    Write-Warning "Chưa lấy được danh sách phòng: $($_.Exception.Message)"
}

# 4. Hướng dẫn Reset lại dữ liệu ban đầu
Write-Host "`n=== CÁCH ĐẶT LẠI DỮ LIỆU (RESET DEMO) ===" -ForegroundColor Yellow
Write-Host "Để đặt lại toàn bộ dữ liệu mẫu về trạng thái ban đầu:"
Write-Host "1. cd d:\RIKKEI\THUC_TAP\PROJECT\accommodation-service"
Write-Host "2. docker compose --profile full down -v --remove-orphans"
Write-Host "3. docker compose --profile full up -d"
Write-Host "Các Flyway migration và seed data (V1..V17) sẽ tự động nạp lại đầy đủ 78 phòng và các user seed."
Write-Host "=========================================" -ForegroundColor Cyan

