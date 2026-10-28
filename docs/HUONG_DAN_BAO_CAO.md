# Gợi ý trình bày đồ án — Module Xác thực & Hồ sơ sinh viên

## 1. Giới thiệu

- **Đề tài:** UTH Connect — ứng dụng tiện ích sinh viên UTH.
- **Module phụ trách:** Authentication & Profile.
- **Mục tiêu:** xây dựng luồng đăng nhập, kiểm tra đầu vào, trang chính, hồ sơ và ghi nhớ phiên dùng được khi demo.

## 2. Phân tích chức năng theo ảnh giao việc

| STT | Yêu cầu | File chính | Cách demo |
|---|---|---|---|
| 1 | Màn hình đăng nhập | `feature/auth/ui/LoginScreen.kt` | Nhập MSSV/email, mật khẩu, checkbox |
| 2 | Kiểm tra rỗng và định dạng | `domain/validation/LoginValidator.kt` | Bỏ trống, nhập `abc` hoặc `demo@gmail.com` |
| 3 | Xử lý đăng nhập | `domain/usecase/LoginUseCase.kt`, `data/auth/DemoAuthRepository.kt` | Demo đúng/sai mật khẩu |
| 4 | Chuyển trang sau login | `ui/UthConnectApp.kt`, `feature/home/ui/HomeScreen.kt` | Login -> Trang chính |
| 5 | Hồ sơ cá nhân | `feature/profile/ui/ProfileScreen.kt` | Họ tên, MSSV, lớp, ngành, khoa, avatar |
| 6 | Giữ phiên | `data/session/DataStoreSessionRepository.kt` | Bật nhớ -> đóng mở app -> còn phiên |
| 7 | Đăng xuất | `feature/auth/AuthViewModel.kt` | Profile -> Đăng xuất -> Login |

## 3. Kiến trúc phần mềm

- **UI:** Composable nhận state + callback (không xử lý dữ liệu backend).
- **ViewModel:** `MutableStateFlow<AuthUiState>` điều khiển luồng màn hình và biến đổi trạng thái.
- **Domain:** entity `StudentProfile`, `LoginValidator`, `LoginUseCase`, repository interfaces; không import Android.
- **Data:** tài khoản demo và DataStore session.
- **DI:** constructor injection qua `AuthViewModelFactory`.

```text
LoginScreen --onSignInClick--> AuthViewModel
                                     |
                                     v
                               LoginUseCase
                              /            \
                        Validator     AuthRepository
                                         |
                                    DemoAuthRepository
                                     (offline)
                                     |
                              StudentProfile
                                     |
                   AuthViewModel -> UthConnectApp
                        |               |
               SessionRepository   Home -> Profile
                        |
               DataStoreSessionRepository
```

## 4. Thuật toán đăng nhập (giải thích khi báo cáo)

1. Sinh viên nhập **MSSV hoặc email** và mật khẩu.
2. Nhấn Đăng nhập. ViewModel đặt `isSigningIn = true` để chống bấm lặp.
3. `LoginValidator` kiểm tra `identifier.trim()` có rỗng không.
4. Nếu là MSSV, dữ liệu demo yêu cầu **8–12 chữ số**; nếu là email yêu cầu dạng `...@ut.edu.vn`.
5. Mật khẩu không được rỗng (bản demo không đặt chính sách mật khẩu phức tạp).
6. `LoginUseCase` trả `InvalidForm` ngay nếu đầu vào không hợp lệ.
7. Nếu hợp lệ, `DemoAuthRepository.authenticate()` so khớp 1 tài khoản demo.
8. Nếu sai -> trả `InvalidCredentials`, giao diện hiển thị lỗi; đúng -> `Success(profile)`.
9. Nếu bật nhớ đăng nhập -> DataStore lưu `studentId`; nếu không bật -> xóa mã đã nhớ.
10. ViewModel xóa mật khẩu khỏi UI state, cập nhật `student` và chuyển sang Trang chính.

## 5. Thuật toán giữ phiên và đăng xuất

**Khởi động:** đọc DataStore -> có studentId thì đọc Profile mẫu tương ứng -> nếu hợp lệ, vào Home; nếu không có, vào Login.

**Đăng xuất:** gọi `SessionRepository.clear()` -> xóa phiên trên ổ đĩa -> xóa state sinh viên -> về Login. Nếu xóa dữ liệu thất bại, không đăng xuất giả để tránh bỏ sót phiên đã lưu.

**Phạm vi bảo mật:** Trong Demo, studentId không phải token bảo mật; Firebase/Auth API thật phải được kiểm tra phía server và dựa trên auth token; custom claim `uthVerified` không thể cấp ở client.

## 6. Đề xuất ảnh chụp minh chứng trong báo cáo

1. Login trống và thông báo bắt buộc.
2. Login nhập email ngoài trường -> báo định dạng không hợp lệ.
3. Login nhập tài khoản sai -> sai thông tin.
4. Login bằng MSSV đúng -> Home.
5. Nhấn Xem hồ sơ -> Profile đầy đủ trường thông tin.
6. Đăng xuất -> Login.
7. Bật/tắt Ghi nhớ để chứng minh phiên qua khởi động lại.
8. Dark mode hoặc chữ lớn 130% trên Compose Preview.

## 7. Phạm vi tài liệu nguồn

README tổng khuyến nghị Hilt, Navigation Compose, Firebase Auth và Firestore cho **kiến trúc đích**. Trong module nhỏ này dùng luồng giả lập (được ảnh phân công cho phép) với StateFlow + DI bằng Factory, bảo đảm tách file và dễ thay bằng Firebase khi nhóm tích hợp.
