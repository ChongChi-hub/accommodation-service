# scripts/google-check.ps1 - Google Identity Services Diagnostics Script
[CmdletBinding()]
param(
    [string]$BackendEnvPath = "",
    [string]$FrontendEnvPath = "",
    [string]$ApiBaseUrl = "http://localhost:8080/api/v1"
)

$ErrorActionPreference = "Continue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
if (-not $scriptDir) { $scriptDir = (Get-Location).Path }

if (-not $BackendEnvPath) {
    $candBe = Join-Path $scriptDir "..\.env"
    if (-not (Test-Path $candBe)) { $candBe = "d:\RIKKEI\THUC_TAP\PROJECT\accommodation-service\.env" }
    $BackendEnvPath = (Resolve-Path $candBe -ErrorAction SilentlyContinue).Path
    if (-not $BackendEnvPath) { $BackendEnvPath = $candBe }
}

if (-not $FrontendEnvPath) {
    $candFe = Join-Path $scriptDir "..\..\accommodation-web\.env.local"
    if (-not (Test-Path $candFe)) { $candFe = "d:\RIKKEI\THUC_TAP\PROJECT\accommodation-web\.env.local" }
    $FrontendEnvPath = (Resolve-Path $candFe -ErrorAction SilentlyContinue).Path
    if (-not $FrontendEnvPath) { $FrontendEnvPath = $candFe }
}

Write-Host "============================================================"
Write-Host "[GOOGLE-CHECK] Kiem tra cau hinh Google OAuth (GIS)"
Write-Host "============================================================"

# 1. Kiem tra file .env phia Backend
Write-Host "`n1. Kiem tra cau hinh Backend (.env):"
$beClientId = ""
$beEnabled = "false"
if (Test-Path $BackendEnvPath) {
    Get-Content $BackendEnvPath | ForEach-Object {
        if ($_ -match "^\s*GOOGLE_CLIENT_ID\s*=\s*(.+)$") { $beClientId = $matches[1].Trim() }
        if ($_ -match "^\s*GOOGLE_LOGIN_ENABLED\s*=\s*(.+)$") { $beEnabled = $matches[1].Trim() }
    }
    Write-Host "  - File ton tai: $BackendEnvPath"
    Write-Host "  - GOOGLE_LOGIN_ENABLED: $beEnabled"
    if ($beClientId) {
        $maskedBe = $beClientId.Substring(0, [Math]::Min(12, $beClientId.Length)) + "..."
        Write-Host "  - GOOGLE_CLIENT_ID: $maskedBe"
    } else {
        Write-Host "  [CANH BAO] GOOGLE_CLIENT_ID chua duoc dat trong backend .env"
    }
} else {
    Write-Host "  [BO QUA] Khong tim thay file: $BackendEnvPath"
}

# 2. Kiem tra file .env.local phia Frontend
Write-Host "`n2. Kiem tra cau hinh Frontend (.env.local):"
$feClientId = ""
$feEnabled = "false"
if (Test-Path $FrontendEnvPath) {
    Get-Content $FrontendEnvPath | ForEach-Object {
        if ($_ -match "^\s*NEXT_PUBLIC_GOOGLE_CLIENT_ID\s*=\s*(.+)$") { $feClientId = $matches[1].Trim() }
        if ($_ -match "^\s*NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED\s*=\s*(.+)$") { $feEnabled = $matches[1].Trim() }
    }
    Write-Host "  - File ton tai: $FrontendEnvPath"
    Write-Host "  - NEXT_PUBLIC_GOOGLE_LOGIN_ENABLED: $feEnabled"
    if ($feClientId) {
        $maskedFe = $feClientId.Substring(0, [Math]::Min(12, $feClientId.Length)) + "..."
        Write-Host "  - NEXT_PUBLIC_GOOGLE_CLIENT_ID: $maskedFe"
    } else {
        Write-Host "  [CANH BAO] NEXT_PUBLIC_GOOGLE_CLIENT_ID chua duoc dat"
    }
} else {
    Write-Host "  [BO QUA] Khong tim thay file: $FrontendEnvPath"
}

# 3. Kiem tra tinh dong nhat cua Client ID Fingerprint (SHA-256 8 ky tu dau)
Write-Host "`n3. Kiem tra fingerprint Client ID:"
function Get-Sha256Fingerprint([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return "" }
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $hash = $sha256.ComputeHash($bytes)
    $hex = -join ($hash[0..3] | ForEach-Object { $_.ToString("x2") })
    return $hex
}

$beFingerprint = Get-Sha256Fingerprint $beClientId
$feFingerprint = Get-Sha256Fingerprint $feClientId

Write-Host "  - Backend SHA-256 Fingerprint : $beFingerprint"
Write-Host "  - Frontend SHA-256 Fingerprint: $feFingerprint"

if ($beFingerprint -and $feFingerprint) {
    if ($beFingerprint -eq $feFingerprint) {
        Write-Host "  [THANH CONG] Fingerprint Backend va Frontend HOAN TOAN KHOP NHAU."
    } else {
        Write-Host "  [LOI NGIEM TRONG] Fingerprint KHONG KHOP giua BE va FE!"
    }
}

# 4. Kiem tra ket noi den Google JWKS
Write-Host "`n4. Kiem tra ket noi Google JWKS certs:"
$jwksUrl = "https://www.googleapis.com/oauth2/v3/certs"
try {
    $jwksResponse = Invoke-RestMethod -Uri $jwksUrl -Method Get -TimeoutSec 5
    $keyCount = $jwksResponse.keys.Count
    Write-Host "  [THANH CONG] Truc tiep ket noi JWKS Google thanh cong ($keyCount khoa cong khai RSA san sang)."
} catch {
    Write-Host "  [CANH BAO] Khong the ket noi JWKS Google: $_"
}

# 5. Kiem tra endpoint Backend /status va /nonce neu dang chay
Write-Host "`n5. Kiem tra API Backend ($ApiBaseUrl):"
try {
    $statusRes = Invoke-RestMethod -Uri "$ApiBaseUrl/auth/google/status" -Method Get -TimeoutSec 3
    Write-Host "  - GET /auth/google/status: enabled=$($statusRes.data.enabled), fingerprint=$($statusRes.data.clientIdFingerprint)"
} catch {
    Write-Host "  - [THONG TIN] Backend service chua khoi dong hoac khong the truy cap qua gateway."
}

try {
    $nonceRes = Invoke-RestMethod -Uri "$ApiBaseUrl/auth/google/nonce" -Method Get -TimeoutSec 3
    if ($nonceRes.data.nonce) {
        Write-Host "  - GET /auth/google/nonce: nonce da cap hop le (do dai $($nonceRes.data.nonce.Length))"
    }
} catch {
    Write-Host "  - [THONG TIN] Endpoint nonce chua san sang de goi."
}

Write-Host "`n============================================================"
Write-Host "[HOAN TAT] Kiem tra hoan tat."
Write-Host "============================================================"
