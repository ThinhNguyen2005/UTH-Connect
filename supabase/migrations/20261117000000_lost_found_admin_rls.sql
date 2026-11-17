-- ============================================================================
-- UTH Connect — Nâng cấp RLS cho bảng public.lost_found_items (KAN-92 / KAN-107)
-- Cấp quyền cho Quản trị viên (Admin) và Ban Tổ chức (Organizer) toàn quyền
-- kiểm duyệt, cập nhật trạng thái đã trao trả và xóa bài viết vi phạm.
-- ============================================================================

-- 1. Cập nhật chính sách SELECT cho public.lost_found_items
-- Chỉ cho phép người dùng đã đăng nhập (authenticated) đọc tin để bảo vệ
-- thông tin số điện thoại và tên liên hệ của sinh viên, ngăn chặn quét dữ liệu nặc danh (anon).
drop policy if exists "lost_found_select_all" on public.lost_found_items;
create policy "lost_found_select_all"
    on public.lost_found_items
    for select
    to authenticated
    using (true);

-- 2. Cập nhật chính sách UPDATE cho public.lost_found_items
-- Cho phép cập nhật nếu:
--   a) Là chủ sở hữu bài đăng: user_id = auth.uid()
--   b) HOẶC là Admin: public.is_admin()
--   c) HOẶC là Ban Tổ chức / Giảng viên: public.is_organizer()
drop policy if exists "lost_found_update_all" on public.lost_found_items;
create policy "lost_found_update_all"
    on public.lost_found_items
    for update
    to authenticated
    using (
        user_id = (select auth.uid())
        or public.is_admin()
        or public.is_organizer()
    )
    with check (
        user_id = (select auth.uid())
        or public.is_admin()
        or public.is_organizer()
    );

-- 3. Cập nhật chính sách DELETE cho public.lost_found_items
-- Cho phép xóa nếu:
--   a) Là chủ sở hữu bài đăng: user_id = auth.uid()
--   b) HOẶC là Admin: public.is_admin()
--   c) HOẶC là Ban Tổ chức: public.is_organizer()
drop policy if exists "lost_found_delete_all" on public.lost_found_items;
create policy "lost_found_delete_all"
    on public.lost_found_items
    for delete
    to authenticated
    using (
        user_id = (select auth.uid())
        or public.is_admin()
        or public.is_organizer()
    );
