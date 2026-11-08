# Lược đồ dữ liệu Supabase — UTH Connect

Tài liệu này là bản đồ tra cứu ngắn. **Các migration trong
`supabase/migrations/` là nguồn chính xác** về kiểu cột, constraint, index, trigger,
RPC và RLS. Không chạy lại migration đã áp dụng; xem
[`../../supabase/README.md`](../../supabase/README.md) trước khi thay đổi staging.

## Bảng hiện được tạo bởi migration

| Bảng | Mục đích / khóa chính | Ghi chú quyền và nguồn |
|---|---|---|
| `profiles` | Hồ sơ tài khoản; `id` tham chiếu `auth.users.id` | role, MSSV và trạng thái xác minh được kiểm soát server-side; RLS |
| `user_roles` | Vai trò gắn với `user_id`; unique `(user_id, role)` | RLS; trigger thêm role từ profile, hiện chưa gỡ role cũ khi đổi vai trò |
| `events` | Thông tin, thời gian, sức chứa, trạng thái, organizer | Quản lý bằng RPC; RLS; constraint trạng thái/sức chứa |
| `registrations` | Đăng ký sự kiện; khóa `(event_id, user_id)` | RPC đăng ký/hủy kiểm tra sức chứa và quyền |
| `attendance` | Check-in; khóa `(event_id, user_id)` | RPC check-in; migration bảo vệ lịch sử attendance |
| `notifications` | Thông báo chung hoặc giới hạn theo `recipient_id` | RLS; index theo category/pinned/published |
| `schedules` | Thời khóa biểu gắn profile/MSSV | RLS; index theo sinh viên, học kỳ, thứ và tiết; **chưa có ngày học/academic calendar** |
| `grades` | Điểm học tập gắn profile/MSSV | RLS; index theo sinh viên và học kỳ |
| `student_verification_requests` | Yêu cầu xác minh MSSV | RLS và RPC duyệt của admin |
| `housing_posts` | Tin tìm trọ/ở ghép | RLS; chính sách ghi theo chủ sở hữu và trạng thái xác minh |
| `lost_found` | Tin đồ thất lạc/tìm thấy | RLS; chính sách đọc/ghi theo trạng thái xác minh và chủ sở hữu |

Một số bảng lõi được tạo ban đầu trong
[`20261103000000_init_uth_connect.sql`](../../supabase/migrations/20261103000000_init_uth_connect.sql).
Các bảng `user_roles`, `housing_posts` và các chính sách liên quan nằm trong
[`20261107000000_auth_rbac_and_housing.sql`](../../supabase/migrations/20261107000000_auth_rbac_and_housing.sql);
`lost_found` được thêm bởi
[`20261107000000_lost_found_rls.sql`](../../supabase/migrations/20261107000000_lost_found_rls.sql).

## Luồng nghiệp vụ và bảo vệ dữ liệu

- `profiles.id` đồng nhất với UUID của Supabase Auth. Trigger tạo/cập nhật hồ sơ
  không xem signup metadata là bằng chứng xác minh MSSV.
- Migration xác thực mở rộng có trigger kiểm tra email thuộc `@ut.edu.vn` hoặc
  `@uth.edu.vn` khi tạo tài khoản hay đổi email; trạng thái xác minh MSSV vẫn
  do quy trình riêng xử lý. Đây là **định nghĩa trong source**; cần xác nhận
  migration đã được áp dụng và hành vi thực tế trên staging.
- `handle_profile_role_sync` hiện thêm role mới vào `user_roles` và cập nhật
  `auth.users.raw_app_meta_data`, nhưng không xóa role cũ khỏi `user_roles` khi
  hồ sơ đổi vai trò. `is_admin()` đang dựa trên `profiles.role`; không được
  suy luận `user_roles` luôn chứa đúng một vai trò hay dùng bảng này làm nguồn
  phân quyền mới trước khi xử lý sự lệch trạng thái.
