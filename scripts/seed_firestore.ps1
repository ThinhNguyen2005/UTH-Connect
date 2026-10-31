# Script to seed Firestore database for UTH-Connect via REST API
param (
    [string]$ApiKey = "AIzaSyBEHyI3RB4cefMk9n0F9UYbr1CojA5F8c8",
    [string]$ProjectId = "uthconnect-d4d91",
    [string]$StudentId = "2023040123"
)

Write-Host "Authenticating with Firebase Auth..." -ForegroundColor Cyan
$authPayload = '{"email":"testuser2023@ut.edu.vn","password":"Password@123456","returnSecureToken":true}'
try {
    $authRes = Invoke-RestMethod -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$ApiKey" -Method Post -Body $authPayload -ContentType "application/json"
} catch {
    # If not registered yet, sign up
    $authRes = Invoke-RestMethod -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$ApiKey" -Method Post -Body $authPayload -ContentType "application/json"
}

$token = $authRes.idToken
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json; charset=utf-8"
}

Write-Host "Seeding student profile..." -ForegroundColor Yellow
$studentPayload = @{
    fields = @{
        studentId = @{ stringValue = $StudentId }
        fullName = @{ stringValue = "Nguyễn Minh Thắng" }
        faculty = @{ stringValue = "Công nghệ thông tin" }
        class = @{ stringValue = "CNTT2023" }
    }
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/students/$StudentId" -Headers $headers -Method Patch -Body ([System.Text.Encoding]::UTF8.GetBytes($studentPayload))

Write-Host "Seeding tuition information..." -ForegroundColor Yellow
$tuitionPayload = @{
    fields = @{
        semester = @{ stringValue = "Học kỳ 1 (Năm học 2025 - 2026)" }
        isPaid = @{ booleanValue = $true }
        totalAmount = @{ stringValue = "12.650.000 VNĐ" }
        paidAmount = @{ stringValue = "12.650.000 VNĐ" }
        remainingAmount = @{ stringValue = "0 VNĐ" }
        creditsCount = @{ integerValue = "18" }
        dueDate = @{ stringValue = "15/11/2025" }
        courses = @{
            arrayValue = @{
                values = @(
                    @{ mapValue = @{ fields = @{ code = @{ stringValue = "IT001" }; name = @{ stringValue = "Lập trình thiết bị di động" }; credits = @{ integerValue = "3" }; fee = @{ stringValue = "2.100.000 VNĐ" } } } },
                    @{ mapValue = @{ fields = @{ code = @{ stringValue = "IT002" }; name = @{ stringValue = "Kiến trúc & Thiết kế phần mềm" }; credits = @{ integerValue = "3" }; fee = @{ stringValue = "2.100.000 VNĐ" } } } },
                    @{ mapValue = @{ fields = @{ code = @{ stringValue = "IT003" }; name = @{ stringValue = "Kiểm thử & Đảm bảo chất lượng" }; credits = @{ integerValue = "3" }; fee = @{ stringValue = "2.100.000 VNĐ" } } } },
                    @{ mapValue = @{ fields = @{ code = @{ stringValue = "IT004" }; name = @{ stringValue = "Công nghệ phần mềm chuyên sâu" }; credits = @{ integerValue = "3" }; fee = @{ stringValue = "2.100.000 VNĐ" } } } },
                    @{ mapValue = @{ fields = @{ code = @{ stringValue = "IT005" }; name = @{ stringValue = "Đồ án chuyên ngành KTPM" }; credits = @{ integerValue = "3" }; fee = @{ stringValue = "2.100.000 VNĐ" } } } },
                    @{ mapValue = @{ fields = @{ code = @{ stringValue = "GE006" }; name = @{ stringValue = "Kỹ năng nghề nghiệp & Giao tiếp" }; credits = @{ integerValue = "3" }; fee = @{ stringValue = "2.150.000 VNĐ" } } } }
                )
            }
        }
    }
} | ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/students/$StudentId/tuition/hk1" -Headers $headers -Method Patch -Body ([System.Text.Encoding]::UTF8.GetBytes($tuitionPayload))

Write-Host "Seeding training point information..." -ForegroundColor Yellow
$tpPayload = @{
    fields = @{
        semester = @{ stringValue = "Học kỳ 1 (Năm học 2025 - 2026)" }
        totalScore = @{ integerValue = "88" }
        maxScore = @{ integerValue = "100" }
        rank = @{ stringValue = "TỐT" }
        criteria = @{
            arrayValue = @{
                values = @(
                    @{ mapValue = @{ fields = @{ title = @{ stringValue = "1. Ý thức tham gia học tập" }; score = @{ integerValue = "18" }; maxScore = @{ integerValue = "20" }; description = @{ stringValue = "Đi học đầy đủ, đúng giờ, không vi phạm quy chế kiểm tra thi cử." } } } },
                    @{ mapValue = @{ fields = @{ title = @{ stringValue = "2. Ý thức chấp hành nội quy, quy chế" }; score = @{ integerValue = "25" }; maxScore = @{ integerValue = "25" }; description = @{ stringValue = "Chấp hành tốt quy định của Nhà nước, nhà trường và nội quy sinh viên." } } } },
                    @{ mapValue = @{ fields = @{ title = @{ stringValue = "3. Hoạt động phong trào Đoàn - Hội" }; score = @{ integerValue = "18" }; maxScore = @{ integerValue = "20" }; description = @{ stringValue = "Tham gia các buổi hội thảo, hoạt động tình nguyện, CLB học thuật." } } } },
                    @{ mapValue = @{ fields = @{ title = @{ stringValue = "4. Ý thức công dân & Quan hệ cộng đồng" }; score = @{ integerValue = "22" }; maxScore = @{ integerValue = "25" }; description = @{ stringValue = "Tích cực tham gia hoạt động văn hóa, giúp đỡ cộng đồng, lối sống lành mạnh." } } } },
                    @{ mapValue = @{ fields = @{ title = @{ stringValue = "5. Khen thưởng và thành tích đặc biệt" }; score = @{ integerValue = "5" }; maxScore = @{ integerValue = "10" }; description = @{ stringValue = "Đạt giải trong các cuộc thi học thuật hoặc phong trào nghiên cứu khoa học." } } } }
                )
            }
        }
    }
} | ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/students/$StudentId/training_points/hk1" -Headers $headers -Method Patch -Body ([System.Text.Encoding]::UTF8.GetBytes($tpPayload))

Write-Host "Firebase Firestore seed completed successfully!" -ForegroundColor Green
