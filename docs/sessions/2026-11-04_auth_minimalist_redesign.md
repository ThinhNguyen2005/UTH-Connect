# Báo cáo phiên làm việc: Tái thiết kế Giao diện Đăng nhập "Hello Again" & Luồng Tiếp tục thông minh

- **Ngày thực hiện:** 2026-11-04 (Cập nhật lúc 00:23 ngày 2026-11-05)
- **Chủ đề:** Tái cấu trúc giao diện Auth Screen theo phong cách Modern Minimalist ("Hello Again!"), xử lý triệt để lỗi Status Bar Inset, loại bỏ Card lồng khung (Box-in-box), tinh gọn Label và tối ưu phân cấp thị giác theo chuẩn UTH Design System & WCAG AA.

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi

- **Vấn đề phát hiện khi chạy trên thiết bị thật:**
  1. **Va chạm Status Bar:** Khi `enableEdgeToEdge()` được kích hoạt tại `MainActivity`, `AuthScreen` thiếu `statusBarsPadding()`, dẫn đến thanh trạng thái hệ thống (đồng hồ, wifi, pin, 5G) đè trực tiếp lên logo thương hiệu và cụm chuyển ngôn ngữ.
  2. **Bẫy hộp lồng hộp (Nested Card Trap):** Việc bọc toàn bộ form đăng nhập trong một `UthCard` có viền đậm màu trên nền tối tạo cảm giác nặng nề, bí bách, trông như một biểu mẫu hành chính thời cũ thay vì một ứng dụng di động hiện đại.
  3. **Label quá dài bị ngắt dòng:** Nhãn trường `MSSV hoặc Email sinh viên (@ut.edu.vn / @uth.edu.vn)` bị rớt thành 2 dòng bên trong TextField, làm vỡ tỉ lệ chiều cao và gây rối mắt.
  4. **Thanh Segmented Control quá khổ:** Thanh chọn "1. Nhập mật khẩu / 2. Sinh trắc học" chiếm tới 48dp ngay dưới header, tranh chấp điểm nhấn thị giác với tiêu đề chính "Chào bạn trở lại!".
  5. **Footer rườm rà:** Khối thông tin mã hóa SSL và số hiệu phiên bản quá nổi bật, chiếm dụng không gian dọc đáy màn hình.

- **Giải pháp xử lý:**
  1. Thêm `statusBarsPadding()`, `navigationBarsPadding()`, và `imePadding()` đảm bảo hiển thị an toàn tuyệt đối trên mọi loại màn hình (tai thỏ, nốt ruồi, viền mỏng).
  2. Xóa bỏ hoàn toàn lớp `UthCard` bọc ngoài trong `ModernPasswordContent`. Cho phép các trường nhập liệu nổi trực tiếp trên nền với bo góc 16dp (`UthDimens.Radius.LgShape`) và viền mờ tinh tế.
  3. Rút gọn nhãn trường nhập liệu thành 1 dòng duy nhất: `MSSV hoặc Email sinh viên`.
  4. Thu nhỏ thanh chuyển chế độ đăng nhập thành một Pill thanh mảnh (`LanguagePillHeight = 36dp`), bo cong mềm mại.
  5. Giảm độ mờ (`alpha = 0.45f`) và thu nhỏ font của Footer bảo mật để không phân tán sự chú ý của sinh viên.
  6. Xử lý triệt để tính đầy đủ (exhaustive `when`) cho tất cả các nhánh `AuthScreenStep` (bao gồm `ResetPassword`, `VerifyEmail`) và `AuthFailure`.

---

## 2. Kết quả đạt được & Chi tiết Git Diff

