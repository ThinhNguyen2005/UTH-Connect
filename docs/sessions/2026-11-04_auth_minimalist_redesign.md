# Báo cáo phiên làm việc: Tái thiết kế Giao diện Đăng nhập "Hello Again" & Luồng Tiếp tục thông minh

- **Ngày thực hiện:** 2026-11-04
- **Chủ đề:** Tái cấu trúc giao diện Auth Screen theo phong cách Modern Minimalist ("Hello Again!"), hỗ trợ luồng Next-Step thông minh (Identifier-first / Tiếp tục), tối ưu phân cấp thị giác và giảm tải nhận thức theo quy chuẩn UTH Design System.

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi

- **Bối cảnh:** Màn hình đăng nhập sinh viên trước đây có mật độ thông tin tương đối cao, các nút và ô nhập liệu mang phong cách form truyền thống. Người dùng đề xuất chuyển đổi sang ngôn ngữ thiết kế tối giản hiện đại (tham chiếu mẫu "Hello Again!" / Unified Auth), đồng thời tối ưu logic bước tiếp theo khi bấm "Tiếp tục" để giảm tối đa tải nhận thức cho sinh viên.
- **Mục tiêu:**
  1. Xây dựng giao diện "Hello Again!" thân thiện với các trường nhập liệu bo góc mềm (`UthDimens.Radius.LgShape` 16dp), icon con mắt bật/tắt mật khẩu trực quan, checkbox Ghi nhớ đăng nhập cân xứng với Quên mật khẩu.
  2. Nút bấm CTA chính bo tròn nổi bật, đạt độ tương phản chuẩn WCAG AAA trên nền sáng/tối.
  3. Cụm đăng nhập nhanh với Google SSO sử dụng chính xác vector nhận diện 4 màu Google.
  4. Hỗ trợ luồng `AuthScreenStep`:
     - **Bước 1 (Identifier):** Sinh viên chỉ cần nhập MSSV hoặc Email trường. Bấm "Tiếp tục" hệ thống tự nhận diện: nếu tài khoản đã kích hoạt -> chuyển sang bước nhập mật khẩu "Hello Again!"; nếu là tân sinh viên chưa kích hoạt -> chuyển sang bước hoàn thiện hồ sơ sinh viên (First-time setup).
     - **Bước 2 (Password):** Màn hình "Hello Again!" đầy đủ, mượt mà.
     - **Bước 3 (First-time setup):** Kích hoạt tài khoản sinh viên mới (Họ tên, Khoa/Ngành, Tạo mật khẩu).
  5. Đảm bảo toàn bộ 20 semantic test tags và 100% test suite hiện tại đều pass không có regression.

---

## 2. Kết quả đạt được & Chi tiết Git Diff

### 2.1. Danh sách file thay đổi
- `app/src/main/res/drawable/ic_google_logo.xml`: Vector drawable logo Google 4 màu chính hãng.
- `app/src/main/res/drawable/ic_auth_eye.xml`: Vector drawable con mắt hiển thị mật khẩu.
- `app/src/main/res/drawable/ic_auth_eye_off.xml`: Vector drawable con mắt gạch chéo ẩn mật khẩu.
- `app/src/main/res/values/strings.xml` & `app/src/main/res/values-en/strings.xml`: Bổ sung các chuỗi song ngữ cho tiêu đề chào mừng ("Hello Again!", "Chào bạn trở lại!"), divider ("hoặc tiếp tục với"), liên kết đăng ký, kích hoạt tài khoản mới.
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt`: Bổ sung `enum class AuthScreenStep`, các trường hỗ trợ tân sinh viên (`newStudentName`, `newStudentFaculty`, `confirmPassword`, `setupSuccessNotice`).
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt`: Bổ sung các action `ContinueIdentifier`, `SelectStep`, `NewStudentNameChanged`, `NewStudentFacultyChanged`, `ConfirmPasswordChanged`, `SubmitFirstTimeSetup`, `ClearSetupNotice`.
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: Xử lý phân luồng tài khoản kích hoạt vs tân sinh viên khi bấm "Tiếp tục", chuyển đổi step an toàn.
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`: Tái thiết kế toàn bộ Composable theo chuẩn giao diện mới, thêm Canvas background ambient, bo góc mềm mại, eye toggle button, giữ nguyên đầy đủ 20 test tags.

### 2.2. Kết quả kiểm thử
- `.\gradlew testDebugUnitTest`: **BUILD SUCCESSFUL**, 100% unit tests (Auth, Home, Events, Tickets, Profile, Utilities) vượt qua toàn diện.

---

## 3. Bài học & Kinh nghiệm rút ra

1. **Unidirectional Data Flow kết hợp AnimatedContent:** Việc đóng gói trạng thái bước vào `uiState.screenStep` giúp Compose hoán chuyển các bước (Identifier -> Password -> FirstTimeSetup) cực kỳ mượt mà bằng `AnimatedContent(transitionSpec = { fadeIn() togetherWith fadeOut() })` mà không làm thay đổi cấu trúc ViewModel cốt lõi.
2. **Bảo toàn Test Contracts:** Nhờ việc giữ nguyên các default values của `AuthUiState` và giữ nguyên 20 semantic test tags (`auth_identifier`, `auth_password`, `auth_sign_in`, `auth_google_sign_in`,...), các bài test E2E và Adversarial Stress Tests không bị gián đoạn.
3. **Tuân thủ Design System Tokens:** Mọi khoảng cách, bo góc và độ cao đều trích xuất từ `UthDimens` (`Spacing.Xl`, `Radius.LgShape`, `Size.ButtonHeight`), không có literal `.dp` tự do nào bị rò rỉ vào UI code.

---

## 4. Vấn đề tồn đọng / Kỹ thuật cần lưu ý

- Màn hình First-Time Setup hiện đang hoạt động ở mức độ Client/ViewModel simulation theo quy chuẩn demo; khi triển khai Backend chính thức với Supabase/Firebase Auth, luồng này sẽ kết nối với API đăng ký / kích hoạt thực tế của nhà trường.
- Nút Google SSO đã sẵn sàng liên kết với Credential Manager và Supabase OAuth (`BuildConfig.USE_SUPABASE`).

---

## 5. Kế hoạch cho các phiên tiếp theo

- Tiếp tục hoàn thiện trải nghiệm onboarding / hướng dẫn người dùng sau khi đăng nhập thành công lần đầu.
- Rà soát các tương tác animation / shared element transition giữa Auth và Home.
