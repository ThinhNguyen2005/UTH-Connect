# Sau khi nhóm tích hợp Firebase (ngoài phạm vi demo hiện tại)

1. Kết nối Firebase project của nhóm, thêm `google-services.json` qua kênh riêng và không commit lên git public.
2. Viết `FirebaseAuthRepository : AuthRepository`, thực hiện đăng nhập bằng phương thức Firebase Auth được nhóm thống nhất. **Không** dùng `MSSV + mật khẩu Firebase` bằng cách tự giả email khi chưa có cơ chế account mapping an toàn.
3. Lấy hồ sơ từ Firestore `users/{uid}` với trường `displayName`, `studentId`, `faculty`, `avatarUrl`, và có thể mở rộng `className`, `major` sau khi chốt schema với nhóm.
4. Xác minh email trường qua Firebase email verification; nếu dùng MSSV, chỉ backend/Admin được verify và tạo `uthVerified` custom claim. Phải kiểm chứng token/claim trong Security Rules, không tin `studentId` nhập từ client.
5. Firebase Auth SDK tự quản lý phiên xác thực; không lưu access token/password trong Preferences hay DataStore. Bật Remember Me là tùy chính sách sản phẩm, không thay thế xác thực thật.
6. Quên mật khẩu: chỉ cho email đã hợp lệ và dùng cơ chế reset chính thức của Auth provider, thông báo không tiết lộ tài khoản nào tồn tại.
7. Avatar: thêm tải ảnh bằng thư viện image loader, và Storage Rules để user chỉ upload dưới `users/{uid}/...`; phải kiểm tra MIME và giới hạn dung lượng từ backend rules.
8. Bổ sung unit/integration test và Firebase Emulator Rules test theo tiêu chí DoD trong README tổng.

**Quan trọng:** Trong README tổng `role` nằm ở **custom claims**, không được tự ý tin role trong `users/{uid}`. `uthVerified` cũng không được tự đặt true ở app demo này.