### 2.1. Danh sách file thay đổi
- `app/src/main/res/values/strings.xml` & `app/src/main/res/values-en/strings.xml`: Rút gọn nhãn trường đăng nhập thành 1 dòng, bổ sung `auth_brand_monogram` và `auth_verify_email_default_mailbox`, xóa 8 chuỗi sinh trắc học dư thừa của Auth (giữ nguyên chuỗi Profile).
- `app/src/main/res/drawable/ic_google_logo.xml`: Vector drawable logo Google 4 màu chính hãng.
- `app/src/main/res/drawable/ic_auth_eye.xml` & `ic_auth_eye_off.xml`: Vector drawable con mắt bật/tắt mật khẩu.
- `app/src/main/java/com/giathinh/uthconnect/core/designsystem/theme/Dimens.kt`: Bổ sung các tokens Opacity (`AuthAmbientShape`, `AuthFooter`, `AuthOutlineButtonBorder`, `AuthInputUnfocusedBorder`, `AuthSuccessBannerBorder`, `AuthInfoBannerBorder`, `AuthDivider`) và Layout (`AuthAmbientRadiusFraction`, `AuthAmbientCenterFractionX`, `AuthAmbientCenterFractionY`). Dọn sạch các tokens kích thước sinh trắc học lỗi thời.
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`: Tái cấu trúc toàn bộ giao diện:
  - Thêm Insets xử lý status bar, navigation bar & IME (`.statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(...)`).
  - Khôi phục Artistic Ambient Canvas background (`drawCircle`) ở góc trên bên phải.
  - Bỏ `UthCard` bọc ngoài, cho input fields tự do với bo góc 16dp (`UthDimens.Radius.LgShape`).
  - Gỡ bỏ triệt để toàn bộ thành phần Biometric (AuthModePillSwitcher, BiometricModeContent, PreviewAuthLightBiometric).
  - Chuẩn hóa 100% token hóa: 0 hardcoded dp, 0 sp, 0 màu hex, 0 chuỗi giao diện hardcode.
  - Chuẩn hóa bộ semantic test tags từ 20 xuống 15 tags tiêu chuẩn.
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`, `AuthUiState.kt`, `AuthAction.kt`, `AuthRoute.kt`: Loại bỏ toàn bộ `AuthMode`, `AuthAction.SelectMode`, `AuthAction.RequestBiometric`, `AuthAction.BiometricVerified`, `AuthAction.BiometricFailed`, `AuthFailure.BiometricUnavailable`, `biometricUnlock()`, `showLoginBiometric()`.
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/AccessStudentSessionUseCase.kt` & `SessionRepository.kt`: Loại bỏ `unlockAfterBiometricPrompt()` khỏi UseCase, giữ default implementation `suspend fun unlockWithBiometric() = Unit` an toàn trên `SessionRepository`.
- `app/src/test/.../presentation/auth/` (`AuthViewModelTest`, `AuthE2EWorkflowTest`, `AuthAdversarialStressTest`) & `app/src/androidTest/.../AuthScreenUiTest.kt`: Tái cấu trúc các kịch bản kiểm thử, kiểm tra chính xác 15 semantic test tags và kiểm tra phủ định sự vắng mặt của các thẻ sinh trắc học.

### 2.2. Kết quả kiểm thử
- `.\gradlew testDebugUnitTest`: **BUILD SUCCESSFUL in 27s**, 168/168 tests (27 test classes) vượt qua toàn diện với 0 lỗi, 0 thất bại.

---

## 3. Bài học & Kinh nghiệm rút ra

1. **Edge-to-Edge Insets là bắt buộc:** Khi bật `enableEdgeToEdge()` ở cấp độ Activity, Composable cấp cao nhất không dùng `Scaffold` bắt buộc phải áp dụng `statusBarsPadding()` và `navigationBarsPadding()` để tránh va đập giao diện với system bars.
2. **Loại bỏ "Box-in-Box":** Trong UI hiện đại, việc sử dụng các thẻ Card có viền lồng bên trong một Card khác dễ tạo ra sự ngột ngạt. Để các phần tử tương tác thở trực tiếp trên canvas sẽ mang lại cảm giác thoáng đãng, cao cấp hơn rất nhiều.
3. **Phân định rõ ranh giới nghiệp vụ (Module Seams):** Tính năng sinh trắc học có hai phạm vi: mở khóa lúc đăng nhập (Auth) và bật/tắt trong cài đặt (Profile). Khi loại bỏ sinh trắc học khỏi Auth, việc giữ phương thức mặc định trên `SessionRepository` và giữ các string của Profile đảm bảo tách bạch rõ ràng, không gây sụp đổ lan truyền sang các test doubles khác.
4. **Tokenization tuyệt đối:** Việc token hóa toàn bộ các thông số hình học Canvas (`Layout.AuthAmbient*`), độ mờ (`Opacity.Auth*`) và chuỗi resource giúp UI Compose đạt độ bền vững tối đa, sẵn sàng cho Dark Mode và kiểm thử tự động.

---

## 4. Kế hoạch cho các phiên tiếp theo

- Kiểm tra trực quan trên Android Emulator / Physical Device để xác nhận độ hoàn hảo của giao diện ở cả Light Mode và Dark Mode.
- Kiểm tra luồng Reset Password và Deep Link khi kết nối với Supabase GoTrue.
- Chuẩn bị cho Milestone M2 tiếp theo theo lộ trình dự án.
