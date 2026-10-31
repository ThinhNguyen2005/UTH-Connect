# Script to seed Firestore database for UTH-Connect via REST API
param (
    [string]$StudentId = "2023040123"
)

# Đọc cấu hình từ app/google-services.json thay vì hardcode key
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$GsPath = Join-Path $ScriptDir "..\app\google-services.json"

if (Test-Path $GsPath) {
    $gs = Get-Content $GsPath | ConvertFrom-Json
    $ApiKey = $gs.client[0].api_key[0].current_key
    $ProjectId = $gs.project_info.project_id
} else {
    $ApiKey = $env:FIREBASE_API_KEY
    $ProjectId = $env:FIREBASE_PROJECT_ID
}

if (-not $ApiKey) {
    Write-Error "Không tìm thấy API Key từ app/google-services.json hoặc biến môi trường."
    exit 1
}

Write-Host "Authenticating with Firebase Auth..." -ForegroundColor Cyan
$authPayload = '{"email":"2023040123@ut.edu.vn","password":"123456","returnSecureToken":true}'
try {
    $authRes = Invoke-RestMethod -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$ApiKey" -Method Post -Body $authPayload -ContentType "application/json"
} catch {
    $authRes = Invoke-RestMethod -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$ApiKey" -Method Post -Body $authPayload -ContentType "application/json"
}

$token = $authRes.idToken
$uid = $authRes.localId
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json; charset=utf-8"
}

Write-Host "Seeding student profile and utilities to users/$uid..." -ForegroundColor Yellow
$userPayload = @{
    fields = @{
        studentId = @{ stringValue = $StudentId }
        email = @{ stringValue = "2023040123@ut.edu.vn" }
        fullName = @{ stringValue = "Nguyễn Minh Thắng" }
        faculty = @{ stringValue = "Công nghệ thông tin" }
        class = @{ stringValue = "CNTT2023" }
        tuitionSemester = @{ stringValue = "Học kỳ 1 (Năm học 2025 - 2026)" }
        tuitionTotalAmount = @{ stringValue = "12.650.000 VNĐ" }
        tuitionPaidAmount = @{ stringValue = "12.650.000 VNĐ" }
        tuitionRemainingAmount = @{ stringValue = "0 VNĐ" }
        tuitionIsPaid = @{ booleanValue = $true }
        tuitionCreditsCount = @{ integerValue = "18" }
        tuitionDueDate = @{ stringValue = "15/11/2025" }
        trainingTotalScore = @{ integerValue = "92" }
        trainingMaxScore = @{ integerValue = "100" }
        trainingRank = @{ stringValue = "XUẤT SẮC" }
        trainingSemester = @{ stringValue = "Học kỳ 1 (Năm học 2025 - 2026)" }
    }
} | ConvertTo-Json -Depth 5

$res = Invoke-RestMethod -Uri "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/users/$uid" -Headers $headers -Method Patch -Body ([System.Text.Encoding]::UTF8.GetBytes($userPayload))

Write-Host "Firebase Firestore seed completed successfully for $($res.name)!" -ForegroundColor Green
