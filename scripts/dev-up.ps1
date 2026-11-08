# dev-up.ps1 - Antigravity CozyNest Dev Environment Initializer
$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot "..\.env"
if (-not (Test-Path $envFile)) {
    $exampleFile = Join-Path $PSScriptRoot "..\.env.example"
    if (Test-Path $exampleFile) {
        Copy-Item $exampleFile $envFile
        Write-Host "Created .env from .env.example" -ForegroundColor Cyan
    } else {
        New-Item -ItemType File -Path $envFile -Force | Out-Null
    }
}

$envContent = Get-Content $envFile -Raw
if ($envContent -notmatch "INTERNAL_SERVICE_KEY=" -or $envContent -match "INTERNAL_SERVICE_KEY=CozyNestInternalServiceKeySecret2026") {
    $bytes = New-Object byte[] 32
    (New-Object System.Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
    $randomKey = -join ($bytes | ForEach-Object { "{0:x2}" -f $_ })
    if ($envContent -match "INTERNAL_SERVICE_KEY=") {
        $envContent = $envContent -replace "INTERNAL_SERVICE_KEY=.*", "INTERNAL_SERVICE_KEY=$randomKey"
    } else {
        $envContent += "`nINTERNAL_SERVICE_KEY=$randomKey`n"
    }
    Set-Content -Path $envFile -Value $envContent.Trim()
    Write-Host "Generated fresh random INTERNAL_SERVICE_KEY in .env" -ForegroundColor Green
}

Write-Host "Starting CozyNest infrastructure containers..." -ForegroundColor Cyan
docker compose -f (Join-Path $PSScriptRoot "..\docker-compose.yml") --profile core up -d

