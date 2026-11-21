# Phiên 2026-11-21 — Sửa lỗi BUG-01, UI/UX Login, Bảo mật & Access Control (Android & Web)

- **Ngày báo cáo:** 21/11/2026
- **Nhánh làm việc:** `fix/auth-qa-2026-11-21`
- **Người thực hiện:** `ThinhNguyen2005` & AI Pair Programmer
- **Phạm vi:** Sửa lỗi theo báo cáo kiểm thử ngày 18/11/2026 cho module xác thực Android và Web Portal + Access Control.

---

## 1. Bối cảnh & Tổng quan thay đổi (Commit gần đây)

### Commit thuộc đợt fix này (21/11/2026)
1. **`a9bb18b8`** — `fix(android-auth): fix BUG-01 verify email state, race condition, and login contrast` *(8 files, +416/-108)*:
   - **Sửa BUG-01 (báo đã gửi link kích hoạt giả):** Tách `verifyEmailSent` và `isSendingVerifyEmail` trong `AuthUiState.kt`; kiểm tra `identifier` qua `LoginValidator` trong `openVerifyEmail()` của `AuthViewModel.kt` trước khi cho phép chuyển sang `VerifyEmail` (giữ ở màn đăng nhập và báo lỗi inline khi form trống hoặc sai định dạng).
   - **Chống race condition & lệch cooldown:** Gắn mỗi lần gửi với `verifyEmailGeneration` và `resendJob` qua `invalidateVerifyEmailRequests()`, hủy `cooldownJob` và reset `verifyEmailCooldownSeconds = 0` khi `IdentifierChanged`, `SwitchAccount`, hoặc đăng xuất (`justLoggedOut`). Giữ nguyên `verifyEmailSent` khi mở lại đúng email đang có cooldown.
   - **Xử lý lỗi mạng & cấu trúc code:** Tách `sendVerificationRequest()` và `applyVerificationError()`, ánh xạ `IOException` → `AuthFailure.NetworkError`, HTTP `429` → `AuthFailure.RateLimited`, các lỗi khác → `AuthFailure.Unexpected`; cập nhật `ModernVerifyEmailContent` trong `AuthScreen.kt` để hiển thị trạng thái chờ gửi, ẩn nút mở ứng dụng email khi chưa gửi và render `uiState.failure`.
   - **Đồng bộ UI/UX & ngôn ngữ:** Đồng bộ `auth_identifier_label` giữa `values/strings.xml` (*MSSV (8–12 số) hoặc email trường*) và `values-en/strings.xml` (*Student ID (8–12 digits) or university email*); chuyển màu chữ/icon trong `AuthSecurityFooter` sang `extendedColors.textSecondary` (`#475569` light / `#94A3B8` dark), bỏ `.alpha(0.45f)` và xóa `Opacity.AuthFooter` khỏi `Dimens.kt`.
   - **Format & Test:** Sắp xếp lại import trong `AppAvailability.kt` đạt chuẩn Spotless; bổ sung 8 unit test mới trong `AuthViewModelTest.kt` phủ nhánh form trống, sai định dạng, MSSV/email hợp lệ, giữ trạng thái khi mở lại đúng email và hủy cooldown/request khi đổi tài khoản.

2. **`b82b0e53`** — `fix(web-auth): enforce fail-closed access control and fix login/verify-email flows` *(5 files, +126/-57)*:
   - **Access Control fail-closed (`web/src/providers/`):** Chuyển nhánh fallback của `accessControlProvider.can` sang `{ can: false }`; `authProvider.getPermissions()` trả `null` khi chưa có session; bọc nhánh quyền sinh viên trong `if (role === "student")` để `null` hoặc role lạ bị từ chối mặc định; thống nhất thứ tự ưu tiên đọc role (`app_metadata.role` → `profiles.role` → `"student"`) giữa `getPermissions()` và `getIdentity()`.
   - **Khôi phục luồng Login (`web/src/app/login/page.tsx`):** Khôi phục xử lý `#type=recovery`, `?verified=true`, fallback `?error=...` (kèm `otp_expired`, `error_description`) và `<Link href="/forgot-password">`.
   - **Sửa trang Verify Email (`web/src/app/verify-email/page.tsx`, `register/page.tsx`, `auth-provider.ts`):** Chỉ khởi tạo trạng thái đã gửi (`sends = 1, left = 30`) khi có `?sent=1` từ luồng đăng ký; khi vào trực tiếp từ `/login`, hiển thị form nhập email/MSSV chưa gửi; render `errorMessage` trong JSX (`role="alert"`) và chỉ tăng `sends` + chạy cooldown sau khi `resend` thành công.

