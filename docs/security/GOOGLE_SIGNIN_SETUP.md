# Đăng nhập bằng Google (Supabase Auth) — Hướng dẫn Chẩn đoán & Cấu hình Chi tiết

Tài liệu này là đặc tả chuẩn và báo cáo chẩn đoán toàn diện về cơ chế đăng nhập Google Sign-In qua Supabase Auth (`-PuseSupabase=true`) trên ứng dụng UTH Connect (`com.giathinh.uthconnect`).

---

## 1. Báo cáo chẩn đoán sự cố Google Client ID (Diagnostic Report)

### 1.1. Hiện trạng và phát hiện sai lệch (Client ID Discrepancy)
Qua quá trình kiểm tra môi trường, rà soát keystore, và thăm dò trực tiếp endpoint Supabase GoTrue, phát hiện có **3 mã Google OAuth Client ID** cùng thuộc Google Cloud Project `324596143171` đang tồn tại nhưng bị cấu hình không đồng bộ:

| Thành phần | Client ID thực tế | Phân loại | Mục đích & Trạng thái |
|---|---|---|---|
| **Live Supabase GoTrue Endpoint** | `324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com` | Web Application Client | **ĐANG HOẠT ĐỘNG (Active)**: Đang được cấu hình trực tiếp trong Supabase Dashboard provider settings (`Location` redirect). |
| **Old Local `gradle.properties`** | `324596143171-ue1ff5n18bj65t74o8k23so5rr0slotv.apps.googleusercontent.com` | Web Application Client | **ĐÃ BỊ XÓA (Deleted)** trên Google Console (`authError=deleted_client`), gây lỗi `[28444] Developer console is not set up correctly`. |
| **Android Keystore Client** | `324596143171-p52o4uqhuaf0mf8c7mlripsdle9ufdvp.apps.googleusercontent.com` | Android Client | Khai báo trong Google Cloud Console liên kết package `com.giathinh.uthconnect` và debug SHA-1. |

### 1.2. Thao tác xác thực trực tiếp qua Live Probe
Lệnh kiểm tra trực tiếp endpoint Supabase không cần đăng nhập giao diện web:
```bash
curl.exe -si -H "apikey: sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo" \
  "https://bvxilfcctwmgmrwrncvd.supabase.co/auth/v1/authorize?provider=google"
```
Kết quả trả về HTTP Header:
```http
HTTP/1.1 302 Found
Location: https://accounts.google.com/o/oauth2/v2/auth?client_id=324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com&redirect_uri=https%3A%2F%2Fbvxilfcctwmgmrwrncvd.supabase.co%2Fauth%2Fv1%2Fcallback&response_type=code&scope=email+profile&state=...
```

### 1.3. Cơ chế gây lỗi gốc rễ (Root Cause Analysis)
1. **Audience Mismatch**:
   - Android Credential Manager xin Google ID token với `serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID` (`324596143171-ue1ff5n...`). Google cấp ID token có claim `aud = 324596143171-ue1ff5n...`.
   - Khi app gửi token tới Supabase GoTrue qua `supabase.auth.signInWith(IDToken)`, GoTrue giải mã JWT và đối chiếu `aud` với Client ID đã lưu trong Dashboard (`324596143171-hletvs7...`).
   - Do `aud` không khớp với Client ID chính của Supabase, GoTrue từ chối token với mã lỗi HTTP 400 (`invalid audience` / `AuthFailure.GoogleRejected`).
2. **Thiếu Logging chẩn đoán tại tầng Android**:
   - Trước đây, `GoogleIdTokenSource.kt` bắt `GetCredentialException` và `GoogleIdTokenParsingException` nhưng bỏ qua (swallow), trả về `GoogleIdTokenResult.Unavailable` mà không ghi logcat. Điều này làm lập trình viên không thể phát hiện lỗi mã 16 (Developer Error do sai SHA-1 hoặc package name) hay lỗi Play Services.

---

## 2. Kiến trúc luồng xác thực (Native ID Token Flow)

UTH Connect sử dụng **Android Credential Manager API** thế hệ mới (thay thế hoàn toàn `GoogleSignInClient` đã bị Google khai tử). Luồng hoạt động không sử dụng trình duyệt web, không cần deep-link hay custom intent:

