# Google Login Pre-flight Check Script (AUTH-02-FINISH)
# Inspects environment configuration without leaking credentials.

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "  KIEM TRA CAU HINH GOOGLE OAUTH [AUTH-02-FINISH] " -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

$backendEnvPath = "D:\RIKKEI\THUC_TAP\PROJECT\accommodation-service\.env"
$frontendEnvPath = "D:\RIKKEI\THUC_TAP\PROJECT\accommodation-web\.env.local"

# 1. Kiem tra .env backend
$beEnvExists = Test-Path $backendEnvPath
$beHasEnabled = $false
$beHasClientId = $false
$beFingerprint = ""

if ($beEnvExists) {
    $beContent = Get-Content -Path $backendEnvPath -Raw
    if ($beContent -match 'GOOGLE_LOGIN_ENABLED\s*=\s*(true|1)') {
        $beHasEnabled = $true
    }
    if ($beContent -match 'GOOGLE_CLIENT_ID\s*=\s*([^\r\n]+)') {
        $val = $matches[1].Trim()
        if ($val -ne "" -and -not ($val -match '^your[-_]google')) {
            $beHasClientId = $true
            # Tinh SHA-256 fingerprint (8 ky tu dau)
            $sha = [System.Security.Cryptography.SHA256]::Create()
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($val)
            $hash = [System.BitConverter]::ToString($sha.ComputeHash($bytes)).Replace("-", "").ToLower()
            $beFingerprint = $hash.Substring(0, 8)
        }
    }
}

# 2. Kiem tra .env.local frontend
$feEnvExists = Test-Path $frontendEnvPath
$feHasEnabled = $false
$feHasClientId = $false
$feFingerprint = ""

if ($feEnvExists) {
    $feContent = Get-Content -Path $frontendEnvPath -Raw
    if ($feContent -match 'NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED\s*=\s*(true|1)') {
        $feHasEnabled = $true
    }
    if ($feContent -match 'NEXT_PUBLIC_GOOGLE_CLIENT_ID\s*=\s*([^\r\n]+)') {
        $val = $matches[1].Trim()
        if ($val -ne "" -and -not ($val -match '^your[-_]google')) {
            $feHasClientId = $true
            $sha = [System.Security.Cryptography.SHA256]::Create()
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($val)
            $hash = [System.BitConverter]::ToString($sha.ComputeHash($bytes)).Replace("-", "").ToLower()
            $feFingerprint = $hash.Substring(0, 8)
        }
    }
}

# 3. Kiem tra tinh dong nhat giua Backend va Frontend
$fingerprintsMatch = ($beFingerprint -ne "" -and $beFingerprint -eq $feFingerprint)

# 4. Kiem tra Backend Status API qua Gateway
$backendStatusOk = $false
$backendFingerprintMatch = $false
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/google/status" -Method GET -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
    $statusData = if ($resp.data) { $resp.data } else { $resp }
    if ($statusData.enabled -eq $true) {
        $backendStatusOk = $true
    }
    if ($statusData.clientIdFingerprint -eq $beFingerprint) {
        $backendFingerprintMatch = $true
    }
} catch {
    $backendStatusOk = $false
}

# 5. Kiem tra Next.js headers (COOP, CSP)
$coopOk = $false
$cspOk = $false
try {
    $webResp = Invoke-WebRequest -Uri "http://localhost:3000/login" -Method GET -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
    if ($webResp.Headers["Cross-Origin-Opener-Policy"] -match "same-origin-allow-popups") {
        $coopOk = $true
    }
    if ($webResp.Headers["Content-Security-Policy"] -match "accounts\.google\.com") {
        $cspOk = $true
    }
} catch {
    $coopOk = $false
}

$results = @(
    [PSCustomObject]@{
        "Kiem tra" = "1. Backend: GOOGLE_LOGIN_ENABLED"
        "Trang thai" = $(if ($beHasEnabled) { "CO" } else { "KHONG" })
        "Ghi chu" = "Kich hoat Google Auth tren backend"
    },
    [PSCustomObject]@{
        "Kiem tra" = "2. Backend: GOOGLE_CLIENT_ID khong phai placeholder"
        "Trang thai" = $(if ($beHasClientId) { "CO" } else { "KHONG" })
        "Ghi chu" = "Da cau hinh Client ID hop le (an bao mat)"
    },
    [PSCustomObject]@{
        "Kiem tra" = "3. Frontend: NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED"
        "Trang thai" = $(if ($feHasEnabled) { "CO" } else { "KHONG" })
        "Ghi chu" = "Kich hoat Google Auth tren frontend"
    },
    [PSCustomObject]@{
        "Kiem tra" = "4. Frontend: NEXT_PUBLIC_GOOGLE_CLIENT_ID khong phai placeholder"
        "Trang thai" = $(if ($feHasClientId) { "CO" } else { "KHONG" })
        "Ghi chu" = "Da cau hinh Client ID hop le tren web (an bao mat)"
    },
    [PSCustomObject]@{
        "Kiem tra" = "5. Khop Client ID giua Frontend va Backend"
        "Trang thai" = $(if ($fingerprintsMatch) { "CO" } else { "KHONG" })
        "Ghi chu" = $(if ($fingerprintsMatch) { "SHA-256 fingerprint: $beFingerprint" } else { "Khong khop!" })
    },
    [PSCustomObject]@{
        "Kiem tra" = "6. Backend Endpoint GET /google/status tra ve 200"
        "Trang thai" = $(if ($backendStatusOk) { "CO" } else { "KHONG" })
        "Ghi chu" = "Backend API san sang phuc vu OAuth"
    },
    [PSCustomObject]@{
        "Kiem tra" = "7. Backend status fingerprint khop voi Backend .env"
        "Trang thai" = $(if ($backendFingerprintMatch) { "CO" } else { "KHONG" })
        "Ghi chu" = "Chung thuc fingerprint tu container hop le"
    },
    [PSCustomObject]@{
        "Kiem tra" = "8. Web Header COOP: same-origin-allow-popups"
        "Trang thai" = $(if ($coopOk) { "CO" } else { "KHONG" })
        "Ghi chu" = "Next.js cho phep popup Google OAuth giao tiep"
    },
    [PSCustomObject]@{
        "Kiem tra" = "9. Web Header CSP: cho phep Google GIS domains"
        "Trang thai" = $(if ($cspOk) { "CO" } else { "KHONG" })
        "Ghi chu" = "accounts.google.com duoc whitelist day du"
    }
)

$results | Format-Table -AutoSize

$allPassed = ($beHasEnabled -and $beHasClientId -and $feHasEnabled -and $feHasClientId -and $fingerprintsMatch -and $backendStatusOk -and $backendFingerprintMatch -and $coopOk -and $cspOk)

if ($allPassed) {
    Write-Host "`n>>> TAT CA 9/9 MUC KIEM TRA DEU DAT CHUAN! GOOGLE AUTH SAN SANG HOAT DONG! <<<`n" -ForegroundColor Green
} else {
    Write-Host "`n>>> CAN KIEM TRA LAI CAC MUC 'KHONG' O TREN <<<`n" -ForegroundColor Yellow
}
