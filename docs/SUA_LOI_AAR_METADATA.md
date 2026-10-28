# Sửa lỗi `:app:checkDebugAarMetadata` (4 issues)

## Nguyên nhân

Phiên bản Lifecycle `2.11.0` đòi Android compile SDK 37 và AGP 9.1+,
trong khi project demo dùng compileSdk 36 và AGP 8.10.1.

## Đã sửa

Trong `gradle/libs.versions.toml`, thay

```toml
lifecycle = "2.11.0"
```

bằng

```toml
lifecycle = "2.9.4"
```

Giữ nguyên: AGP 8.10.1, Kotlin 2.2.10, compileSdk 36, targetSdk 36, minSdk 31.
Không thay đổi code đăng nhập, profile, DataStore hay giao diện.

## Chạy trên Android Studio

1. Mở **thư mục project bên trong** có `settings.gradle.kts`.
2. File > Sync Project with Gradle Files.
3. Build > Rebuild Project (hoặc Run app).
4. Nếu có lỗi mới, mở Build Output và gửi log lỗi tương ứng.

**Giới hạn kiểm thử:** Bản vá này khắc phục xung đột phiên bản trong thông báo
AAR metadata được cung cấp, chưa được xác minh build APK trên máy thực do
không có Android SDK trong môi trường tạo file.