```
[Người dùng bấm "Tài khoản Google SV (@ut.edu.vn)"]
               │
               ▼
[AuthAction.RequestGoogleSignIn]
               │
               ▼
[AuthRoute: launchGoogleSignIn]
   ├── Tạo nonce ngẫu nhiên: rawNonce = UUID.randomUUID().toString()
   ├── Băm SHA-256: hashedNonce = sha256Hex(rawNonce) (64-char lowercase hex)
   ├── Gọi CredentialManager.create(activity).getCredential(...)
   │     Option: GetGoogleIdOption(serverClientId = WEB_CLIENT_ID, nonce = hashedNonce)
   └── Trả về: GoogleIdTokenCredential.idToken
               │
               ▼
[AuthAction.GoogleIdTokenReceived(idToken, rawNonce)]
               │
               ▼
[SignInWithGoogleUseCase]
               │
               ▼
[SupabaseAuthRepository.authenticateWithGoogle]
   ├── [SupabaseGoogleExchange.exchange(idToken, rawNonce)]
   │     Gọi Supabase GoTrue: POST /auth/v1/token?grant_type=id_token
   │     Body: { id_token, provider = "google", nonce = rawNonce }
   │     (Supabase tự băm SHA-256(rawNonce) và so khớp với claim "nonce" trong ID Token)
   │     Lấy Session: accessToken, refreshToken, userId
   │     Lưu Bearer token vào MemorySessionManager (chỉ giữ trong RAM)
   ├── [Kiểm tra Domain sinh viên UTH]
   │     Lấy email từ ownProfileRow()
   │     Nếu KHÔNG kết thúc bằng "@ut.edu.vn" -> client.signOut() -> ném DomainRestrictionException
   ├── [Kiểm tra Xác minh MSSV]
   │     Nếu student_id_verified == false hoặc student_id trống:
   │       client.signOut()
   │       ném StudentVerificationRequiredException(email, displayName)
   └── [Thành công]: Trả về StudentProfile đã xác minh
```

---

## 3. Các bước thiết lập Google Cloud Console