### Mạch commit gần trước đó trên nhánh gốc (14/11 – 18/11)
- **Base local (14/11):** `79852df7` (PKCE middleware & đồng bộ contract housing/notifications), `3dc9ce24` (bổ sung security guards từ review CodeRabbit), `7360df7d` (dọn repo demo/fake cũ), `59fca2ec` (deep link đặt lại mật khẩu), `9e56eca3` (PR #71 tích hợp Coil 3 & nén WebP `[KAN-108]`), `af6e4f9a` (brand UTH & login Web sạch không quick-fill), `fe50e1cc` (Detekt CI workflow).
- **Trên `origin/main` (17/11 – 18/11):** `1d3f7515` (Merge PR #76 `feature/housing-duc-v2`), `5e57f9d4` (Merge PR #91 `feature/notifications-inbox-ui`).

---

## 2. Kết quả & Trạng thái kiểm thử / Static Analysis

- `.\gradlew detekt spotlessCheck`: **PASS** (exit code 0, sau khi sửa cuối cùng trên Android).
- `.\gradlew testDebugUnitTest --tests "com.giathinh.uthconnect.presentation.auth.*" --tests "com.giathinh.uthconnect.AppAvailabilityTest"`: **PASS (77/77)** — `AuthViewModelTest`: 21, `AuthE2EWorkflowTest`: 17, `AuthAdversarialStressTest`: 11, `AppAvailabilityTest`: 28 (0 fail, 0 error, 0 skip).
- Web (`web/`): `npx tsc --noEmit` **PASS** (exit code 0); `npm run lint` **PASS** (exit code 0, còn 1 warning sẵn có tại `src/components/layout/Header.tsx:65`).
- CodeRabbit CLI (`coderabbit review --agent --uncommitted`): đã rà soát và xử lý các finding `major`/`minor` thuộc phạm vi (race condition đổi tài khoản, giữ trạng thái mở lại `VerifyEmail`, hồi quy `login/page.tsx`).

---

## 3. Bài học rút ra

- Không hiển thị thông điệp "đã gửi liên kết" khi chưa nhận phản hồi 2xx từ backend; cần tách rõ trạng thái chờ xác nhận và trạng thái đã gửi trên cả Android lẫn Web.
- Các tác vụ bất đồng bộ gắn với tài khoản (gửi email kích hoạt + bộ đếm cooldown) cần generation token và hủy job cũ ngay khi đổi `identifier`, đổi tài khoản hoặc đăng xuất để tránh ghi đè chéo trạng thái.
- Access Control Provider cần fail-closed với cả trường hợp chưa đăng nhập (`null`) và role/resource chưa khai báo, thay vì ép mặc định về `"student"` hoặc `{ can: true }`.

---

## 4. Nợ kỹ thuật / Vấn đề còn tồn đọng

- **Bảo mật RLS (chưa sửa trong phạm vi, chờ chốt spec 4 role):**
  - `public.notifications` (`supabase/migrations/20261107000000_auth_rbac_and_housing.sql`, `20261114010000_sync_housing_and_notifications_contract.sql`) cho phép `is_admin() or is_organizer()` sửa/xóa mọi thông báo nhưng chưa có cột/policy ownership `created_by`.
  - `housing_posts_update` (`20261108000000_housing_schema_rebuild.sql`) chỉ cho `author_id = auth.uid()` nên `AdminModerationPage` (`web/src/app/admin/moderation/page.tsx`) có thể cập nhật 0 dòng mà báo thành công giả.
- **Test coverage:** `AuthViewModel` gọi trực tiếp `SupabaseRestClient` nên chưa có unit test mock cho các nhánh 2xx / `IOException` / HTTP 429 / 5xx của `resendVerificationEmail()`.
- **Đồng bộ nhánh:** `fix/auth-qa-2026-11-21` được tạo từ local `main` (chậm 97 commit so với `origin/main`), có 3 file trùng vùng sửa trên `origin/main` là `Dimens.kt`, `values/strings.xml`, `values-en/strings.xml`.
- **Working tree ngoài phạm vi (chưa commit):** `.idea/appInsightsSettings.xml`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, cùng xóa `Bao-cao-KAN-103.docx` và `create_kan103_report.py`.

---

## 5. Kế hoạch tiếp theo

1. Rebase hoặc merge `origin/main` vào nhánh `fix/auth-qa-2026-11-21` (giải quyết xung đột nếu có ở `Dimens.kt` và `strings.xml`) trước khi mở/merge PR.
2. Build APK và verify thủ công BUG-01 trên emulator (form trống → nhập sai → nhập MSSV hợp lệ khi tắt mạng → bật mạng gửi thật) cùng luồng `/login`, `/verify-email` trên Web.
3. Tạo ticket riêng xử lý 2 vấn đề RLS (`notifications` ownership và quyền admin kiểm duyệt `housing_posts`).
