# scripts/dev-reset-db.ps1
# Script to safely reset dev database schemas without encoding loss
# Strictly forbidden in production

$ErrorActionPreference = "Stop"

$currentProfile = $env:SPRING_PROFILES_ACTIVE
if (-not $currentProfile) {
    $currentProfile = $env:APP_PROFILE
}
if (-not $currentProfile) {
    $currentProfile = "dev"
}

if ($currentProfile -eq "prod" -or $currentProfile -eq "production") {
    Write-Error "CRITICAL: dev-reset-db is strictly forbidden on production profile ($currentProfile)!"
    exit 1
}

Write-Host "Target Environment: $currentProfile (SAFE)" -ForegroundColor Green
Write-Host "Resetting dev databases and re-applying migrations/seeds in UTF-8..." -ForegroundColor Cyan

$scriptPath = Join-Path $PSScriptRoot "reset_db_core.js"
node $scriptPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "dev-reset-db failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Database reset successfully completed!" -ForegroundColor Green

