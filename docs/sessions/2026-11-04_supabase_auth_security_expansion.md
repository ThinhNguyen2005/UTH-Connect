# Báo cáo Phiên làm việc: Mở rộng Supabase Auth & Ranh giới Bảo mật (Tương thích 100% Giao diện Đăng nhập Mới)

- **Ngày thực hiện**: 04/11/2026
- **Nhánh làm việc**: `feature/fix_security`
- **Tác giả**: Antigravity Pair Programming
- **Trạng thái kiểm thử**: `BUILD SUCCESSFUL` (Toàn bộ Unit Tests & Build APK hoàn tất không lỗi)

---

## 1. Tóm tắt Bối cảnh & Toàn cảnh Thay đổi

Sau khi người dùng hoàn thành thiết kế giao diện đăng nhập mới theo phong cách Modern Minimalist ("Hello Again!" với luồng 3 bước: `Identifier` $\rightarrow$ `Password` $\rightarrow$ `FirstTimeSetup`), phiên làm việc này tập trung triển khai và kết nối toàn diện các chức năng xác thực cốt lõi của Supabase Auth và PostgreSQL, đảm bảo tuân thủ tuyệt đối các quy tắc bảo mật của dự án UTH Connect:
1. **Thiết kế lại nơi lưu MSSV chờ duyệt**: Xây dựng bảng chuyên biệt `student_verification_requests` và 2 RPC bảo mật (`submit_student_verification_request` cho sinh viên và `admin_review_student_verification` cho admin). Tuyệt đối duy trì quy tắc cấm tài khoản sinh viên (`authenticated`) tự ý sửa cột `student_id` và `student_id_verified` trên `public.profiles`.
2. **Khép kín luồng Quên & Đặt lại mật khẩu (End-to-End Recovery)**: Nâng cấp hộp thoại Quên mật khẩu thành form tương tác kết nối Supabase `/auth/v1/recover`, hỗ trợ Android Deep Link `uthconnect://auth`, và xây dựng màn hình `ResetPassword` để cập nhật mật khẩu mới qua `PUT /auth/v1/user`.
3. **Thực thi quy tắc Domain kép bắt buộc ở Server-side**: Đặt trigger `enforce_user_email_domain` trên `auth.users` ngăn chặn đăng ký ngoài trường, hỗ trợ đầy đủ 2 domain trường: `@ut.edu.vn` và `@uth.edu.vn`.
4. **Hỗ trợ Xác nhận Email & Cooldown 60s**: Xây dựng bước `VerifyEmail` kèm bộ đếm ngược cooldown 60 giây khi đăng ký tài khoản với Supabase.
5. **Đồng bộ hóa thông tin liên hệ sinh viên**: Bổ sung `personal_email` và `current_address` vào `public.profiles` qua migration và cấp quyền `UPDATE` chặt chẽ, kết nối `StudentContactRepository` trong Dagger Hilt.

---

## 2. Kết quả Đạt được & Chi tiết Git Diff

### 2.1. Danh sách chi tiết các file đã sửa đổi & tạo mới

#### Cơ sở dữ liệu & Kiểm thử Bảo mật SQL:
- **`supabase/migrations/20261104000000_auth_security_expansion.sql`** *(Mới)*:
  - Function & Trigger `check_user_email_domain()` trên `auth.users`.
  - Cột `personal_email` và `current_address` trên `public.profiles` kèm quyền `UPDATE` cho `authenticated`.
  - Bảng `student_verification_requests` có RLS (sinh viên chỉ đọc bản ghi của mình, thu hồi trực tiếp `INSERT/UPDATE/DELETE`).
  - RPC `submit_student_verification_request(p_student_id)`: Kiểm tra độ dài MSSV, kiểm tra trùng lặp và tạo yêu cầu PENDING.
  - RPC `admin_review_student_verification(p_request_id, p_decision, p_reason)`: Duyệt và gọi `admin_verify_student`.
- **`supabase/tests/security_contract.sql`** *(Cập nhật)*:
  - Bổ sung assertion kiểm tra RLS trên `student_verification_requests`.
  - Kiểm tra `authenticated` không có quyền ghi trực tiếp trên bảng yêu cầu xác minh.
  - Kiểm tra quyền thực thi và thuộc tính `SECURITY DEFINER` trên 2 RPC mới.

#### Tầng Domain Layer:
- **`domain/repository/AuthRepository.kt`**:
  - Bổ sung các phương thức: `signUp`, `sendPasswordReset`, `completePasswordReset`, `submitVerificationRequest`, `checkVerificationStatus`.
  - Bổ sung data class: `SignUpResult`, `VerificationStatus`, `VerificationRequestResult`.
