# Kiểm toán tài liệu dữ liệu so với code — 2026-11-08

**Phạm vi:** `docs/data/DATA_LAYER_GUIDE.md`, `docs/data/DATABASE_SCHEMA.md` đối chiếu
với Kotlin repository/Hilt bindings, Auth UI/use cases và SQL trong
`supabase/migrations/`. Đây là **kiểm toán source tại workspace**, chưa xác minh
schema của Supabase staging/production, dữ liệu thực hoặc hành vi trên thiết bị.

**Quy tắc xử lý:** nếu code có cơ chế đáng tin cậy hơn tài liệu mô tả thì cập
nhật tài liệu. Nếu code chưa đáp ứng kỳ vọng về bảo mật, tính đúng đắn hoặc độ
bền dữ liệu, giữ chuẩn đích và ghi công việc sửa code; không hợp thức hóa lỗi
bằng cách hạ tiêu chuẩn tài liệu. Không sửa source/config, không sửa/xóa
`docs/plan/`, không xóa tài liệu.

## 1. Đã đồng bộ tài liệu theo implementation có căn cứ

| Hạng mục | Bằng chứng trong source | Kết luận / cập nhật |
|---|---|---|
| Session/password | `data/repository/SecureSessionStore.kt`, `PreferencesSessionRepository.kt`, `SupabaseRestClient.kt`, `presentation/auth/AuthViewModel.kt` | Có AES-GCM/Android Keystore lưu refresh token khi Remember Me; restore làm mới token và lấy lại profile. Không còn mô tả tất cả phiên là in-memory. |
| Google SSO | `domain/usecase/SignInWithGoogleUseCase.kt`, `presentation/auth/AuthViewModel.kt` | Use case có tham số `remember=false` và ViewModel đang dùng mặc định: Google SSO in-memory. Phân biệt với remember của đăng nhập mật khẩu. |
| QR & quyền | `supabase/migrations/20261104000000_stable_ticket_qr.sql`, `20261103010000_secure_core_workflows.sql` | QR ổn định ký HMAC, check-in xác minh phía server, không có TTL 60 giây; chỉ là hợp đồng trong migration cho tới khi kiểm thử staging. |
| Lịch theo thứ / ngày | `data/di/ScheduleDataModule.kt`, `data/repository/SupabaseUniversityRepositories.kt` | Schedule theo thứ đọc Supabase; `getScheduleByDate()` trả rỗng vì chưa có lịch học theo ngày. |
| Deadline cá nhân | `data/di/TimetableDataModule.kt`, `data/repository/LocalAccountTimetableRepository.kt` | Dữ liệu cục bộ phân tách theo Supabase UUID; không có đồng bộ đa thiết bị hay dữ liệu buổi học có ngày chính thức. |
| Tiện ích | `data/di/UtilitiesDataModule.kt` | Danh mục tĩnh; học phí/điểm rèn luyện chưa có nguồn dữ liệu khả dụng; phần cài đặt dùng preferences. |
| Role & bảng nghiệp vụ | `supabase/migrations/20261107000000_auth_rbac_and_housing.sql`, `20261107000000_lost_found_rls.sql` | Bổ sung `lecturer`, `user_roles`, `housing_posts`, `lost_found`, RLS; lưu ý trigger role mới chỉ thêm, chưa xóa role cũ. |

## 2. Code/migration chưa đạt chuẩn: cần sửa, không hạ tài liệu

### P0 — Luồng cấp admin bằng danh sách email viết cứng

- **Bằng chứng:** `supabase/migrations/20261107000000_auth_rbac_and_housing.sql`,
  hàm `public.handle_new_user()` gán `v_role := 'admin'` khi email được xác nhận
  và khớp danh sách email trong source. `20261104000000_auth_security_expansion.sql`
  có trigger giới hạn domain; hai ràng buộc phải được kiểm thử cùng nhau.
