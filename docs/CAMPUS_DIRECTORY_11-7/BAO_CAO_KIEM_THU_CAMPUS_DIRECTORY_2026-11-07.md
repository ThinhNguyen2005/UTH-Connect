# Báo cáo kiểm thử — CAMPUS-DIRECTORY

**Ngày chạy:** 07/11/2026
**Thiết bị:** Android Emulator `Medium_Phone_API_37.0` (Android 17 / API 37)
**Kết luận:** Màn Campus và các Intent đã được kiểm tra trên emulator bằng component harness; route production còn bị chặn tại cổng đăng nhập vì thiếu cấu hình Firebase staging.

## Tóm tắt kết quả

| Hạng mục | Kết quả thực tế | Bằng chứng |
|---|---|---|
| Rà soát màn Campus, địa chỉ 3 cơ sở và route Tiện ích | PASS khi rà mã nguồn: địa chỉ theo yêu cầu; route `UtilitiesDestination.Campuses` đã nối; màn dùng `ACTION_VIEW` với `geo:0,0?q=...` và hotline dùng `ACTION_DIAL`/`tel:` | `../CAMPUS_DIRECTORY_DAILY_REPORT_2026-11-07.md` |
| Build Debug trong worktree `_kan95` | PASS — `:app:assembleDebug` | `../../app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests trong worktree `_kan95` | PASS — 48 tests, 0 failures, 0 errors, 0 skipped | `../../app/build/test-results/testDebugUnitTest/` |
| Android instrumentation trong worktree `_kan95` | PASS — 1 smoke test (`useAppContext`), 0 failures, emulator API 37; không phải UI test cho Campus | `../../app/build/outputs/androidTest-results/connected/debug/` |
| Detekt / Spotless | PASS — đã cấu hình task trong Gradle; `detekt` phân tích route và package campuses; `spotlessCheck` pass | `../../app/build/reports/detekt/detekt.html` |
| Mở app và thử khôi phục session | BLOCKED — app hiện thông báo “Could not restore the old session. Sign in again.”; bấm Retry vẫn ở màn đăng nhập | `01-app-auth-gate.png`, `03-session-retry.png`, `04-after-test-rerun.png` |
| Xác nhận backend | BLOCKED — không có `app/google-services.json` và `.firebaserc`; tài liệu thiết lập trong repo ghi Firebase chưa cấu hình/deploy. Không thể xác thực tài khoản staging từ app này | Ảnh màn đăng nhập và `../AUTH3_FIREBASE_SETUP.md` |
| Mở component `CampusesScreen` trên emulator | PASS trong harness instrumentation tạm để kiểm tra component; harness bỏ qua cổng đăng nhập và không chứng minh route production | `08-campus-screen-manual-harness.png` |
| Điều hướng production qua đăng nhập → Tiện ích → Cơ sở | BLOCKED — app chính vẫn ở cổng đăng nhập; chưa cấu hình Firebase staging/session để đi qua route thật | `07-fresh-uthconnect.png` |
| Bấm Chỉ đường Cơ sở 1 | PASS trong component harness — mở Google Maps, kết quả Trường Đại học GTVT TP.HCM tại Võ Oanh | `10-directions-button-result.png` |
| Bấm Chỉ đường Cơ sở 2 | PASS trong component harness — Maps nhận địa chỉ Đường 12/An Khánh và hiển thị kết quả UTH cơ sở 2 | `11-directions-campus-2.png` |
| Bấm Chỉ đường Cơ sở 3 | PASS trong component harness — Maps nhận địa chỉ Tô Ký/Tân Chánh Hiệp và hiển thị UTH cơ sở 3 | `12-directions-campus-3.png` |
| Bấm Gọi hotline trên thẻ cơ sở | PASS trong component harness — mở giao diện quay số với hotline Cơ sở 1 `02838992862` | `09-call-button-result.png` |
| Danh bạ phòng ban | PASS khi hiển thị — thấy bốn phòng ban và hotline trên màn hình | `13-department-directory.png` |
| Bấm Gọi hotline phòng ban | PASS trong component harness — nút Phòng Đào tạo mở giao diện quay số với `02838992862` | `16-department-call-result.png` |

## Checklist kiểm thử

- [x] Rà soát địa chỉ Cơ sở 1: `Số 2, Võ Oanh (Nguyễn Văn Thương cũ), P.25, Q. Bình Thạnh, TP.HCM`.
- [x] Rà soát địa chỉ Cơ sở 2: `Số 10, Đường 12, P. An Khánh, TP. Thủ Đức, TP.HCM`.
- [x] Rà soát địa chỉ Cơ sở 3: `Số 70, Tô Ký, P. Tân Chánh Hiệp, Q.12, TP.HCM`.
- [x] Rà soát route `UtilityModuleId.CAMPUSES`/`UtilitiesDestination.Campuses` và cách gọi Intent trong mã nguồn.
- [x] Mở app thật trên emulator; ghi lại trạng thái đăng nhập và thử Retry session.
- [x] Chạy build Debug, unit tests và instrumentation tests hiện có.
- [x] Rerun riêng `assembleDebug`, `testDebugUnitTest` và `connectedDebugAndroidTest`; build/unit pass, instrumentation 1/1 pass trên `Medium_Phone_API_37.0`.
- [x] Hiển thị `CampusesScreen` bằng harness instrumentation trên emulator (component test, không qua route auth).
- [x] Bấm Chỉ đường cho cả ba cơ sở; Google Maps nhận địa chỉ và trả kết quả UTH phù hợp.
- [x] Bấm Gọi hotline từ thẻ cơ sở; giao diện quay số hiển thị đúng số.
- [x] Hiển thị danh bạ phòng ban và bấm gọi Phòng Đào tạo; giao diện quay số hiển thị đúng số.
- [ ] Xác nhận route production từ Utilities vào Campus bằng tài khoản đã đăng nhập.
- [x] Chạy `detekt spotlessCheck` sau khi cấu hình task; cả hai task BUILD SUCCESSFUL.

## Ảnh bằng chứng trong folder này

- `01-app-auth-gate.png`: ảnh app đang ở màn đăng nhập, có lỗi khôi phục session.
- `01-app-auth-gate.xml`: cây giao diện của ảnh trên.
- `02-app-running.png`: ảnh app sau khi đưa lên foreground.
- `03-session-retry.png`: kết quả sau thao tác Retry session; app vẫn ở cổng đăng nhập.
- `04-after-test-rerun.png`: ảnh sau khi cài lại APK và mở app sau bộ instrumentation; vẫn bị chặn tại đăng nhập.
- `07-fresh-uthconnect.png`: trạng thái UTH Connect sau khi reboot AVD.
- `08-campus-screen-manual-harness.png`: màn Campus thật trong component harness; có đủ ba cơ sở, nút gọi/chỉ đường.
- `09-call-button-result.png`: dialer sau khi bấm Gọi trên thẻ Cơ sở 1.
- `10-directions-button-result.png`: Google Maps sau khi bấm Chỉ đường Cơ sở 1.
- `11-directions-campus-2.png`: Google Maps sau khi bấm Chỉ đường Cơ sở 2.
- `12-directions-campus-3.png`: Google Maps sau khi bấm Chỉ đường Cơ sở 3.
- `13-department-directory.png`: danh bạ bốn phòng ban và hotline.
- `16-department-call-result.png`: dialer sau khi bấm Gọi của Phòng Đào tạo.

Các ảnh Campus/Maps/Dialer là bằng chứng component trên thiết bị thật của emulator, nhưng không chứng minh route production do auth gate. Ảnh trạng thái đăng nhập chỉ ghi nhận blocker backend/configuration.

**Phương pháp kiểm tra component:** dùng một Compose instrumentation harness tạm trong checkout test để hiển thị `CampusesScreen` và bấm các nút trên emulator; harness đã được gỡ sau khi chụp ảnh. SHA-256 của `CampusesScreen.kt`, `CampusesUiState.kt` và `UtilitiesRoute.kt` khớp giữa checkout test và worktree `_kan95`. Harness bỏ qua cổng đăng nhập nên không xác nhận được hành trình production từ Tiện ích.

### Màn Campus và ba địa chỉ

![Campus screen hiển thị ba cơ sở và các nút gọi/chỉ đường](08-campus-screen-manual-harness.png)

### Kết quả bấm Chỉ đường

**Cơ sở 1 — Võ Oanh:**

![Google Maps nhận địa chỉ Cơ sở 1](10-directions-button-result.png)

**Cơ sở 2 — Đường 12, An Khánh:**

![Google Maps nhận địa chỉ Cơ sở 2](11-directions-campus-2.png)

**Cơ sở 3 — Tô Ký, Tân Chánh Hiệp:**

![Google Maps nhận địa chỉ Cơ sở 3](12-directions-campus-3.png)

### Kết quả bấm Gọi và danh bạ phòng ban

**Hotline Cơ sở 1:**

![Dialer mở với hotline Cơ sở 1](09-call-button-result.png)

**Danh bạ phòng ban:**

![Danh sách phòng ban và hotline](13-department-directory.png)

**Hotline Phòng Đào tạo:**

![Dialer mở với hotline Phòng Đào tạo](16-department-call-result.png)

### Cổng đăng nhập production

![UTH Connect sau reboot vẫn yêu cầu đăng nhập do không khôi phục được session](07-fresh-uthconnect.png)

## Việc cần có để hoàn tất nghiệm thu trên app

1. Owner cung cấp Firebase staging đã cấu hình hợp lệ cho `com.giathinh.uthconnect` (đặt `google-services.json` cục bộ, không commit file này) và xác nhận backend/provider.
2. Cấp tài khoản test được phép đăng nhập, đã xác minh email và có hồ sơ/claims cần thiết. Không ghi mật khẩu hoặc token vào Jira/báo cáo.
3. Mở lại app, đăng nhập bằng tài khoản test, vào Tiện ích → Tra cứu Cơ sở & Danh bạ; chụp từng màn hình sau khi bấm Chỉ đường và Gọi hotline.
4. Đã cấu hình Detekt/Spotless, sửa các vi phạm trong feature và xác minh lại thành công.

**Trạng thái DoD:** Build Debug, 48 unit tests, 1 connected instrumentation smoke test và `detekt spotlessCheck` đều pass trong lượt rerun cuối. Các thao tác campus/Maps/Dialer có ảnh bằng chứng từ component harness; đây không phải UI instrumentation test và không chứng minh route production. DoD chưa hoàn tất tuyệt đối vì chưa đi được vào màn Campus qua app production: app báo không khôi phục được session và repo thiếu Firebase staging config/không xác minh được tài khoản backend. Cần cấu hình backend staging và tài khoản test hợp lệ rồi chụp thêm bằng chứng route trong app.
