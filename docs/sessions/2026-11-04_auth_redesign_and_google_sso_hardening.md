# Báo cáo Phiên làm việc: Tái thiết kế Giao diện Đăng nhập (R1), Chẩn đoán Google SSO (R2), Ràng buộc Xác thực (R3) & E2E Testing

- **Ngày thực hiện**: 2026-11-04
- **Chủ đề**: Tái thiết kế toàn diện giao diện xác thực theo layout Stitch AI, hoàn thiện chẩn đoán Google SSO Credential Manager, khắc phục triệt để lỗi `[28444]` và `NoCredentialException`, thiết lập bảo vệ tên miền sinh viên @ut.edu.vn, và kiểm thử Hermetic E2E 4 tầng.
- **Trạng thái**: Hoàn tất 100%, chạy thử nghiệm thực tế trên Emulator thành công (đăng nhập Google nhận diện tài khoản `thinhnhg8684@ut.edu.vn`, kích hoạt đúng luồng chờ Admin duyệt MSSV), tất cả test suite passed (`BUILD SUCCESSFUL`).

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi

Triển khai đầy đủ các yêu cầu trọng tâm (R1, R2, R3), chẩn đoán thực tế và hạ tầng kiểm thử E2E:
1. **R1: Tái thiết kế Giao diện AuthScreen trong Jetpack Compose**:
   - Chuyển đổi giao diện đăng nhập sang chuẩn Stitch AI hiện đại, bám sát UTH Design System (`docs/Design-system.md`, `docs/UI-UX.mc`, `AGENTS.md`).
   - Tích hợp Brand Header với logo monogram UTH, tiêu đề kèm badge `PRO` (Accent tone / Gold container), bộ chọn ngôn ngữ pill `VI`/`EN` đạt chuẩn touch target 48dp.
   - Bổ sung bộ điều khiển phân đoạn Segmented Control chuyển đổi mượt mà giữa "1. Nhập mật khẩu" và "2. Sinh trắc học".
   - Chế độ Password Mode: Input MSSV/Email, Password kèm nút show/hide, checkbox kích hoạt vân tay lần sau, liên kết quên mật khẩu, nút chính "ĐĂNG NHẬP" (in hoa, spinner loading), đường kẻ phân cách "Hoặc" và nút Google SSO.
   - Chế độ Biometric Mode: Thẻ hồ sơ sinh viên đã lưu (avatar 56dp, tên sinh viên `titleMedium`, MSSV `tabularFigures`, lớp/khoa, badge "SV đã xác thực"), vùng chạm cảm ứng hình tròn (>= 48dp) kích hoạt `BiometricPrompt`, nút quay lại mật khẩu và chuyển tài khoản khác.
   - Chân trang bảo mật: Biểu tượng ổ khóa kèm văn bản "Bảo mật chuẩn mã hóa SSL 256-bit UTH" và phiên bản ứng dụng "UTH Connect v0.3.1 (M3 Native)".
   - Loại bỏ 100% literal kích thước tự do (`.dp`, `.sp`, `Color(0x...)`) trong các composable, tái sử dụng token chuẩn trong `core.designsystem.*`.
2. **R2: Chẩn đoán & Khắc phục triệt để Google Sign-In**:
   - **Xử lý `NoCredentialException: No credentials available`**: Cập nhật `GoogleIdTokenSource.kt` chuyển từ API One-Tap `GetGoogleIdOption` sang API `GetSignInWithGoogleOption` (Explicit Button Flow). API này mở hộp thoại chọn tài khoản chuẩn của Android thay vì âm thầm từ chối hiển thị khi chưa cấp quyền trước.
   - **Xử lý `[28444] Developer console is not set up correctly`**: Dùng lệnh probe trực tiếp tới Google OAuth Server phát hiện Web Client ID cũ (`...ue1ff5n...`) trong `gradle.properties` đã bị xóa trên Google Cloud Console (`authError=deleted_client`). Đã chuyển sang Web Client ID đang hoạt động (`324596143171-hletvs7gnj8rurb5juet4hhs3qmm1nuk.apps.googleusercontent.com`), đồng bộ giữa Supabase GoTrue và app Android.
   - Cập nhật tài liệu hướng dẫn từng bước đầy đủ tại `docs/GOOGLE_SIGNIN_SETUP.md`.