Truy cập [Google Cloud Console](https://console.cloud.google.com/) với tài khoản quản trị dự án:

### Bước 3.1: Màn hình đồng thuận OAuth (OAuth Consent Screen)
- **User Type**: Chọn **External** (hoặc Internal nếu Google Workspace trường UTH mở cho sinh viên).
- **App name**: `UTH Connect`
- **User support email** & **Developer contact information**: Nhập email quản trị viên.
- **Authorized domains**: Khai báo domain `ut.edu.vn` và `supabase.co`.
- **Scopes**: Thêm 3 scope cơ bản:
  - `openid`
  - `https://www.googleapis.com/auth/userinfo.email`
  - `https://www.googleapis.com/auth/userinfo.profile`

### Bước 3.2: Tạo OAuth Client ID cho Android (Android Client)
- **Application type**: **Android**
- **Name**: `UTH Connect Android Debug`
- **Package name**: `com.giathinh.uthconnect`
- **SHA-1 certificate fingerprint**:
  Lấy từ lệnh Gradle:
  ```powershell
  ./gradlew signingReport
  ```
  Thông tin hiện tại của debug keystore:
  - **Keystore**: `C:\Users\Abc\.android\debug.keystore`
  - **Alias**: `AndroidDebugKey`
  - **SHA-1**: `9E:CC:99:8D:14:74:C7:C2:BB:57:36:26:0D:61:62:B3:42:FB:3B:2A`
  - **Client ID Android được cấp**: `324596143171-p52o4uqhuaf0mf8c7mlripsdle9ufdvp.apps.googleusercontent.com`
- *Lưu ý*: Client ID Android này **không** truyền vào `serverClientId` trong code Android, nhưng bắt buộc phải tồn tại trong Google Cloud Console để Google Play Services cho phép ứng dụng mint ID token.

### Bước 3.3: Tạo OAuth Client ID cho Web Application (Web Client)
- **Application type**: **Web application**
- **Name**: `UTH Connect Web Client (Supabase)`
- **Authorized redirect URIs**:
  Thêm đúng URI callback của dự án Supabase:
  `https://bvxilfcctwmgmrwrncvd.supabase.co/auth/v1/callback`
- Nhấn **Create** và lưu lại:
  - **Web Client ID**: `324596143171-ue1ff5n18bj65t74o8k23so5rr0slotv.apps.googleusercontent.com` (hoặc `324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com`)
  - **Web Client Secret**: (Giữ bí mật, chỉ nhập vào Supabase Dashboard, KHÔNG commit vào git).

---

## 4. Các bước thiết lập Supabase Dashboard

Truy cập [Supabase Dashboard](https://app.supabase.com/) → Dự án `bvxilfcctwmgmrwrncvd`:

1. Đi tới **Authentication** → **Providers** → Chọn **Google**:
   - **Enable Sign in with Google**: Bật `ON`.
   - **Client ID (for OAuth)**:
     Nhập Web Client ID: `324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com` (hoặc `324596143171-ue1ff5n18bj65t74o8k23so5rr0slotv.apps.googleusercontent.com`).
   - **Client Secret (for OAuth)**: Nhập Client Secret tương ứng của Web Client trên.
   - **Authorized Client IDs**:
     Thêm danh sách các Client ID được phép gửi ID token:
     ```
     324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com
     324596143171-ue1ff5n18bj65t74o8k23so5rr0slotv.apps.googleusercontent.com
     ```
     *(Việc thêm cả 2 ID vào Authorized Client IDs giúp hệ thống tương thích ngay cả khi dev dùng client ID nào)*.
   - **Skip Nonce Check**: **BẮT BUỘC TẮT (Disabled)**.
     *Lý do*: App UTH Connect luôn tạo mã nonce bảo mật chuẩn mã hóa SHA-256 chống tấn công phát lại (replay attack). Tắt Skip Nonce Check đảm bảo GoTrue luôn xác thực claim `nonce`.
2. Đi tới **Authentication** → **URL Configuration**:
   - **Redirect URLs**: Đảm bảo có `https://bvxilfcctwmgmrwrncvd.supabase.co/auth/v1/callback`.

---

## 5. Cấu hình môi trường Local Developer

Khai báo thông số trong file `~/.gradle/gradle.properties` (hoặc `gradle.properties` của dự án):

```properties
useSupabase=true
useMockData=false
supabaseUrl=https://bvxilfcctwmgmrwrncvd.supabase.co
supabasePublishableKey=sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo
googleWebClientId=324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com
```

*Lưu ý*: Nếu Supabase Dashboard đang cấu hình `324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com`, đặt `googleWebClientId` bằng giá trị này để `aud` khớp hoàn toàn 100%.

Lệnh biên dịch và chạy kiểm thử:
```powershell
.\gradlew assembleDebug testDebugUnitTest
```

---

## 6. Chẩn đoán và sửa lỗi thường gặp (Troubleshooting)

| Triệu chứng / Log lỗi | Nguyên nhân gốc rễ | Cách khắc phục triệt để |
|---|---|---|
| `GetCredentialCustomException: [28444]` (Developer console is not set up correctly) | 1. Web Client ID bị xóa hoặc không tồn tại trong Console (`deleted_client`).<br>2. Android Client ID chưa thêm đúng package `com.giathinh.uthconnect` và debug SHA-1.<br>3. OAuth Consent Screen đang ở chế độ Testing và tài khoản Google chưa được thêm vào mục Test Users. | Sử dụng Web Client ID còn sống (`324596143171-hletvs7...`) trong `gradle.properties`. Kiểm tra Android OAuth Client và thêm tài khoản vào Test Users trong Google Cloud Console nếu ở chế độ Testing. |
| `GetCredentialException: 16` (Developer Error) | Keystore SHA-1 hoặc package name chưa được đăng ký trong Google Cloud Console. | Chạy `./gradlew signingReport`, lấy SHA-1 của debug keystore và thêm vào Android OAuth Client trong Google Cloud Console với package `com.giathinh.uthconnect`. |
| `AuthFailure.GoogleRejected` / HTTP 400 `invalid audience` | `googleWebClientId` trong app không trùng với Client ID đã đăng ký trong Supabase Google Provider. | Đồng bộ `googleWebClientId` và Supabase Dashboard Client ID; hoặc thêm `googleWebClientId` vào ô "Authorized Client IDs" trong Supabase Dashboard. |
| HTTP 400 `invalid nonce` | Nonce bị gửi ngược hoặc Skip Nonce Check bị bật. | Đảm bảo code gửi `sha256Hex(rawNonce)` cho Google Credential Manager và gửi `rawNonce` gốc cho Supabase `signInWith(IDToken)`. |
| `DomainRestrictionException` | Người dùng sử dụng tài khoản Google cá nhân (không phải `@ut.edu.vn`). | Thông báo người dùng sử dụng email sinh viên trường (@ut.edu.vn). |
| `StudentVerificationRequiredException` | Tài khoản Google sinh viên lần đầu đăng nhập, chưa được admin duyệt MSSV. | Ứng dụng tự động hiển thị banner "Đang chờ xác minh MSSV" kèm email sinh viên, không xem đây là lỗi sập app. Quản trị viên gọi RPC `admin_verify_student(p_user_id, p_student_id)` để xác nhận. |
