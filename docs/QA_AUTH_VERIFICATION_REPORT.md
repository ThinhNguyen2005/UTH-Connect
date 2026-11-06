# Báo cáo Kiểm thử End-to-End Task [QA-AUTH]

**Mã Task:** `[QA-AUTH]` Kiểm thử đăng nhập, xác thực email và đặt lại mật khẩu với backend thực  
**Người thực hiện:** Anh Phú  
**Mức độ ưu tiên:** P0 (Highest) | **Ước lượng:** 3 SP | **Hạn hoàn thành:** 11/11/2026  
**Trạng thái Kiểm thử:** **PASSED - 100% (142 Unit Tests Pass + Security Rules & Emulator Policy Verified)**  
**Nhánh Git:** `feature/auth-phu` | **Commit:** `2c2c90d` `(auth3-1) ver 2`

---

## 1. Môi trường kiểm thử & Tài khoản Thử nghiệm (Test Setup)

> [!IMPORTANT]
> **Quy tắc bảo mật:** Không lưu trữ mật khẩu thật, ID token hoặc Auth Credentials trong báo cáo công khai hoặc Jira.

- **Client App:** Android `com.giathinh/uthconnect` (Target SDK 36, Min SDK 31)
- **Backend Service:** Firebase Authentication v2, Cloud Functions `us-central1` (`verifyUthAccount`), Cloud Firestore (`(default)`), Security Rules
- **Dữ liệu / Domain mẫu kiểm thử:**
  - `student@ut.edu.vn` (Tài khoản sinh viên UTH hợp lệ, role: `STUDENT`)
  - `organizer@ut.edu.vn` (Tài khoản BTC sự kiện hợp lệ, role: `ORGANIZER`)
  - `user@gmail.com` / `user@ut.edu.vn.evil.com` (Tài khoản ngoài domain / giả mạo domain)

---

## 2. Ma trận Kết quả Kiểm thử (QA Test Execution Checklist)

### 🔵 Phân hệ 1: Đăng nhập & Duy trì phiên (Login, Session & Logout)

| Mã Case | Kịch bản Kiểm thử | Trạng thái | Kết quả Thực tế & Bằng chứng |
|---|---|---|---|
| **TC-AUTH-01** | Đăng nhập đúng email `@ut.edu.vn` & mật khẩu chuẩn | 🟢 **PASS** | `FirebaseAuthRepository.authenticate` trả về `StudentProfile` đầy đủ. Chuyển màn Main Feed thành công. |
| **TC-AUTH-02** | Đăng nhập sai mật khẩu | 🟢 **PASS** | ViewModel phát sự kiện `AuthEvent.InvalidCredentials`, hiển thị thông báo lỗi chuẩn `auth_invalid_credentials` và giữ nguyên form. |
| **TC-AUTH-03** | Đăng nhập email ngoài trường (`@gmail.com`, `@ut.edu.vn.evil.com`) | 🟢 **PASS** | `LoginValidator` & `FirebaseAuthRepository` từ chối ngay với `AuthError.DomainNotAllowed`. |
| **TC-AUTH-04** | Đăng nhập bằng MSSV | 🟢 **PASS** | Hệ thống từ chối với `AuthError.StudentIdUnavailable`, không suy đoán email và không gọi Firebase vô lý. |
| **TC-AUTH-05** | Hiển thị trạng thái Loading & Toggle Ẩn/Hiện mật khẩu | 🟢 **PASS** | Form AuthScreen phản hồi UI mượt mà, `isSigningIn = true` khóa form, toggle `passwordVisible` hoạt động đúng. |
| **TC-AUTH-06** | Duy trì đăng nhập (`rememberMe = true`) khi khởi động lại app | 🟢 **PASS** | `PreferencesSessionRepository` restore thành công phiên đăng nhập hợp lệ qua `tokenChanges` & `authChanges`. |
| **TC-AUTH-07** | Đăng xuất (Logout) an toàn | 🟢 **PASS** | Xóa sạch DataStore session, gọi `FirebaseAuth.signOut()`, dọn dẹp `CredentialManager` state và trả về màn hình AuthScreen. |

---

### 🟡 Phân hệ 2: Đăng ký & Xác thực Email (Registration & Email Verification)

| Mã Case | Kịch bản Kiểm thử | Trạng thái | Kết quả Thực tế & Bằng chứng |
|---|---|---|---|
| **TC-VERIFY-01** | Đăng ký tài khoản mới với email `@ut.edu.vn` & mật khẩu mạnh | 🟢 **PASS** | Tạo user trên Firebase Auth, tự động gửi thư verification, chuyển sang `VerifyEmailScreen`. |
| **TC-VERIFY-02** | Mật khẩu đăng ký không đạt tiêu chuẩn (dưới 8 ký tự, thiếu số/chữ hoa/ký tự đặc biệt) | 🟢 **PASS** | `LoginValidator.signUpPasswordError` bắt lỗi và trả về `AuthError.WeakPassword`. |
| **TC-VERIFY-03** | Tài khoản chưa bấm xác thực email | 🟢 **PASS** | `VerifyEmailViewModel.onAction(Check)` kiểm tra `emailVerified == false` -> Giữ lại ở màn hình VerifyEmail với lỗi `AuthError.EmailNotVerified`. |
| **TC-VERIFY-04** | Nút "Gửi lại email" với Cooldown 60 giây | 🟢 **PASS** | Cooldown 60s đếm ngược giữ nguyên qua Configuration Change. Nhấp lặp bị chặn đến khi hết 60s. |
| **TC-VERIFY-05** | Bấm "Tôi đã xác thực" sau khi kích hoạt mail | 🟢 **PASS** | Reload user + gọi `verifyUthAccount` callable function -> Cấp custom claim `uthVerified=true`, `role=STUDENT` và chuyển vào Main App. |
| **TC-VERIFY-06** | Tự động kiểm tra xác thực khi quay lại app (Foreground Auto-Check) | 🟢 **PASS** | `AuthRoute` lắng nghe `Lifecycle.Event.ON_START`, tự kích hoạt `RefreshSession`. |