- **`domain/validation/LoginValidator.kt`**:
  - Nâng cấp regex `universityEmailPattern` chấp nhận cả 2 đuôi `@ut.edu.vn` và `@uth.edu.vn`.
  - Bổ sung hàm tiện ích `isUniversityEmail(email)`.
- **`domain/usecase/SignUpUseCase.kt`** *(Mới)*:
  - Xử lý kiểm tra dữ liệu đầu vào (họ tên, khoa, mật khẩu $\ge 6$ ký tự, khớp xác nhận mật khẩu, domain trường), gọi repository và trả về sealed `SignUpOutcome`.
- **`domain/usecase/ForgotPasswordUseCase.kt`** *(Mới)*:
  - Kiểm tra email trường, xử lý bắt lỗi mạng và rate limit 429, bảo vệ Anti-enumeration.
- **`domain/usecase/CompletePasswordResetUseCase.kt`** *(Mới)*:
  - Kiểm tra mật khẩu mạnh (8-64 ký tự, hoa, thường, số, ký tự đặc biệt) và xử lý liên kết hết hạn.
- **`domain/usecase/SubmitVerificationRequestUseCase.kt`** *(Mới)*:
  - Kiểm tra định dạng MSSV (5-32 ký tự), bắt các mã lỗi nghiệp vụ đã xác minh hoặc đã bị liên kết.

#### Tầng Data Layer:
- **`data/repository/SupabaseRestClient.kt`**:
  - Bổ sung `signUp`: Gửi `POST /auth/v1/signup` kèm metadata `display_name` và `faculty`.
  - Bổ sung `recoverPassword`: Gửi `POST /auth/v1/recover` kèm redirect url `uthconnect://auth/reset-password`.
  - Bổ sung `patchTable`: Hỗ trợ `PATCH` an toàn qua header `X-HTTP-Method-Override: PATCH` và `Prefer: return=representation`.
  - Bổ sung `updatePassword` kèm quản lý token tạm thời `recoveryToken`.
- **`data/repository/SupabaseAuthRepository.kt`**:
  - Hiện thực `AuthRepository` và `StudentContactRepository`.
  - Chuẩn hóa định danh: chuyển MSSV 8-12 số sang email trường khi đăng nhập.
  - Hỗ trợ domain kép `@ut.edu.vn` và `@uth.edu.vn` trong Google SSO.
  - Giữ phiên làm việc cho tài khoản chưa xác minh để có thể gửi yêu cầu xác thực MSSV.
- **`data/repository/SupabaseMappers.kt`**:
  - Ánh xạ thêm `personal_email` và `current_address` từ JSON sang `StudentProfile`.
- **`data/di/AuthDataModule.kt`**:
  - Cung cấp `StudentContactRepository` động (gán `SupabaseAuthRepository` khi `USE_SUPABASE = true`).

#### Tầng Presentation Layer & UI:
- **`AndroidManifest.xml`**:
  - Bổ sung `intent-filter` đón nhận Deep Link scheme `uthconnect://auth`.
- **`presentation/auth/AuthUiState.kt`**:
  - Mở rộng `AuthScreenStep`: thêm `ResetPassword` và `VerifyEmail`.
  - Thêm các trường trạng thái cho Quên mật khẩu, Xác minh MSSV, Đặt lại mật khẩu mới, và bộ đếm cooldown.
- **`presentation/auth/AuthAction.kt`**:
  - Bổ sung các action tương tác cho các bước mới.
- **`presentation/auth/AuthViewModel.kt`**:
  - Kết nối `SubmitFirstTimeSetup` $\rightarrow$ `SignUpUseCase`.
  - Kết nối `SubmitForgotPassword` $\rightarrow$ `ForgotPasswordUseCase`.
  - Kết nối `SubmitVerificationRequest` $\rightarrow$ `SubmitVerificationRequestUseCase`.
  - Kết nối `SubmitResetPassword` $\rightarrow$ `CompletePasswordResetUseCase`.
  - Xử lý Deep Link recovery token và đếm ngược 60 giây gửi lại email kích hoạt.
- **`presentation/auth/AuthScreen.kt`**:
  - Nâng cấp hộp thoại Quên mật khẩu thành form tương tác nhập email.
  - Tích hợp form nhập MSSV và nút "Gửi xác minh" ngay trong banner Google Verification.
  - Bổ sung composable `ModernResetPasswordContent` và `ModernVerifyEmailContent`.
  - Sử dụng 100% token chuẩn `UthDimens` và `UthTheme`.
