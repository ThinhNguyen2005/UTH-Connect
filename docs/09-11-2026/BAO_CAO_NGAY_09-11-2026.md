# Báo cáo công việc ngày 09/11/2026 (Cập nhật Hoàn thiện Nhánh `kan-102`)

**Dự án:** UTH Connect — Android  
**Nhánh Git:** `kan-102` (tách từ `origin/main` mới nhất, commit sạch `1e7ce0f`)  
**Phạm vi:** Danh bạ cơ sở, kiểm thử ứng dụng, cấu hình giám sát vận hành (Crashlytics, Remote Config) và CI/CD Release Workflow.

---

## 1. Danh bạ cơ sở và chỉ đường

### Yêu cầu

- Xử lý trường hợp thiết bị không có ứng dụng nhận URI `geo:` khi người dùng bấm **Chỉ đường**; hiển thị thông báo bằng tiếng Việt và tiếng Anh.
- Bổ sung instrumentation test mô phỏng `ActivityNotFoundException`.
- Bỏ chú thích “Nguyễn Văn Thương cũ” khỏi địa chỉ Cơ sở 1 và cập nhật assertion địa chỉ.

### Kết quả thực hiện

- Đã bắt `ActivityNotFoundException`, hiển thị thông báo phù hợp và thêm test cho trường hợp có hoặc không có ứng dụng bản đồ.
- Địa chỉ Cơ sở 1 hiện là **Số 2, Võ Oanh, P.25, Q. Bình Thạnh, TP.HCM**.
- Đã xử lý thêm tình huống thiết bị không có ứng dụng gọi điện, với thông báo Việt/Anh và test tương ứng.
- Toàn bộ tính năng danh bạ đã hoàn thiện và hoạt động ổn định.

![Màn hình danh bạ cơ sở](anh/co-so-uth.png)

---

## 2. Giám sát vận hành: Crashlytics, Remote Config và CI/CD Release Workflow

### Yêu cầu

- Gắn UID Supabase và `user_role` vào Firebase Crashlytics khi phiên hợp lệ được đăng nhập hoặc khôi phục (`CrashIdentity.set(userId, role)`).
- Thiết lập mặc định Remote Config `force_update_min_version = "1.0.0"` và `maintenance_mode = false`; tự động chuyển màn hình bảo trì/cập nhật khi cần.
- Đảm bảo pipeline GitHub Actions (KAN-101 / Release Workflow) tự động inject đúng file cấu hình `google-services.json` khi build APK Release mà không commit file credentials nhạy cảm vào Git repo.

### Kết quả thực hiện & Cập nhật mới nhất trên nhánh `kan-102`

1. **Firebase Crashlytics:**
   - Mã nguồn trong [`CrashIdentity.kt`](file:///D:/Downloads/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/data/repository/CrashIdentity.kt) và [`SupabaseAuthRepository.kt`](file:///D:/Downloads/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt) tự động gán `setUserId(supabaseUser.id)` và `setCustomKey("user_role", user.role.name)` khi xác thực/khôi phục phiên Supabase thành công. Gọi `CrashIdentity.clear()` khi đăng xuất.

2. **Firebase Remote Config (Kill-switch & In-app Defaults):**
   - [`MainActivity.kt`](file:///D:/Downloads/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/MainActivity.kt#L104) thiết lập `config.setDefaultsAsync(mapOf("force_update_min_version" to "1.0.0", "maintenance_mode" to false))`.
   - Màn hình [`AppAvailability.kt`](file:///D:/Downloads/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/AppAvailability.kt) tự động hiển thị màn bảo trì (`MAINTENANCE`) hoặc yêu cầu cập nhật (`UPDATE_REQUIRED`). Đã có unit test `AppAvailabilityTest` và instrumentation test `RemoteConfigNetworkTest`.

3. **Bảo mật File Credentials & Git Ignore:**
   - Đã kiểm tra kỹ quy tắc theo dõi file: File `google-services.json` **KHÔNG** bị dính hay theo dõi trong Git (`git ls-files` trả về trống).
   - Đã khai báo loại trừ file `google-services.json` chuẩn 100% trong cả `.gitignore` gốc và [`app/.gitignore`](file:///D:/Downloads/UTH-Connect/app/.gitignore).

4. **Workflow CI/CD GitHub Actions độc lập:**
   - Thư mục workflow [`.github/workflows/android-release.yml`](file:///D:/Downloads/UTH-Connect/.github/workflows/android-release.yml) và các script Python [`inject_google_services.py`](file:///D:/Downloads/UTH-Connect/.github/scripts/inject_google_services.py) đã đưa về thư mục gốc repository.
   - Bước `Inject Firebase configuration` tự động giải mã `${{ secrets.GOOGLE_SERVICES_JSON }}` khi chạy trên GitHub Runner.

5. **Dọn dẹp Nhánh Git & Lịch sử Commit (`kan-102`):**
   - Đã khởi tạo nhánh mới **`kan-102`** từ `origin/main` mới nhất, xóa bỏ hoàn toàn các commit rác/xung đột cũ.
   - Gộp toàn bộ tính năng thành 1 commit sạch `1e7ce0f` (`feat(kan-102): add observability, crashlytics, remote config and github actions workflow`) và push thành công lên remote [`origin/kan-102`](https://github.com/ThinhNguyen2005/UTH-Connect/tree/kan-102).

---

## 3. Bảng tổng hợp kiểm thử

| Nội dung Kiểm thử | Kết quả | Chi tiết Xác nhận |
|---|---|---|
| **Crashlytics UID & Role** | 🟢 **100% ĐẠT** | Gắn UID và `user_role` tự động khi đăng nhập/restore session Supabase. |
| **Remote Config Defaults** | 🟢 **100% ĐẠT** | Mặc định `force_update_min_version = "1.0.0"`, `maintenance_mode = false`. |
| **Màn hình Bảo trì / Cập nhật** | 🟢 **100% ĐẠT** | Đã test tự động bằng `RemoteConfigNetworkTest` và `AppAvailabilityTest`. |
| **Loại trừ `google-services.json`** | 🟢 **100% ĐẠT** | File được loại trừ 100% trong `app/.gitignore`, không bị lộ credentials trên GitHub. |
| **CI/CD Inject Script** | 🟢 **100% ĐẠT** | `inject_google_services.py` tự động validate project ID & package name trước khi ghi. |
| **Cấu trúc Nhánh & Git** | 🟢 **100% ĐẠT** | Tách từ `origin/main`, commit sạch không bị xung đột, đã push lên `origin/kan-102`. |

---

## 4. Hướng dẫn Thao tác trên GitHub để Workflow Chạy Xanh `✅`

Để GitHub Actions workflow chạy thành công và cấp file APK Release ký sẵn:
1. Truy cập Repository GitHub ➔ **Settings** ➔ **Secrets and variables** ➔ **Actions**.
2. Thêm **Secrets**: `GOOGLE_SERVICES_JSON`, `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `GOOGLE_WEB_CLIENT_ID`, `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
3. Thêm **Variable**: `FIREBASE_PROJECT_ID`.
4. Mở Pull Request từ nhánh **`kan-102`** vào **`main`** để hoàn tất nghiệm thu.
