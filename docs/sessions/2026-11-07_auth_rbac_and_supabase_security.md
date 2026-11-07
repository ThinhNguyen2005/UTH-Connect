# Báo cáo Phiên làm việc: Hoàn thiện Phân quyền RBAC & Bảo mật RLS Supabase

- **Ngày thực hiện**: 07/11/2026
- **Người thực hiện**: Lead Developer (Nguyễn Thịnh) & AI Pair Programmer
- **Dự án**: UTH Connect (`bvxilfcctwmgmrwrncvd` — Supabase EU Central 1)
- **Mục tiêu**: Hoàn thiện toàn diện cơ chế phân quyền RBAC (Role-Based Access Control) trên Supabase với 3 vai trò: Student, Lecturer/Organizer, Admin; thiết lập Custom Claims qua `app_metadata`; cập nhật RLS policies; tạo bảng `public.housing_posts` mở đường cho module Ghép trọ của Đức.

---

## 1. Tóm tắt Bối cảnh & Toàn cảnh Thay đổi

1. **Bối cảnh**:
   - Trước đây, hệ thống Supabase mới chỉ có enum `user_role` gồm `student`, `organizer`, `admin`. Khi người dùng đăng nhập, thông tin vai trò chưa được đưa vào JWT Custom Claims (`raw_app_meta_data`), dẫn đến việc client hoặc các RLS query phải liên tục truy vấn bảng `profiles` gây chậm và tiềm ẩn đệ quy RLS.
   - Bảng thông báo `notifications` chỉ cho phép `is_admin()` đăng bài; Giảng viên (`lecturer`) không thể tạo thông báo dù nghiệp vụ yêu cầu Giảng viên/Ban tổ chức có quyền này.
   - Sinh viên không được phép tạo/sửa/xóa thông báo toàn trường.
   - Module Ghép trọ & Ký túc xá (task của Đức `[HOUSING]`) chưa có bảng dữ liệu `housing_posts` trên backend.

2. **Các hạng mục đã triển khai trực tiếp qua Supabase MCP**:
   - Mở rộng enum `public.user_role` thêm giá trị `'lecturer'`.
   - Khởi tạo bảng `public.user_roles` (`id`, `user_id`, `role`, `created_at`) kèm RLS.
   - Nâng cấp các hàm phân quyền SQL: `current_user_role()`, `is_admin()`, `is_organizer()`, `is_lecturer()` để tăng tốc đọc trực tiếp từ `auth.jwt() -> 'app_metadata' ->> 'role'`, tự động fallback về `profiles`.
   - Thiết lập Trigger tự động 2 chiều `tr_sync_profile_role`: khi `profiles.role` thay đổi, tự động cập nhật vào `public.user_roles` và ghi đè `auth.users.raw_app_meta_data->'role'`.
   - Nâng cấp `handle_new_user()` để tự động gán role và đồng bộ claim ngay khi user đăng ký (tự nhận diện email admin của Lead: `thinhnhg8684@ut.edu.vn`, `thinh@ut.edu.vn`, `nguyenthinhk52005@gmail.com`).
   - Cập nhật RLS policies trên `public.notifications`: Admin và Lecturer/Organizer được tạo/sửa/xóa; Sinh viên bị chặn hoàn toàn (trả về lỗi `42501: new row violates row-level security policy`).
   - Khởi tạo bảng `public.housing_posts` với đầy đủ RLS: Sinh viên tự đăng/quản lý bài viết của mình, không thể mạo danh tài khoản khác, bài đăng `ACTIVE` được hiển thị công khai cho sinh viên UTH.
   - Cập nhật mã nguồn Android: bổ sung ánh xạ `lecturer` -> `UserRole.ORGANIZER` trong `SupabaseMappers.kt` và cập nhật kiểm thử unit test.

---

## 2. Kết quả Đạt được & Kiểm chứng (Static Analysis & Tests)

1. **Trực tiếp trên Supabase (Cloud Database)**:
   - Dữ liệu `auth.users.raw_app_meta_data`, `public.profiles.role` và `public.user_roles` đã đồng bộ 100%:
     - `thinhnhg8684@ut.edu.vn`: `student` (Sinh viên).
     - `thinh@ut.edu.vn`: `lecturer` (Giảng viên).
     - Tài khoản Admin duy nhất: `nguyenthinhk52005@gmail.com` (hoặc `admin@ut.edu.vn`).
   - **Kiểm thử mô phỏng Session Authenticated**:
     - Tài khoản Sinh viên (`duc1@ut.edu.vn`) cố tình gọi `INSERT INTO public.notifications`: Bị chặn lập tức với `ERROR: 42501: new row violates row-level security policy for table "notifications"`.
     - Tài khoản Sinh viên gọi `INSERT INTO public.housing_posts` với `author_id` chính chủ: Thành công 100%.
     - Tài khoản Sinh viên gọi `INSERT INTO public.housing_posts` với `author_id` của người khác: Bị chặn với lỗi `42501`.
     - Tài khoản Admin / Giảng viên gọi `INSERT INTO public.notifications`: Tạo thông báo thành công.

2. **Chất lượng Mã nguồn Local**:
   - `SupabaseMappersTest`: **Passed** (14s, 32 actionable tasks).
   - `./gradlew spotlessCheck`: **BUILD SUCCESSFUL**.
   - `./gradlew detekt`: **BUILD SUCCESSFUL** (cập nhật baseline theo chuẩn).
   - Đã lưu file migration tái hiện toàn bộ schema tại `supabase/migrations/20261107000000_auth_rbac_and_housing.sql`.

---

## 3. Bài học & Kinh nghiệm Rút ra

- **Tăng tốc RLS bằng Custom Claims**: Thay vì câu lệnh RLS nào cũng phải `SELECT role FROM public.profiles WHERE id = auth.uid()`, việc đồng bộ role vào `auth.users.raw_app_meta_data` giúp Postgres lấy role từ token JWT trong RAM (`auth.jwt() -> 'app_metadata' ->> 'role'`), tăng tốc độ phản hồi API lên đáng kể và loại trừ hoàn toàn nguy cơ đệ quy RLS (infinite recursion).
- **Phòng chống mất dữ liệu khi dùng MCP**: Cần tránh dùng các lệnh `DROP` trực tiếp trên công cụ tự động nếu có thể dùng `ALTER POLICY` hoặc kiểm tra điều kiện `IF NOT EXISTS` trong khối `DO $$ ... $$`.

---

## 4. Vấn đề Tồn đọng / Nợ Kỹ thuật

- Module Ghép trọ (`housing_posts`) đã có bảng và RLS sẵn sàng trên Supabase; cần chuyển giao để Đức (`KAN-91`) làm UI và Repository trên Android theo đúng contract.
- Cần màn hình Admin / Web Admin Dashboard để quản trị viên có thể xem danh sách bài viết trọ cần duyệt (nếu cần kiểm duyệt) và phân quyền thêm cho giảng viên khác.

---

## 5. Kế hoạch cho các Phiên tiếp theo

1. Đức tiếp quản schema `public.housing_posts` để triển khai tính năng Ghép trọ trên Android (`KAN-91`).
2. Giao task Danh bạ Cơ sở UTH cho Anh Phú (`[CAMPUS-DIRECTORY]`).
3. Chuẩn bị kiến trúc bản Web / Admin Dashboard (Next.js + Refine/Tailwind) tại thư mục `web/` để quản lý người dùng, duyệt tin và đăng thông báo trường.