- **Vấn đề:** dùng email để bootstrap quyền quản trị khiến quy tắc đặc quyền
  khó kiểm toán, dễ ảnh hưởng khi tài khoản/email thay đổi. Chưa thể khẳng định
  đường khai thác thực tế vì chưa kiểm tra trạng thái trigger và dữ liệu staging.
- **Hướng sửa:** xác định quản trị viên hiện hữu bằng quy trình đáng tin cậy;
  tạo migration **tiến về phía trước** thay phép gán admin theo email bằng bước
  cấp quyền có xác thực, audit log và chính sách thu hồi. Kiểm tra tính đúng đắn
  của role/profile/JWT sau cấp, thu hồi và refresh token.
- **Nghiệm thu:** đăng ký bằng bất kỳ email hợp lệ nào mặc định chỉ có quyền
  sinh viên; chỉ luồng quản trị được phép mới nâng quyền; test các vai trò và
  regression RLS trên staging.

### P1 — Hai cặp migration trùng version

- **Bằng chứng:** `supabase/migrations/20261104000000_{auth_security_expansion,stable_ticket_qr}.sql`
  và `supabase/migrations/20261107000000_{auth_rbac_and_housing,lost_found_rls}.sql`.
- **Vấn đề:** cùng version dễ làm Supabase CLI/history không phân biệt được hai
  nội dung. Chưa có kết quả `migration list`/`schema_migrations` trên staging,
  nên chưa kết luận migration nào được áp dụng hay lệnh nào thất bại.
- **Hướng sửa:** đối chiếu lịch sử migration ở từng môi trường và hành vi CLI;
  chọn kế hoạch đồng bộ an toàn (migration bù tiến về phía trước hoặc quy trình
  repair được rà soát), kiểm tra toàn bộ RPC, bảng và RLS. Không đổi tên file
  đã áp dụng hoặc chạy lại migration chỉ dựa vào tên.
- **Nghiệm thu:** lịch sử migration nhất quán, deploy sạch lên môi trường thử
  nghiệm và deploy nâng cấp staging cho kết quả schema/permissions tương đương.

### P1 — Deadline báo thành công dù lưu thất bại

- **Bằng chứng:** `data/repository/LocalAccountTimetableRepository.kt` trong
  `addDeadline()` và `completeDeadline()` gọi
  `runCatching { saveDeadlines(...) }` nhưng luôn `state.update(...)` sau đó.
  Trong khi đó `saveDeadlines()` dùng `.commit()` và có thể ném exception.
- **Vấn đề:** UI hiển thị deadline đã lưu nhưng dữ liệu biến mất sau restart nếu
  ghi lỗi; API thao tác hiện trả `Unit` và không cung cấp lỗi cho ViewModel.
- **Hướng sửa:** xác định contract `Result`/suspend action; chỉ publish state
  sau khi ghi bền vững; gửi lỗi lên UI, giữ dữ liệu cũ nếu commit không thành
  công, vẫn phân tách theo user UUID. Có thể cân nhắc DataStore/Room sau khi
  đánh giá migration, nhưng không bắt buộc thay storage chỉ để che lỗi.
- **Nghiệm thu:** test ghi thất bại, restore sau restart, chuyển tài khoản,
  dữ liệu hỏng và thông báo lỗi; không có trạng thái thành công giả.

### P1 — Lịch theo ngày chưa có dữ liệu có thể chứng minh

- **Bằng chứng:** `SupabaseScheduleRepository.getScheduleByDate()` validate
  `LocalDate.parse(date)` rồi `emit(emptyList())`; `schedules` trong
  `20261103000000_init_uth_connect.sql` chỉ chứa `day_of_week`, học kỳ và tiết.
  `LocalAccountTimetableRepository` cũng để `Timetable.sessions` rỗng.
- **Vấn đề:** tính năng lịch theo ngày/tuần học không thể kết luận lịch chính
  xác từ thứ trong tuần; không nên tạo lịch giả từ dữ liệu thiếu ngày.
