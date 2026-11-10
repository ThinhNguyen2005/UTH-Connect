# Quy chuẩn & Ràng buộc Xác thực (Authentication Constraints & Security Architecture)

Tài liệu này quy định đầy đủ các ràng buộc nghiệp vụ, chính sách bảo mật, quy tắc xác minh danh tính và vòng đời phiên đăng nhập của hệ thống UTH Connect.

---

## 1. Ràng buộc Tên miền Email Sinh viên (@ut.edu.vn)

### 1.1. Mục tiêu bảo mật
UTH Connect là hệ thống phục vụ riêng cho cộng đồng sinh viên, giảng viên và cán bộ Trường Đại học Giao thông vận tải TP.HCM (UTH). Mọi cơ chế đăng nhập đều phải kiểm soát chặt chẽ danh tính thuộc miền quản lý của nhà trường.

### 1.2. Quy tắc áp dụng cho Đăng nhập bằng Mật khẩu (Password Mode)
- **Đầu vào hợp lệ**:
  - Mã số sinh viên (MSSV) gồm 8 đến 12 chữ số: Regex `^[0-9]{8,12}$`.
  - Email sinh viên chính thức: Regex `^[A-Za-z0-9._%+\\-]+@ut\\.edu\\.vn$` (không phân biệt chữ hoa, chữ thường).
- **Cơ chế chuẩn hóa**:
  - Khi sinh viên nhập MSSV (ví dụ: `2023040123`), hệ thống tự động chuẩn hóa thành `2023040123@ut.edu.vn` trước khi gửi tới API xác thực backend (`SupabaseAuthRepository.authenticate`).
  - Mọi email có đuôi khác (như `@gmail.com`, `@yahoo.com`, `@uth.edu.vn` sai định dạng) đều bị từ chối ngay tại tầng Domain Validator (`LoginValidator.kt`) với mã lỗi `LoginFieldError.InvalidIdentifier`.

### 1.3. Quy tắc áp dụng cho Đăng nhập bằng Google SSO (Google Mode)
- **Ràng buộc Domain cấp Repository**:
  - Khi người dùng đăng nhập bằng Google Credential Manager, sau khi hoàn tất trao đổi ID token với Supabase GoTrue, `SupabaseAuthRepository.authenticateWithGoogle` kiểm tra:
    ```kotlin
    val email = row?.string("email").orEmpty()
    if (!email.endsWith("@ut.edu.vn", ignoreCase = true)) {
        client.signOut()
        throw DomainRestrictionException(email)
    }
    ```
  - Nếu tài khoản Google không thuộc miền `@ut.edu.vn`, phiên làm việc bị hủy ngay lập tức (`client.signOut()`) và ngoại lệ `DomainRestrictionException` được ném ra.
  - `SignInWithGoogleUseCase` bắt ngoại lệ này và chuyển thành `SignInWithGoogleResult.Rejected`. Giao diện hiển thị thông báo từ chối đăng nhập.

---

## 2. Quy trình Xử lý Tài khoản Google Sinh viên Mới (Verification Lifecycle)

### 2.1. Trigger tự động trong Cơ sở dữ liệu Postgres
- Khi sinh viên đăng nhập Google lần đầu, Supabase GoTrue tự động thêm bản ghi vào `auth.users`.
- Trigger cơ sở dữ liệu `handle_new_user()` (trong file migration `20261103010000_secure_core_workflows.sql`) được kích hoạt:
  ```sql
  create or replace function public.handle_new_user()
  returns trigger language plpgsql security definer as $$
  begin
    insert into public.profiles (id, email, display_name, avatar_url, student_id, student_id_verified)
    values (
      new.id,
      new.email,
      coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''),
      coalesce(new.raw_user_meta_data->>'avatar_url', new.raw_user_meta_data->>'picture', ''),
      null,
      false
    )
    on conflict (id) do nothing;
    return new;
  end;
  $$;
  ```
- **Hệ quả**: Bản ghi `public.profiles` được khởi tạo với:
  - `student_id = NULL`
  - `student_id_verified = false`

### 2.2. Kiểm soát tại Tầng Ứng dụng & Xử lý Trạng thái Chờ (Pending Banner)
- Tại `SupabaseAuthRepository.authenticateWithGoogle`:
  ```kotlin
  if (row == null || !row.bool("student_id_verified") || row.string("student_id").isNullOrBlank()) {
      val displayName = row?.string("display_name").orEmpty()
      client.signOut()
      throw StudentVerificationRequiredException(email = email, displayName = displayName)
  }
  ```
- Khi phát hiện sinh viên chưa xác thực (`student_id_verified == false`):
  1. Hủy phiên làm việc ngay tại client (`client.signOut()`) để tránh rò rỉ Bearer token.
  2. Ném ngoại lệ có cấu trúc `StudentVerificationRequiredException(email, displayName)`.
  3. `SignInWithGoogleUseCase` nhận diện ngoại lệ và trả về `SignInWithGoogleResult.VerificationPending(email, displayName)`.
  4. `AuthViewModel` cập nhật `uiState.googleVerification = GoogleVerificationNotice(email, displayName)`.
  5. `AuthScreen` hiển thị khối thông báo **"Đang chờ xác minh MSSV"** (`auth_google_pending`) kèm địa chỉ email sinh viên.
- **Trải nghiệm người dùng**: Sinh viên hiểu rõ tài khoản đang chờ phê duyệt thay vì thấy thông báo lỗi chung chung ("Sai mật khẩu" hoặc "Đăng nhập thất bại").