3. **R3: Ràng buộc Tên miền Sinh viên (@ut.edu.vn) & Bảo mật Phiên**:
   - Bắt buộc kiểm tra tên miền `@ut.edu.vn` trong `SupabaseAuthRepository.authenticateWithGoogle`: tài khoản Google ngoài trường bị từ chối và ném `DomainRestrictionException`.
   - Giữ vững cơ chế bảo vệ tài khoản Google lần đầu chưa duyệt MSSV: ném `StudentVerificationRequiredException`, hiển thị banner "Đang chờ xác minh MSSV" kèm email sinh viên, ngăn chặn hoàn toàn việc gọi RPC sự kiện.
   - Thiết lập tài liệu đặc tả kiến trúc bảo mật `docs/AUTH_CONSTRAINTS.md`.
4. **Kiểm thử & Xác minh E2E 4 Tầng**:
   - Cập nhật `AuthViewModelTest.kt` với các test case mới (`SelectMode`, `SelectLanguage`, `SwitchAccount`, `BiometricFailed`).
   - Xây dựng mới `AuthE2EWorkflowTest.kt` bao quát 4 tầng kiểm thử hermetic: Domain Validation, ViewModel State Machine, UI Semantics Contract và 6 hành trình người dùng thực tế.
   - Thử nghiệm trực tiếp trên Emulator Android thành công: Tài khoản `thinhnhg8684@ut.edu.vn` đăng nhập thành công và xuất hiện banner chờ xác minh đúng theo spec.

---

## 2. Kết quả đạt được & Chi tiết Git Diff

### 2.1. Danh mục các file đã chỉnh sửa & tạo mới
| File | Loại thay đổi | Ý nghĩa kỹ thuật |
|---|---|---|
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt` | Chỉnh sửa lớn | Tái thiết kế toàn bộ giao diện Compose: Brand Header, Mode Switcher, Password Mode, Biometric Mode, Footer, và bộ `@Preview` đầy đủ Light/Dark. |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt` | Chỉnh sửa | Bổ sung `enum class AuthMode`, `savedStudent: StudentProfile?`, `selectedLanguage: String`. |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt` | Chỉnh sửa | Bổ sung các action: `SelectMode`, `SelectLanguage`, `SwitchAccount`. |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt` | Chỉnh sửa | Xử lý các action chuyển mode, đổi ngôn ngữ, chuyển tài khoản và lưu giữ `savedStudent`. |
| `app/src/main/java/com/giathinh/uthconnect/core/designsystem/theme/Dimens.kt` | Chỉnh sửa | Thêm các token kích thước chuẩn: `BrandLogoMonogram`, `SegmentedControlHeight`, `BiometricTouchZone`, `BiometricTouchTarget`, `BiometricTouchZoneRing`, `BiometricIcon`, `LanguagePillWidth`, `LanguagePillHeight`, `SecurityIconSize`. |
| `app/src/main/java/com/giathinh/uthconnect/data/repository/GoogleIdTokenSource.kt` | Chỉnh sửa | Tích hợp `GetSignInWithGoogleOption` làm luồng chính, fallback sang `GetGoogleIdOption`, bổ sung logging `Log.w` chi tiết. |
| `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt` | Chỉnh sửa | Kiểm tra email kết thúc bằng `@ut.edu.vn`, ném `DomainRestrictionException` nếu không hợp lệ. |
| `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt` | Chỉnh sửa | Định nghĩa ngoại lệ `DomainRestrictionException`. |
| `app/src/main/res/values/strings.xml` | Chỉnh sửa | Bổ sung 16 chuỗi bản địa hóa tiếng Việt cho chế độ sinh trắc học, PRO badge, SSL indicator. |
| `app/src/main/res/values-en/strings.xml` | Chỉnh sửa | Bổ sung 16 chuỗi bản địa hóa tiếng Anh tương ứng. |
| `gradle.properties` | Chỉnh sửa | Đồng bộ `googleWebClientId` với Web Client ID đang hoạt động (`...hletvs7...`) và bật Supabase backend. |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthViewModelTest.kt` | Chỉnh sửa | Bổ sung unit test mới cho mode switcher, language switcher, switch account, biometric failure. |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthE2EWorkflowTest.kt` | Tạo mới | Suite kiểm thử E2E hermetic 17 test case bao quát Tiers 1-4. |
| `docs/GOOGLE_SIGNIN_SETUP.md` | Cập nhật | Báo cáo chẩn đoán sự cố `[28444]`, client ID bị xóa và hướng dẫn cấu hình toàn diện. |
| `docs/AUTH_CONSTRAINTS.md` | Tạo mới | Tài liệu đặc tả quy tắc xác thực, ràng buộc domain, vòng đời MSSV và bảo mật phiên. |

