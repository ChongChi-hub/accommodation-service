<#
.SYNOPSIS
    Kiểm tra cấu hình và gửi email thử nghiệm qua SMTP (hỗ trợ cả Mailpit dev và SMTP thật có STARTTLS).
.DESCRIPTION
    Tự động đọc biến môi trường từ tệp .env (hoặc biến môi trường hiện hành), gửi email thử nghiệm
    tới địa chỉ người dùng nhập. Tuyệt đối không in mật khẩu/khóa bí mật ra màn hình.
#>
param(
    [string]$ToEmail
)

$ErrorActionPreference = "Stop"

# Tìm và nạp tệp .env nếu có
$EnvPath = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $EnvPath) {
    Get-Content $EnvPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim().Trim('"').Trim("'")
            if (-not [Environment]::GetEnvironmentVariable($key)) {
                [Environment]::SetEnvironmentVariable($key, $val, "Process")
            }
        }
    }
}

$MailHost = if ($env:MAIL_HOST) { $env:MAIL_HOST } else { "localhost" }
# Nếu chạy từ máy host và .env ghi 'mailpit', tự đổi sang localhost:1025
if ($MailHost -eq "mailpit") { $MailHost = "localhost" }

$MailPort = if ($env:MAIL_PORT) { [int]$env:MAIL_PORT } else { 1025 }
$MailUser = $env:MAIL_USERNAME
$MailPass = $env:MAIL_PASSWORD
$MailFrom = if ($env:MAIL_FROM) { $env:MAIL_FROM } else { "no-reply@cozynest.vn" }
$MailFromName = if ($env:MAIL_FROM_NAME) { $env:MAIL_FROM_NAME } else { "Tổ Ấm" }
$UseAuth = $env:MAIL_SMTP_AUTH -eq "true"
$UseStartTls = $env:MAIL_SMTP_STARTTLS -eq "true"
$MailEnabled = if ($env:MAIL_ENABLED) { $env:MAIL_ENABLED -eq "true" } else { $true }

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host " KIỂM TRA KẾT NỐI MÁY CHỦ GỬI EMAIL SMTP (TỔ ẤM) " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "MAIL_HOST       : $MailHost"
Write-Host "MAIL_PORT       : $MailPort"
Write-Host "MAIL_FROM       : $MailFrom ($MailFromName)"
Write-Host "MAIL_SMTP_AUTH  : $UseAuth"
Write-Host "MAIL_STARTTLS   : $UseStartTls"
Write-Host "MAIL_ENABLED    : $MailEnabled"
Write-Host "MAIL_USERNAME   : $(if ($MailUser) { $MailUser } else { '(trống)' })"
Write-Host "MAIL_PASSWORD   : $(if ($MailPass) { '*** đã cấu hình ***' } else { '(trống)' })"
Write-Host ""

if (-not $ToEmail) {
    $ToEmail = Read-Host "Nhập địa chỉ email nhận thử nghiệm (vd: your@email.com)"
}

if ([string]::IsNullOrWhiteSpace($ToEmail)) {
    Write-Host "[LỖI] Địa chỉ email nhận không được để trống." -ForegroundColor Red
    exit 1
}

try {
    $smtp = New-Object System.Net.Mail.SmtpClient($MailHost, $MailPort)
    $smtp.Timeout = 10000
    $smtp.EnableSsl = $UseStartTls

    if ($UseAuth) {
        if ([string]::IsNullOrWhiteSpace($MailUser) -or [string]::IsNullOrWhiteSpace($MailPass)) {
            Write-Host "[LỖI] MAIL_SMTP_AUTH=true nhưng thiếu MAIL_USERNAME hoặc MAIL_PASSWORD (Mật khẩu ứng dụng)." -ForegroundColor Red
            exit 1
        }
        $smtp.Credentials = New-Object System.Net.NetworkCredential($MailUser, $MailPass)
    } else {
        $smtp.UseDefaultCredentials = $false
    }

    $msg = New-Object System.Net.Mail.MailMessage
    $msg.From = New-Object System.Net.Mail.MailAddress($MailFrom, $MailFromName, [System.Text.Encoding]::UTF8)
    $msg.To.Add($ToEmail.Trim())
    $msg.Subject = "[Tổ Ấm] Kiểm tra kết nối gửi email SMTP thành công"
    $msg.SubjectEncoding = [System.Text.Encoding]::UTF8
    $msg.BodyEncoding = [System.Text.Encoding]::UTF8
    $msg.IsBodyHtml = $true
    $msg.Body = "<div style='font-family:sans-serif;padding:20px;'><h2 style='color:#0f766e;'>Xin chào từ Tổ Ấm!</h2><p>Email này xác nhận cấu hình SMTP của bạn đang hoạt động bình thường.</p><p style='color:#64748b;font-size:12px;'>Thời gian kiểm tra: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')</p></div>"

    Write-Host "Đang gửi email thử nghiệm tới $ToEmail..." -ForegroundColor Yellow
    $smtp.Send($msg)
    Write-Host "[THÀNH CÔNG] Đã gửi email thử nghiệm thành công qua ${MailHost}:${MailPort}!" -ForegroundColor Green
}
catch {
    $errMsg = $_.Exception.Message
    if ($_.Exception.InnerException) {
        $errMsg += " | " + $_.Exception.InnerException.Message
    }

    Write-Host ""
    Write-Host "[THẤT BẠI] Không thể gửi email thử nghiệm." -ForegroundColor Red

    if ($errMsg -match "535|Authentication|Username and Password not accepted|credentials|5\.7\.8") {
        Write-Host "-> Nguyên nhân: SAI TÊN ĐĂNG NHẬP HOẶC MẬT KHẨU." -ForegroundColor Yellow
        Write-Host "   Gợi ý: Với Gmail/Outlook, phải dùng 'Mật khẩu ứng dụng' (App Password 16 ký tự), KHÔNG dùng mật khẩu đăng nhập tài khoản."
    }
    elseif ($errMsg -match "530|Must issue a STARTTLS|SSL|TLS") {
        Write-Host "-> Nguyên nhân: THIẾU HOẶC SAI CẤU HÌNH MÃ HÓA STARTTLS / SSL." -ForegroundColor Yellow
        Write-Host "   Gợi ý: Kiểm tra MAIL_SMTP_STARTTLS=true (với cổng 587) hoặc false (với Mailpit cổng 1025)."
    }
    elseif ($errMsg -match "No connection could be made|timed out|Unable to connect|actively refused|HostNotFound") {
        Write-Host "-> Nguyên nhân: KHÔNG THỂ KẾT NỐI TỚI MÁY CHỦ SMTP ($MailHost cổng $MailPort)." -ForegroundColor Yellow
        Write-Host "   Gợi ý: Kiểm tra lại MAIL_HOST, sai cổng MAIL_PORT (587/465/1025), Mailpit chưa chạy, hoặc cổng bị nhà mạng/tường lửa chặn."
    }
    elseif ($errMsg -match "550|Sender|relay|policy") {
        Write-Host "-> Nguyên nhân: ĐỊA CHỈ NGƯỜI GỬI BỊ TỪ CHỐI HOẶC CHẶN SPAM." -ForegroundColor Yellow
        Write-Host "   Gợi ý: Đảm bảo MAIL_FROM khớp với tài khoản đã xác minh trên nhà cung cấp SMTP."
    }
    else {
        Write-Host "-> Chi tiết lỗi kỹ thuật: $errMsg" -ForegroundColor Gray
    }
    exit 1
}

