# UTH Connect — Authentication & Profile

**Project Android Studio Kotlin + Jetpack Compose + Material 3** dành cho phần việc được phân công **Xác thực & Hồ sơ sinh viên**.

- **Tải project rồi Open bằng Android Studio** ở thư mục chứa `settings.gradle.kts`.
- **Tài khoản demo:** `2023040123` hoặc `demo@ut.edu.vn` / mật khẩu `UTH@123456`.
- **Luồng:** Login -> Trang chính -> Hồ sơ (Avatar, tên, MSSV, lớp, ngành/khoa) -> Logout.
- **Ghi nhớ phiên:** DataStore (chỉ giữ mã SV; không lưu mật khẩu).
- **Chạy:** `./gradlew :app:assembleDebug` (Windows: `.\gradlew.bat :app:assembleDebug`). Có fallback bootstrap Gradle 8.11.1 lần chạy đầu.

Chi tiết từng file, cách chạy, giới hạn demo và tài liệu báo cáo:

- [`docs/README_HUONG_DAN.md`](docs/README_HUONG_DAN.md)
- [`docs/HUONG_DAN_BAO_CAO.md`](docs/HUONG_DAN_BAO_CAO.md)
- [`docs/KIEM_THU.md`](docs/KIEM_THU.md)
- [`docs/HUONG_DAN_TICH_HOP_FIREBASE.md`](docs/HUONG_DAN_TICH_HOP_FIREBASE.md)
- [`docs/README_NGUON_UTH_CONNECT.md`](docs/README_NGUON_UTH_CONNECT.md) — README của nhóm cung cấp.

**Lưu ý:** Đăng nhập giả lập ngoại tuyến, không phải xác thực UTH/Firebase thật. Nút “Quên mật khẩu” hiện thông báo demo chứ không gửi email; điều này phù hợp phạm vi “giả lập hoặc qua API” trong ảnh phân công.
