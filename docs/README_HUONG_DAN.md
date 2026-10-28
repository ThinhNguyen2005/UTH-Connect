# UTH Connect — Authentication & Profile (Module đồ án)

## 1. Phạm vi được giao

Chỉ triển khai **Authentication & Profile** theo ảnh phân công; các phân hệ Sự kiện/CLB, QR, lịch học... ở bản README tổng là **ngoài phạm vi module này**.

### Task sáng

- `LoginScreen.kt`: UI nhập MSSV/email `@ut.edu.vn`, mật khẩu, ghi nhớ, quên mật khẩu.
- `LoginValidator.kt`: không để trống, MSSV 8–12 chữ số (quy ước demo), email đúng định dạng và domain trường.
- `LoginUseCase.kt`: kiểm tra dữ liệu rồi gọi Repository.
- `DemoAuthRepository.kt`: đối chiếu dữ liệu mẫu offline, không giả vờ là Firebase thật.

### Task chiều

- `ProfileScreen.kt`: avatar chữ cái đại diện, tên, MSSV, lớp, khoa, ngành, email, đăng xuất.
- `DataStoreSessionRepository.kt`: giữ lại **mã sinh viên**, không bao giờ lưu mật khẩu; chỉ lưu khi bật “Ghi nhớ”.
- `AuthViewModel.kt`: khôi phục phiên khi mở app; chuyển Login -> Home -> Profile; đăng xuất xóa phiên.

### Definition of Done theo ảnh

`Nhập MSSV/Email -> Đăng nhập -> Trang chính -> Xem Profile -> Đăng xuất`. Đã được nối logic trong mã nguồn.

## 2. Chạy với Android Studio trên Windows

1. Giải nén toàn bộ thư mục `UTH_Connect_Authentication_Profile`.
2. Android Studio -> **Open** -> chọn đúng thư mục chứa `settings.gradle.kts`.
3. Cài **Android SDK Platform 36** và **JDK 17 trở lên**; dùng JDK 17 là lựa chọn ổn định.
4. Internet cần thiết để Gradle tải dependency ở lần sync đầu tiên.
5. Chạy cấu hình `app` trên emulator Android 12+ hoặc điện thoại API 31+.
6. Nếu cần build tại Terminal/PowerShell: `.\gradlew.bat :app:assembleDebug` (xem dòng lệnh chính xác bên dưới).

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

> **Về Gradle wrapper:** Bản ZIP cung cấp `gradlew.bat`/`gradlew` với fallback script tải Gradle chính thức 8.11.1 lần đầu. Do môi trường đóng gói không có bộ wrapper JAR gốc nên bản này **không kèm `gradle-wrapper.jar` chuẩn**. Nếu máy đã có Gradle, chạy `gradle wrapper --gradle-version 8.11.1` tại gốc để tạo wrapper chuẩn. Fallback sử dụng HTTPS `services.gradle.org` và không ghi thông tin đăng nhập.

## 3. Tài khoản mẫu

| Loại | Giá trị |
|---|---|
| MSSV | `2023040123` |
| Email | `demo@ut.edu.vn` |
| Mật khẩu | `UTH@123456` |
| Tên sinh viên (giả lập) | Nguyễn Minh Anh |
| Lớp | CN23A |
| Ngành | Công nghệ thông tin |
| Khoa | Khoa Công nghệ thông tin |

Có thể sử dụng MSSV **hoặc** email trường cùng mật khẩu ở trên để đăng nhập. Đăng nhập thử với mật khẩu sai sẽ nhận thông báo lỗi.

**Lưu ý:** Đây là tài khoản giả lập. Không gửi email, không gọi Firebase và không xác minh sinh viên thật với UTH.

## 4. Bố cục project

```text
UTH_Connect_Authentication_Profile/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/giathinh/uthconnect/
│       │   ├── MainActivity.kt                   # Entry point
│       │   ├── ui/UthConnectApp.kt               # Chuyển 3 màn hình
│       │   ├── ui/theme/Color.kt, Theme.kt       # Màu + Material 3
│       │   ├── core/designsystem/UthDimens.kt    # Spacing tokens
│       │   ├── domain/model/StudentProfile.kt    # Model hồ sơ
│       │   ├── domain/repository/*.kt             # Interface repository
│       │   ├── domain/validation/LoginValidator.kt
│       │   ├── domain/usecase/LoginUseCase.kt
│       │   ├── data/auth/DemoAuthRepository.kt
│       │   ├── data/session/DataStoreSessionRepository.kt
│       │   ├── di/AuthViewModelFactory.kt
│       │   ├── feature/auth/AuthUiState.kt
│       │   ├── feature/auth/AuthViewModel.kt
│       │   ├── feature/auth/ui/LoginScreen.kt
│       │   ├── feature/home/ui/HomeScreen.kt
│       │   └── feature/profile/ui/ProfileScreen.kt, StudentAvatar.kt
│       ├── main/res/values{,-en}/strings.xml
│       ├── test/.../LoginValidatorTest.kt, LoginUseCaseTest.kt
│       └── androidTest/.../LoginScreenTest.kt, ProfileScreenTest.kt
├── docs/
│   ├── README_NGUON_UTH_CONNECT.md
│   ├── HUONG_DAN_BAO_CAO.md
│   ├── KIEM_THU.md
│   └── HUONG_DAN_TICH_HOP_FIREBASE.md
└── README.md
```

## 5. Môi trường và tính tương thích

- Kotlin **2.2.10**, Compose BOM **2026.02.01**, Material3, `compileSdk/targetSdk = 36`, `minSdk = 31`, namespace **`com.giathinh.uthconnect`**.
- **AGP 8.10.1 + Gradle 8.11.1** thay vì tham chiếu AGP 9.3.3 trong README gốc để ghép với Kotlin plugin 2.2.10 theo dải tương thích chính thức. Khi nhập code vào dự án nhóm AGP 9.x, nên giữ build settings theo dự án nhóm và chỉ copy package code.
- `dynamicColor` không dùng, giữ màu thương hiệu #4F46E5 từ README gốc.
- DataStore cho nhớ phiên demo; tránh lưu password.
- Chế độ DI hiện tại sử dụng constructor injection và Factory vì module đồ án nhỏ; Hilt nằm trong thiết kế **đích** của dự án tổng và có thể được tích hợp sau.
- `Design.md` và `Design-system.md` được nhắc tới ở README gốc nhưng **không được tải lên**, do đó không thể kiểm chứng mọi token/luồng xác thực từ 2 file đó.

## 6. Đúng/không đúng với sản phẩm hoàn chỉnh

**Có:** UI đăng nhập, kiểm tra định dạng, thông báo sai tài khoản, nút hiển thị mật khẩu, ghi nhớ phiên, luồng trang chính, trang hồ sơ, đăng xuất, tài khoản demo, strings Việt/Anh, light/dark, Preview và test mã nguồn.

**Chưa có:** đăng nhập Firebase/Auth API thật, email xác minh `uthVerified` và custom claims, mật khẩu reset qua email, avatar từ URL thật, dữ liệu Firestore, bảo mật phía máy chủ. Không dùng demo này làm hệ thống quản lý sinh viên thực tế.

## 7. Hướng mở rộng

Thay `DemoAuthRepository` bằng `FirebaseAuthRepository` implement `AuthRepository` và thêm bước xác minh sinh viên qua backend. `ProfileScreen`, `LoginUseCase`, `AuthViewModel` có thể được giữ lại với điều chỉnh trạng thái lỗi tương ứng. Không lưu thông tin mật khẩu và không tự gán custom claims trên client.