- **`presentation/auth/AuthRoute.kt`**:
  - Lắng nghe `activity.intent.data` để trích xuất access token từ Deep Link.
  - Hiển thị thông báo qua `SnackbarHostState`.
- **`res/values/strings.xml` & `res/values-en/strings.xml`**:
  - Cập nhật và bổ sung 25 chuỗi đa ngôn ngữ (VI/EN) phục vụ xác thực, phục hồi mật khẩu và xác minh MSSV.

#### Tầng Unit Test:
- **`domain/usecase/SignUpUseCaseTest.kt`** *(Mới)*: 7 test cases kiểm tra validation, domain kép, mật khẩu, và server exception.
- **`domain/usecase/ForgotPasswordUseCaseTest.kt`** *(Mới)*: 6 test cases kiểm tra MSSV sang email, anti-enumeration, rate limit 429 và mạng.
- **`domain/usecase/CompletePasswordResetUseCaseTest.kt`** *(Mới)*: 4 test cases kiểm tra mật khẩu mạnh, khớp mật khẩu, và token hết hạn.
- **`domain/usecase/SubmitVerificationRequestUseCaseTest.kt`** *(Mới)*: 4 test cases kiểm tra định dạng MSSV và mã lỗi trùng lặp.
- **`presentation/auth/AuthViewModelTest.kt`**: Bổ sung test kiểm tra chuyển bước Deep Link, Quên mật khẩu và domain kép.
- **`presentation/auth/AuthAdversarialStressTest.kt`** & **`AuthSecurityAdversarialChallengeTest.kt`**: Đồng bộ chấp nhận domain `@uth.edu.vn`.

---

## 3. Bài học & Kinh nghiệm Rút ra

1. **Ranh giới Bảo mật MSSV**:
   - Việc cấm tuyệt đối sinh viên cập nhật `profiles.student_id` là quyết định kiến trúc đúng đắn. Việc chuyển sang bảng trung gian `student_verification_requests` và RPC có kiểm tra RLS giúp bảo vệ toàn vẹn dữ liệu, loại bỏ hoàn toàn rủi ro mạo danh sinh viên.
2. **Quản lý Phiên Đăng nhập Chưa Xác minh**:
   - Khi đăng nhập Google lần đầu, tài khoản chưa có MSSV được xác nhận. Ban đầu repository gọi `client.signOut()` ngay lập tức, dẫn đến việc người dùng không thể gọi RPC `submit_student_verification_request` do thiếu `auth.uid()`. Việc duy trì phiên đăng nhập ở trạng thái unverified tại client cho phép người dùng thực hiện gửi yêu cầu xác thực hợp lệ ngay trên giao diện.
3. **HTTP PATCH trên HttpURLConnection**:
   - Một số phiên bản Java/Android ném ngoại lệ `ProtocolException` khi thiết lập `connection.requestMethod = "PATCH"`. Giải pháp sử dụng `POST` kết hợp header `X-HTTP-Method-Override: PATCH` và `Prefer: return=representation` đảm bảo 100% khả năng tương thích trên mọi thiết bị.

---

## 4. Vấn đề Tồn đọng / Kỹ thuật Cần Lưu ý

1. **Cấu hình trên Supabase Console**:
   - Migration `20261104000000_auth_security_expansion.sql` đã được áp dụng và chạy kiểm thử an ninh `security_contract.sql` đạt **PASS 100%** trên live Supabase project `bvxilfcctwmgmrwrncvd`.
   - Cả 2 Redirect URLs `uthconnect://auth/callback` và `uthconnect://auth/reset-password` đã được cấu hình thành công trên Supabase Console.
   - Tài khoản test cũ `nguyenthinh30032005@gmail.com` tồn tại trước khi cài trigger domain có thể được xóa thủ công trên console để dọn sạch môi trường.
2. **Cảnh báo deprecation của Hilt**:
   - Một số file route hiển thị warning deprecation cho `hiltViewModel()`. Cần có kế hoạch di chuyển sang `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` ở các phiên tối ưu hóa sau.

---

## 5. Kế hoạch cho các Phiên Tiếp theo

1. **Kiểm thử trên thiết bị thật / Emulator**:
   - Kiểm tra luồng Deep Link `uthconnect://auth/reset-password` khi click link từ email ứng dụng Gmail trên Android.
   - Kiểm tra luồng gửi yêu cầu xác minh MSSV từ tài khoản Google và tài khoản kích hoạt mới.
2. **Tích hợp giao diện Quản trị viên (Admin Review)**:
   - Xây dựng màn hình danh sách yêu cầu xác minh MSSV đang chờ duyệt (`PENDING`) dành riêng cho tài khoản có `role = ADMIN`.