- RPC quan trọng gồm `register_for_event`, `cancel_event_registration`,
  `issue_ticket_qr`, `check_in_ticket_qr`, xác minh sinh viên và quản lý sự kiện.
  Xem định nghĩa mới nhất trong migrations thay vì suy ra hợp đồng từ tên hàm.
- QR được migration `20261104000000_stable_ticket_qr.sql` ký bằng HMAC và hiện
  không mang TTL 60 giây. Check-in vẫn xác minh sự kiện, organizer, đăng ký và
  khung giờ; lần quét sau bị từ chối khi attendance đã tồn tại.
- Các bảng nghiệp vụ bật RLS. Quyền thực tế phụ thuộc policy, grants và RPC
  security context; chỉ nhìn thấy `ENABLE ROW LEVEL SECURITY` chưa đủ để kết luận
  một luồng đã an toàn.
- Schema có `app.qr_signing_keys` trong schema riêng `app`; đây là dữ liệu khóa
  ký server, không được đọc hoặc đưa vào client.
- Android gọi Supabase REST/RPC bằng publishable key và JWT người dùng. Không
  nhúng `service_role`/secret key vào APK hoặc web client.
- `schedules.day_of_week` biểu diễn lịch lặp theo tuần; schema không chứa
  ngày lên lớp hoặc quy tắc tuần nghỉ. Repository Android trả rỗng cho tra cứu
  ngày cụ thể để tránh bịa lịch. Deadline tự tạo thuộc kho dữ liệu cục bộ theo
  Supabase UUID, **không phải** bảng trong schema Supabase hiện tại.

## Trạng thái migration cần xử lý trước khi triển khai

- Hai tập tin có cùng version `20261104000000`:
  [`auth_security_expansion`](../../supabase/migrations/20261104000000_auth_security_expansion.sql)
  và [`stable_ticket_qr`](../../supabase/migrations/20261104000000_stable_ticket_qr.sql).
  Hai tập tin khác có cùng version `20261107000000`:
  [`auth_rbac_and_housing`](../../supabase/migrations/20261107000000_auth_rbac_and_housing.sql)
  và [`lost_found_rls`](../../supabase/migrations/20261107000000_lost_found_rls.sql).
  Cần kiểm tra `supabase_migrations.schema_migrations` ở môi trường đích và
  hành vi Supabase CLI trước khi quyết định cách sửa lịch sử; **không tự đổi
  tên/chạy lại migration đã áp dụng**.
- `20261107000000_auth_rbac_and_housing.sql` đang cấp `admin` dựa vào danh
  sách email viết cứng trong `handle_new_user` nếu email được xác nhận. Đây là
  điểm cần kiểm toán và thay bằng quy trình bootstrap/duyệt quyền tin cậy,
  ngay cả khi trigger kiểm tra miền email giới hạn một số đường đăng ký.
- Các vấn đề và tiêu chí nghiệm thu chi tiết được ghi tại
  [`../reports/DATA_CODE_ALIGNMENT_AUDIT.md`](../reports/DATA_CODE_ALIGNMENT_AUDIT.md).

## Ngoài phạm vi schema hiện hành

Không thấy migration tạo các bảng Firestore cũ, Cloud Functions hay
`user_fcm_tokens`. FCM Topics là quyết định kiến trúc riêng và không đồng nghĩa
đã có bảng token hoặc pipeline gửi push. Các module có repository giả/tĩnh cũng
không chứng minh dữ liệu tương ứng đã tồn tại trên Supabase.

## Cách xác minh trước khi sửa

1. Đọc migration mới nhất có liên quan và migration lịch sử mà nó thay thế.
2. Kiểm tra `supabase/README.md` để xác định thứ tự áp dụng và trạng thái staging.
3. Thử quyền/RPC trên staging bằng user sở hữu, user khác vai trò và dữ liệu biên.
4. Cập nhật tài liệu này khi schema đã được chốt; không sửa migration đã chạy,
   hãy tạo migration tiến về phía trước.