- **Hướng sửa:** thống nhất nguồn academic calendar chính thức, học kỳ/tuần
  học, ngoại lệ nghỉ/bù; bổ sung schema và repository mapping, cập nhật UI
  empty/error theo luồng học vụ. Trước khi có nguồn tin cậy, giữ trạng thái
  chưa có lịch thay vì sinh dữ liệu sai.
- **Nghiệm thu:** kiểm tra nhiều học kỳ, ngày nghỉ, tuần học ngoại lệ, timezone,
  quyền truy cập và hiển thị empty/error.

### P2 — Role phụ còn lại sau khi đổi quyền

- **Bằng chứng:** `handle_profile_role_sync()` trong
  `20261107000000_auth_rbac_and_housing.sql` chỉ `INSERT ... ON CONFLICT DO NOTHING`
  vào `user_roles` và cập nhật `auth.users.raw_app_meta_data`, không xóa role cũ.
- **Vấn đề:** `user_roles` có thể giữ cả role trước và sau khi hạ quyền. `is_admin()`
  hiện đọc `profiles.role`, nên chưa có bằng chứng lỗi leo thang quyền qua hàm
  này, nhưng các consumer khác có thể đọc dữ liệu sai.
- **Hướng sửa:** chốt mô hình 1 role hay nhiều role; nếu 1 role thì chuẩn hóa
  uniqueness, migration loại vai trò cũ và trigger cập nhật/thu hồi nhất quán;
  nếu nhiều role thì mô hình authorization và semantics phải được định nghĩa rõ.
- **Nghiệm thu:** thử nâng/hạ quyền, làm mới JWT, đối chiếu `profiles`,
  `user_roles`, custom claims, policy và không còn quyền tồn dư.

## 3. Tài liệu ngoài phạm vi đang lệch, đề nghị xử lý ở lượt riêng

- `supabase/README.md` hiện nói đăng nhập **luôn in-memory và Remember Me tắt**,
  mâu thuẫn `SecureSessionStore`/`PreferencesSessionRepository`; phần chỉ dẫn
  migration cũng cần so lại với đủ 13 SQL migration đang tồn tại.
- `README.md` vẫn mô tả REST qua `supabase-kt` và Room cache như phần code hiện
  tại ở một số chỗ; transport Android thực tế là `SupabaseRestClient`
  (`HttpURLConnection`), còn deadline lưu SharedPreferences. Cần tách rõ
  **kiến trúc đích** với **implementation hiện tại**, không tự hạ tiêu chuẩn.
- `docs/Design.md` một số đoạn còn nói `InMemoryTimetableRepository` và nhắc
  dữ liệu sample; Hilt hiện đã bind `LocalAccountTimetableRepository`.

## 4. Trình tự chuẩn bị sửa code

1. **Chặn rủi ro quyền:** kiểm tra trạng thái migration/trigger trên staging;
   loại bootstrap admin bằng email và kiểm tra quyền từng vai trò qua RLS/RPC.
2. **Ổn định chuỗi migration:** xác minh duplicate version, lập và thử chiến
   lược tiến về phía trước trong môi trường dựng mới và staging.
3. **Đảm bảo bền dữ liệu:** sửa contract deadline/write error, thêm kiểm thử
   theo tài khoản và tình huống ghi lỗi.
4. **Hoàn thiện nghiệp vụ lịch:** đưa calendar có ngày tin cậy vào schema/API/UI,
   rồi nghiệm thu với dữ liệu học vụ và ngoại lệ.
5. **Nhất quán vai trò và tài liệu:** sửa sync role, rà soát README liên quan
   sau khi các thay đổi được kiểm chứng.

**Đã thực hiện:** chỉ bổ sung thông tin thực thi có thể xác nhận từ source vào
hai tài liệu `docs/data/`; chưa sửa code hoặc SQL, chưa xóa tài liệu. Các mức
ưu tiên phản ánh rủi ro tiềm tàng trong source, không phải xác nhận có sự cố
đã xảy ra ở production.
