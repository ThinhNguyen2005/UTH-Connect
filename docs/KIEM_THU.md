# Kịch bản kiểm thử để nghiệm thu

> Bảng này là **test plan**. Không được coi là kết quả chạy trên thiết bị khi chưa thực sự build/execute.

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | Để trống MSSV/email và mật khẩu rồi login | Hai trường báo bắt buộc |
| TC02 | Nhập `abc` + mật khẩu không rỗng | Báo MSSV/email sai định dạng |
| TC03 | Nhập `demo@gmail.com` | Không nhận email ngoài `@ut.edu.vn` |
| TC04 | Nhập `demo@ut.edu.vn`, mật khẩu sai | Báo thông tin đăng nhập không đúng |
| TC05 | Nhập `2023040123` + `UTH@123456` | Vào Trang chính |
| TC06 | Nhập `DEMO@UT.EDU.VN` + `UTH@123456` | Vào Trang chính (không phân biệt hoa/thường) |
| TC07 | Từ Trang chính nhấn Xem hồ sơ | Thấy avatar, tên, MSSV, lớp, ngành, khoa |
| TC08 | Profile nhấn Đăng xuất | Về Login, xóa phiên nhớ |
| TC09 | Bật Ghi nhớ, đăng nhập, force-stop, mở lại | Vẫn đăng nhập (nếu app data còn) |
| TC10 | Không bật Ghi nhớ, đăng nhập, kill process, mở lại | Về Login |
| TC11 | Nhấn Quên mật khẩu | Hiện dialog mô tả giới hạn demo, không giả gửi email |
| TC12 | Chuyển dark mode / tăng font size | Giao diện cuộn, chữ không bị mất |

## Các test có mã

- Unit: `LoginValidatorTest.kt` (9 trường hợp).
- Unit: `LoginUseCaseTest.kt` (4 trường hợp).
- Android Compose UI test: `LoginScreenTest.kt` (2 trường hợp).
- Android Compose UI test: `ProfileScreenTest.kt` (1 trường hợp).
- Android instrumentation test: `DataStoreSessionRepositoryTest.kt` (1 trường hợp lưu/xóa phiên).

## Lệnh kiểm thử

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
```

Nếu dự án chưa có Android SDK hoặc thiếu mạng Gradle, các lệnh trên không thể hoàn tất; khắc phục môi trường trước khi xác nhận đạt nghiệm thu.