### 2.3. Quy trình Phê duyệt MSSV của Quản trị viên (Admin Verification RPC)
- Sinh viên không thể tự gán hoặc thay đổi `student_id` và `student_id_verified` (bị chặn bởi chính sách Row-Level Security RLS).
- Quản trị viên sử dụng RPC chuyên dụng có quyền `SECURITY DEFINER` (migration `20261103030000_enforce_verified_checkin.sql`):
  ```sql
  create or replace function public.admin_verify_student(
    p_user_id uuid,
    p_student_id text
  ) returns jsonb ...
  ```
- Hàm kiểm tra định dạng MSSV, kiểm tra quyền admin và cập nhật `student_id_verified = true`.
- **Ràng buộc duy nhất (Identity Uniqueness)**:
  Index có điều kiện đảm bảo không bao giờ có 2 tài khoản cùng mang một MSSV đã xác thực:
  ```sql
  create unique index if not exists profiles_verified_student_id_unique
    on public.profiles (upper(btrim(student_id)))
    where student_id_verified;
  ```

### 2.4. Khóa bảo vệ Tính năng Nghiệp vụ (Security Gate)
Tất cả các hàm RPC cốt lõi liên quan đến sự kiện trường đều khóa cứng đối với tài khoản chưa xác thực MSSV:
- `register_for_event(p_event_id)`
- `issue_checkin_token(p_event_id)`
- `check_in(p_event_id, p_token)`
Nếu tài khoản chưa có `student_id_verified = true`, cơ sở dữ liệu sẽ raise exception `STUDENT_NOT_VERIFIED` với mã lỗi SQL `42501` (Forbidden).

---

## 3. Kiến trúc Lưu trữ & Quản lý Phiên Đăng nhập (Session Persistence)

### 3.1. Nguyên tắc An toàn Tuyệt đối: Zero Unencrypted Token Storage
Theo tiêu chuẩn bảo mật hệ thống thông tin trường học:
- **Không bao giờ lưu Bearer Access Token hoặc Refresh Token dạng văn bản thô (plaintext)** vào `SharedPreferences` hoặc `SQLite`.
- Trong môi trường Supabase (`BuildConfig.USE_SUPABASE == true`), toàn bộ phiên làm việc của người dùng là **Ephemeral In-Memory**:
  - `MemorySessionManager()` được cấu hình cho Supabase Auth SDK.
  - Tầng HTTP Transport `SupabaseRestClient` lưu thông tin phiên trong biến `credentials: InFlightCredentials?` trên RAM.
  - Khi tiến trình ứng dụng kết thúc (Process Death / Cold Start), token tự động bị giải phóng khỏi bộ nhớ RAM. Người dùng cần đăng nhập lại ở phiên làm việc kế tiếp.

### 3.2. Ràng buộc Tùy chọn "Ghi nhớ đăng nhập" (Remember Me)
- Trong `PreferencesSessionRepository.kt`:
  ```kotlin
  if (remember && !BuildConfig.USE_SUPABASE) {
      edit.putString(KEY_ID, profile.studentId)
  } else {
      edit.remove(KEY_ID)
  }
  ```
- Tùy chọn ghi nhớ mã sinh viên chỉ hoạt động ở chế độ Demo nội bộ. Ở chế độ Supabase kết nối dữ liệu thật của trường, hệ thống luôn loại bỏ cờ ghi nhớ để ngăn chặn rủi ro lộ định danh khi dùng chung thiết bị.

### 3.3. Ràng buộc Đăng nhập Sinh trắc học (Biometric Authentication)
- Ở chế độ Demo: Mã sinh viên có thể được lưu trữ cục bộ khi người dùng tích chọn "Kích hoạt vân tay lần sau", cho phép gọi `BiometricPrompt` để mở nhanh phiên sinh viên demo.
- Ở chế độ Supabase: `PreferencesSessionRepository` kiểm tra nghiêm ngặt:
  ```kotlin
  check(!BuildConfig.USE_SUPABASE) {
      "Biometric unlock requires an authenticated Supabase session"
  }
  ```
  Để mở rộng tính năng sinh trắc học với Supabase trong tương lai, hệ thống yêu cầu triển khai `Android Keystore` với khóa `MasterKey` phần cứng mã hóa Refresh Token an toàn (Keystore-backed Token Storage).

---

## 4. Tóm tắt Bảng Ma trận Quyền & Ràng buộc Xác thực

| Trạng thái tài khoản | Đăng nhập Google | Đăng nhập MSSV | Xem Bảng tin / Tiện ích | Đăng ký Sự kiện / Nhận vé QR | Điểm danh Sự kiện |
|---|:---:|:---:|:---:|:---:|:---:|
| **Chưa đăng nhập** | Có | Có | Chỉ xem thông tin công khai | Không | Không |
| **Email ngoài @ut.edu.vn** | Bị từ chối | Không hỗ trợ | Không | Không | Không |
| **Google SV chưa duyệt MSSV** | Chờ xác minh | Không | Không | Bị chặn (42501) | Bị chặn (42501) |
| **Sinh viên UTH đã xác minh** | Thành công | Thành công | Đầy đủ | Đầy đủ | Đầy đủ |
| **Ban tổ chức / Admin** | Thành công | Thành công | Đầy đủ + Quản trị | Quản lý & Tạo sự kiện | Quét QR điểm danh |