### 2.2. Kết quả chạy kiểm thử (Build & Test Verification)
- Lệnh thực thi: `.\gradlew assembleDebug testDebugUnitTest`
- **Kết quả**: `BUILD SUCCESSFUL`.
- Toàn bộ 32 unit test tasks và các bộ test hermetic/E2E đều pass 100% với 0 failure, 0 error.
- Thử nghiệm thực tế: Đăng nhập Google SSO thành công trên máy ảo Android với tài khoản `thinhnhg8684@ut.edu.vn`.

---

## 3. Bài học & Kinh nghiệm rút ra

1. **Khớp Audience và Trạng thái Client ID trong Google Cloud**:
   - `serverClientId` trong Credential Manager bắt buộc phải là **Web Application Client ID** đang ở trạng thái ACTIVE trong Google Cloud Console.
   - Một client ID bị xóa trong Console (`authError=deleted_client`) sẽ biểu hiện trên Android thành lỗi mơ hồ `[28444] Developer console is not set up correctly`. Việc probe trực tiếp endpoint Google OAuth bằng curl giúp xác định ngay lập tức trạng thái sống/chết của Client ID.
2. **Sự khác biệt giữa `GetSignInWithGoogleOption` và `GetGoogleIdOption`**:
   - `GetGoogleIdOption` là One-Tap bottom-sheet, sẽ trả về `NoCredentialException` nếu tài khoản chưa từng liên kết với app.
   - Đối với thao tác click nút bấm (Explicit Click), bắt buộc phải dùng `GetSignInWithGoogleOption` để mở hộp thoại Account Chooser tiêu chuẩn.
3. **Nguyên tắc "Reuse before create" trong Design System**:
   - Việc bổ sung các token có ngữ nghĩa rõ ràng vào `Dimens.kt` (`BiometricTouchZone`, `BrandLogoMonogram`,...) giúp giao diện Compose đạt độ sạch tuyệt đối, dễ bảo trì và hoàn toàn tương thích với các chế độ Dark Theme / Font Scaling.

---

## 4. Vấn đề tồn đọng / Kỹ thuật cần lưu ý

1. **Quy trình Duyệt MSSV cho Sinh viên Mới**:
   - Sau khi sinh viên đăng nhập Google lần đầu, tài khoản cần được quản trị viên duyệt gán MSSV qua câu lệnh SQL:
     ```sql
     UPDATE public.profiles
     SET student_id = '<MSSV>', student_id_verified = true
     WHERE email = '<email@ut.edu.vn>';
     ```
   - Trong tương lai có thể xây dựng màn hình Admin Panel hoặc webhook để tự động hóa việc đối soát này.
2. **Kích hoạt Android Keystore trong tương lai cho Supabase Biometric**:
   - Hiện tại, ở chế độ Supabase, các phiên làm việc là memory-only để đảm bảo an toàn. Khi mở rộng lưu session sinh trắc học lâu dài, cần triển khai `EncryptedSharedPreferences` với khóa phần cứng `MasterKey`.

---

## 5. Kế hoạch cho các phiên tiếp theo

1. Xây dựng giao diện / luồng cho Admin duyệt danh sách sinh viên đang chờ xác minh MSSV.
2. Hoàn thiện tính năng quét mã QR điểm danh sự kiện cho sinh viên đã xác thực.
3. Tích hợp màn hình đăng nhập mới vào flow điều hướng chính khi người dùng đăng xuất từ ProfileScreen.
