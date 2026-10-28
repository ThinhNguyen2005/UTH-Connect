# Kết quả tự kiểm tra tại môi trường tạo mã nguồn

- **Đã kiểm tra:** các file XML hợp lệ; biên dịch `domain/` và `DemoAuthRepository.kt` với Kotlin CLI thành công.
- **Đã chạy:** 8 lượt kiểm tra thực thi logic nhập MSSV/email/định dạng/mật khẩu/đăng nhập đúng-sai: đều PASS.
- **Chưa kiểm chứng:** `:app:assembleDebug`, Android instrumentation/UI tests, chạy trên máy ảo/thực. Môi trường tạo file không có Android SDK, Gradle distribution và không có quyền tải dependencies trực tiếp.
- **Cần thực hiện trên Android Studio của bạn:** Gradle Sync, Run `app`, `testDebugUnitTest`, `connectedDebugAndroidTest`; ghi nhận ảnh/điểm thực tế để nghiệm thu.

Không tuyên bố APK đã build được hoặc Firestore/Firebase đã tích hợp khi chưa thực hiện.
