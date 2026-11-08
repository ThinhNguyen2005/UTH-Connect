# Hướng dẫn Data Layer UTH Connect

Tài liệu này mô tả implementation hiện tại. Interface nghiệp vụ nằm trong
`domain/repository/`; implementation và Hilt bindings nằm trong
`data/repository/` và `data/di/`. UI đi theo luồng
`Route → ViewModel → UseCase → Repository`; Screen không gọi repository trực tiếp.

## Backend đang được build

`app/build.gradle.kts` hiện mặc định `useSupabase=true`, `useMockData=false`, đồng
thời yêu cầu Supabase URL và publishable key có tiền tố `sb_publishable_`.
Build không cấu hình các giá trị này sẽ dừng ở bước kiểm tra Gradle. Ví dụ
PowerShell từ thư mục gốc:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest `
  -PuseSupabase=true -PuseMockData=false `
  -PsupabaseUrl=https://YOUR-PROJECT.supabase.co `
  -PsupabasePublishableKey=sb_publishable_YOUR_KEY
```

Chỉ dùng URL/key của staging hoặc môi trường được phép. Publishable key không
thay thế RLS; không đưa `service_role`/secret key vào ứng dụng. Hướng dẫn migration,
bootstrap tài khoản và nghiệm thu staging ở [`../../supabase/README.md`](../../supabase/README.md).

## Luồng dữ liệu thực tế

- `SupabaseRestClient` là transport REST/GoTrue dùng `HttpURLConnection`,
  `kotlinx.serialization`, bearer token và publishable key. Repository gọi
  PostgREST tables/RPC; không phải Firebase SDK.
- Hilt modules hiện bind các interface cho Auth, User, Events, Tickets,
  Attendance Roster, Lost & Found, Schedule, Grades, Notifications và Announcements
  tới implementation Supabase. Ví dụ: `AuthDataModule`,
  `EventsDataModule`, `TicketDataModule` và
  `AttendanceRosterDataModule`.
- Auth gồm email/MSSV và Google token exchange. `PreferencesSessionRepository`
  điều phối khôi phục/lưu phiên; `SupabaseRestClient` giữ access credentials trong
  bộ nhớ và có đường lưu refresh token qua `SecureSessionStore` khi chính sách
  remember cho phép. Vì vậy không nên mô tả mọi phiên như luôn in-memory-only.
- Cụ thể, đăng nhập bằng mật khẩu truyền lựa chọn `rememberMe` từ UI vào
  `SignInUseCase` (giá trị UI mặc định hiện là `true`). Khi bật, `SecureSessionStore`
  mã hóa **UUID người dùng + refresh token** bằng AES-GCM với khóa trong Android
  Keystore trước khi lưu vào SharedPreferences. Lúc mở lại ứng dụng, client làm
  mới token và repository tải lại profile từ Supabase; token không được coi là
  danh tính đã xác minh chỉ vì được lưu trên máy. Khi không bật Remember Me,
  access credentials chỉ tồn tại trong phiên bộ nhớ. `AuthViewModel` hiện gọi
  `SignInWithGoogleUseCase` không truyền `remember`, vì vậy Google SSO dùng mặc
  định `remember = false` (phiên in-memory). Đăng xuất xóa phiên đã lưu; đăng
  nhập sinh trắc học vẫn bị vô hiệu hóa.
- Đăng ký sự kiện, quản lý sự kiện và check-in dùng RPC được migration định nghĩa.
  Vé QR hiện là mã ổn định có chữ ký server. Scanner kiểm tra quyền, sự kiện,
  đăng ký và chống check-in trùng; QR không tự hết hạn sau 60 giây.
- Schedule, grades, notifications và profile có Supabase repositories, nhưng
  việc có implementation không chứng minh dữ liệu production đã được nạp hoặc
  staging đã nghiệm thu.
- Phân biệt hai interface liên quan lịch học:
  `ScheduleRepository` được Hilt bind đến `SupabaseScheduleRepository`, đọc bảng
  `schedules` theo **thứ trong tuần**. Schema chưa có ngày họp lớp thực tế,
  nên `getScheduleByDate(date)` hiện kiểm tra cú pháp ngày rồi trả danh sách rỗng;
  không được suy diễn một buổi học theo tuần thành lịch học của ngày cụ thể.
  `TimetableRepository` lại được bind đến `LocalAccountTimetableRepository`:
  `Timetable.sessions` chưa có dữ liệu trường xác thực, còn deadline người dùng
  tự nhập được lưu trong SharedPreferences trên **thiết bị hiện tại**, tách theo
  Supabase UUID; chưa đồng bộ đa thiết bị hay lên Supabase.
- `UtilitiesDataModule` vẫn bind danh mục tiện ích tĩnh, học phí và điểm rèn
  luyện về repository báo chưa khả dụng, cài đặt về `PreferencesSettingsRepository`.
  Không mô tả các mục này như đã được tích hợp API học vụ.
- Fake, in-memory, static và Firebase repositories còn trong source tree. Tuy
  nhiên Gradle hiện từ chối `useMockData=true` hoặc `useSupabase=false`; sự tồn tại
  của các class đó không đồng nghĩa mock/Firebase build đang khả dụng.

## Ranh giới domain và UI

Khi thêm luồng dữ liệu:

1. Khai báo/điều chỉnh model và repository contract trong `domain/`.
2. Implement thao tác trong `data/repository/` và bind bằng Hilt ở module của feature.
3. Dùng `Flow` cho trạng thái theo dõi và `suspend`/`Result` cho thao tác một lần,
   theo contract hiện có của repository tương ứng.
4. Chuyển kết quả thành `UiState` trong ViewModel. Phân biệt loading, empty,
   content và error; không giả định network data là fixture.
5. Thêm migration/RLS/RPC khi thay đổi schema hoặc quyền; cập nhật tests và chạy
   kiểm tra staging phù hợp với thay đổi.

Không dùng tên hàm hay model trong tài liệu này làm hợp đồng thay thế source code.
Interface Kotlin, Hilt binding và migration mới nhất mới là căn cứ khi triển khai.

## Giới hạn đã biết

- `LocalAccountTimetableRepository.addDeadline/completeDeadline` đang nuốt lỗi
  `saveDeadlines` và vẫn cập nhật `StateFlow`. UI có thể hiển thị đã lưu dù
  ghi xuống thiết bị thất bại. **Đây là khoảng cách cần sửa trong code**, không
  phải hành vi hợp lệ để giảm yêu cầu về độ bền dữ liệu. Yêu cầu đích: thao tác
  lưu phải có kết quả lỗi tường minh, cập nhật UI sau khi ghi thành công và có
  test cho lỗi I/O, chuyển tài khoản, khởi động lại.
- `SupabaseScheduleRepository.getScheduleByDate` chỉ là khoảng trống chức năng;
  muốn lịch theo **ngày** phải có academic calendar/dated meetings đáng tin cậy
  và kiểm thử học kỳ, kỳ nghỉ, tuần học, trước khi nối dữ liệu vào UI.
- Firebase Auth/Firestore implementations và demo fixtures còn lưu trong source,
  dù build hiện khóa sang Supabase. Cần quyết định riêng có gỡ bỏ chúng hay khôi
  phục cấu hình demo; tài liệu không khẳng định chúng đang được dùng lúc chạy.
- Hạ tầng Firebase Crashlytics, Performance, Remote Config, FCM Topics, nén WebP
  và Coil 3 là quyết định kiến trúc; kiểm tra dependencies và wiring thực tế trước
  khi coi từng hạng mục đã triển khai.
- Chỉ build/test cục bộ không xác nhận migration đã áp dụng, RLS đúng trên project,
  Google OAuth đã cấu hình, hay deployment production hoạt động.
- Các sai lệch cần nâng cấp code/migration và mức ưu tiên nằm tại
  [`../reports/DATA_CODE_ALIGNMENT_AUDIT.md`](../reports/DATA_CODE_ALIGNMENT_AUDIT.md).