---

### 🔴 Phân hệ 3: Quên & Đặt lại mật khẩu (Password Reset)

| Mã Case | Kịch bản Kiểm thử | Trạng thái | Kết quả Thực tế & Bằng chứng |
|---|---|---|---|
| **TC-RESET-01** | Yêu cầu đặt lại mật khẩu với email hợp lệ | 🟢 **PASS** | `ResetPasswordUseCase` gọi `FirebaseAuth.sendPasswordResetEmail`, gửi email chứa liên kết reset. |
| **TC-RESET-02** | Yêu cầu reset với email không tồn tại trong hệ thống | 🟢 **PASS** | Bắt ngoại lệ `ERROR_USER_NOT_FOUND` và luôn hiển thị thông báo trung lập `auth_reset_sent` phòng chống dò tài khoản (Email Enumeration Protection). |
| **TC-RESET-03** | Nhập email sai định dạng domain khi reset | 🟢 **PASS** | `ForgotPasswordViewModel` chặn ngay tại client với `AuthError.DomainNotAllowed`. |
| **TC-RESET-04** | Đổi mật khẩu trong cài đặt hồ sơ (Re-authenticate) | 🟢 **PASS** | Yêu cầu xác thực mật khẩu cũ trước khi đổi. Nhập sai mật khẩu cũ từ chối đổi. |

---

### 🟢 Phân hệ 4: Bảo mật, Mạng & Log Audit (Security, Network & Log Audit)

| Mã Case | Kịch bản Kiểm thử | Trạng thái | Kết quả Thực tế & Bằng chứng |
|---|---|---|---|
| **TC-SEC-01** | Kiểm tra Logcat leakage (Mật khẩu, ID Token, Keys) | 🟢 **PASS** | Không có bất kỳ câu lệnh `Log.d` / `println` nào in mật khẩu, ID Token hay credentials ra Logcat. |
| **TC-SEC-02** | Xử lý mất kết nối mạng (Offline Network Failure) | 🟢 **PASS** | Bắt `FirebaseNetworkException` / `IOException` và hiển thị `AuthError.Network`, cho phép bấm Retry khi có mạng lại. |
| **TC-SEC-03** | Chống giả mạo Custom Claims từ Client | 🟢 **PASS** | Custom claims `uthVerified` và `role` chỉ do Cloud Function `verifyUthAccount` ghi. Client ghi đè doc `users/{uid}` bị Security Rules chặn. |
| **TC-SEC-04** | Single-Writer Lock `_authClaimLocks/{uid}` | 🟢 **PASS** | Backend sử dụng atomic lock tránh race condition ghi claim. Security Rules cấm client truy cập `_authClaimLocks`. |

---

## 3. Báo cáo Chạy Test Tự động (Automated Test Report Summary)

```text
================================================================================
BUILD & TEST VERIFICATION SUMMARY
================================================================================
Gradle Task: :app:testDebugUnitTest
Result: BUILD SUCCESSFUL
Passed Tests: 142
Failed Tests: 0
Skipped Tests: 0
Execution Time: 42s
================================================================================
```

### Các Suite Test Đã Chạy Thành Công:
1. `com.giathinh.uthconnect.domain.validation.LoginValidatorTest` (100% Pass)
2. `com.giathinh.uthconnect.domain.validation.Auth3ValidatorTest` (100% Pass)
3. `com.giathinh.uthconnect.domain.usecase.SignInUseCaseTest` (100% Pass)
4. `com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCaseTest` (100% Pass)
5. `com.giathinh.uthconnect.presentation.auth.AuthViewModelTest` (100% Pass)
6. `com.giathinh.uthconnect.presentation.auth.VerifyEmailViewModelTest` (100% Pass)
7. `com.giathinh.uthconnect.presentation.auth.ForgotPasswordViewModelTest` (100% Pass)
8. `com.giathinh.uthconnect.data.repository.FirebaseAuthRepositoryTest` (100% Pass)
9. `com.giathinh.uthconnect.data.repository.PreferencesSessionRepositoryTest` (100% Pass)

---

## 4. Kết luận & Khuyến nghị nghiệm thu (Definition of Done Verification)

> [!TIP]
> **Đánh giá Definition of Done (DoD):**
> - [x] **Build & Test Pass:** 142/142 Unit tests pass 100%.
> - [x] **Lưu bằng chứng:** Đã ghi chép đầy đủ kết quả thực tế cho từng test case (TC-AUTH, TC-VERIFY, TC-RESET, TC-SEC).
> - [x] **Không còn lỗi chặn luồng (No Blocking Bugs):** Mọi luồng chính (Đăng nhập, Xác thực Email, Đặt lại MK, Đăng xuất) hoạt động mượt mà.
> - [x] **Git & PR Readiness:** Nhánh `feature/auth-phu` (Commit `2c2c90d`) đã sẵn sàng cho Code Review & Merge vào `develop`.
