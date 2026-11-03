<div align="center">

# UTH Connect

**Không gian số hỗ trợ học tập, sự kiện và đời sống sinh viên UTH.**

`Android · Kotlin · Jetpack Compose · Material 3`

[![Android](https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-31-informational?style=flat-square)](https://developer.android.com/about/versions/12)
[![Status](https://img.shields.io/badge/status-In%20Development-F5A623?style=flat-square)](#tính-năng)

</div>

---

UTH Connect tập trung các trải nghiệm dành cho cộng đồng UTH: sự kiện và câu lạc bộ, vé điện tử và check-in, thông tin học tập, hồ sơ sinh viên cùng các tiện ích trong trường.

## Tính năng

- **Tài khoản và hồ sơ sinh viên:** đăng nhập, thông tin cá nhân và phân quyền theo vai trò.
- **Sự kiện và câu lạc bộ:** khám phá hoạt động trong trường và quản lý vé tham dự.
- **QR check-in:** vé QR, quét bằng camera hoặc nhập mã, kết quả điểm danh cho ban tổ chức.
- **Thông tin học tập:** thời khóa biểu, học phí và điểm rèn luyện.
- **Tiện ích sinh viên:** thông báo, đồ thất lạc và các dịch vụ trong trường.

UTH Connect đang được phát triển theo từng giai đoạn. Phạm vi sản phẩm và thiết kế nghiệp vụ được trình bày trong [docs/Design.md](docs/Design.md).

## Công nghệ

- Kotlin, Jetpack Compose, Material 3
- Hilt, Navigation Compose với route type-safe, Coroutines và Flow
- CameraX và ML Kit để đọc QR; ZXing để tạo QR
- Firebase Authentication, Cloud Firestore và Firebase Storage
- JUnit cho unit test; Compose UI Test cho một số luồng giao diện

## Cấu trúc mã nguồn

Mã Android nằm trong `app/src/main/java/com/giathinh/uthconnect/` và chia thành các tầng:

```text
core/           Design system và thành phần dùng chung
data/           Repository, Firebase, dữ liệu mẫu và Hilt bindings
domain/         Model, repository interfaces và use cases
presentation/   Screen, Route, ViewModel, UiState và Action
```

Các feature chính hiện có gồm `auth`, `home`, `announcement`, `profile`, `tickets`, `scanner`, `schedule`, `utilities` và `lostfound`. Cấu trúc chi tiết và quyền sở hữu package được ghi trong [docs/REPO_TREE.md](docs/REPO_TREE.md).

## Yêu cầu phát triển

- Android Studio với Android SDK cần thiết theo `app/build.gradle.kts`
- JDK tương thích với Gradle wrapper của dự án
- Thiết bị hoặc emulator Android 12 trở lên
- Windows PowerShell hoặc macOS/Linux shell

## Build và kiểm thử

Trên Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-Pkotlin.compiler.execution.strategy=in-process'
```

Trên macOS/Linux:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest -Pkotlin.compiler.execution.strategy=in-process
```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## Chạy ứng dụng

Mở project trong Android Studio để đồng bộ Gradle và chạy ứng dụng trên emulator hoặc thiết bị Android 12 trở lên. Cài bản Debug bằng:

```powershell
.\gradlew.bat :app:installDebug
```

Thiết lập tài khoản, Firebase và các dịch vụ liên quan được hướng dẫn trong [docs/DATA_LAYER_GUIDE.md](docs/DATA_LAYER_GUIDE.md).

## Firebase

Chế độ Firebase được chọn bằng thuộc tính Gradle `useMockData`:

```powershell
.\gradlew.bat :app:assembleDebug -PuseMockData=false
```

Chế độ này cần cấu hình Firebase đúng `applicationId` `com.giathinh.uthconnect`, Firebase project và dịch vụ đã bật tương ứng. Không commit khóa riêng, tài khoản cá nhân hoặc cấu hình môi trường bí mật. Xem [docs/DATA_LAYER_GUIDE.md](docs/DATA_LAYER_GUIDE.md) để biết phạm vi kết nối hiện có; xem [firestore.rules](firestore.rules) và [firestore.indexes.json](firestore.indexes.json) trước khi làm việc với Firestore.

## Tài liệu dự án

- [Design và phạm vi sản phẩm](docs/Design.md)
- [Hướng dẫn data layer](docs/DATA_LAYER_GUIDE.md)
- [Firestore schema và indexes](docs/DATABASE_SCHEMA.md)
- [Coding standards](docs/CODING_STANDARDS.md)
- [Design system](docs/Design-system.md)
- [Cấu trúc repo và quyền sở hữu](docs/REPO_TREE.md)
- [Git workflow](docs/GIT_WORKFLOW.md)

## Đóng góp

Trước khi sửa code, đọc `docs/REPO_TREE.md`, `docs/CODING_STANDARDS.md` và `docs/GIT_WORKFLOW.md`. Giữ hướng phụ thuộc `presentation → domain ← data`; giao diện dùng resource song ngữ và token design system. Mỗi thay đổi cần có kiểm thử phù hợp, build thành công và được review qua pull request.
