-- ============================================================================
-- UTH Connect — Schema Lost & Found (Đồ thất lạc) + Storage Bucket
-- ----------------------------------------------------------------------------
-- Module: LOST & FOUND (KAN-92)
-- Chuyển đổi lưu trữ từ InMemory sang cơ sở dữ liệu Supabase Postgres và
-- Supabase Storage bucket 'lost-found' để lưu trữ hình ảnh minh chứng.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Bảng public.lost_found_items
-- ---------------------------------------------------------------------------
create table if not exists public.lost_found_items (
    id              uuid primary key default gen_random_uuid(),
    title           text not null check (char_length(trim(title)) >= 3),
    description     text not null default '',
    kind            text not null check (kind in ('LOST', 'FOUND')),
    category        text not null check (category in ('ALL', 'STUDENT_CARD', 'ELECTRONICS', 'WALLET_PAPERS', 'PERSONAL_ITEMS', 'OTHER')),
    location        text not null check (char_length(trim(location)) > 0),
    date            text not null,
    contact_name    text not null check (char_length(trim(contact_name)) > 0),
    contact_phone   text not null check (char_length(trim(contact_phone)) >= 9),
    image_url       text,
    is_resolved     boolean not null default false,
    user_id         uuid references auth.users(id) on delete set null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

-- Index tăng tốc truy vấn bảng tin và bộ lọc
create index if not exists idx_lost_found_created_at on public.lost_found_items (created_at desc);
create index if not exists idx_lost_found_kind_category on public.lost_found_items (kind, category);
create index if not exists idx_lost_found_is_resolved on public.lost_found_items (is_resolved);

-- ---------------------------------------------------------------------------
-- 2. Row Level Security (RLS) cho lost_found_items
-- ---------------------------------------------------------------------------
alter table public.lost_found_items enable row level security;

-- Mọi người (cả khách vãng lai và sinh viên đã đăng nhập) đều có thể xem danh sách đồ thất lạc
drop policy if exists "lost_found_select_all" on public.lost_found_items;
create policy "lost_found_select_all"
    on public.lost_found_items
    for select
    using (true);

-- Cho phép đăng tin mất đồ / nhặt được đồ (chỉ người dùng đã đăng nhập)
drop policy if exists "lost_found_insert_all" on public.lost_found_items;
create policy "lost_found_insert_all"
    on public.lost_found_items
    for insert
    to authenticated
    with check (user_id = (select auth.uid()));

-- Cho phép cập nhật trạng thái đã nhận lại / đã trao trả (chỉ chủ sở hữu tin)
drop policy if exists "lost_found_update_all" on public.lost_found_items;
create policy "lost_found_update_all"
    on public.lost_found_items
    for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- Cho phép xóa tin (chỉ chủ sở hữu tin)
drop policy if exists "lost_found_delete_all" on public.lost_found_items;
create policy "lost_found_delete_all"
    on public.lost_found_items
    for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- 3. Tạo Supabase Storage bucket 'lost-found'
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('lost-found', 'lost-found', true, 5242880, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update set
    public = true,
    file_size_limit = 5242880,
    allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp'];

-- Policy đọc ảnh công khai từ bucket lost-found
drop policy if exists "lost_found_storage_public_select" on storage.objects;
create policy "lost_found_storage_public_select"
    on storage.objects
    for select
    using (bucket_id = 'lost-found');

-- Policy tải ảnh lên bucket lost-found (chỉ người dùng đã xác thực)
drop policy if exists "lost_found_storage_insert" on storage.objects;
create policy "lost_found_storage_insert"
    on storage.objects
    for insert
    to authenticated
    with check (
        bucket_id = 'lost-found'
        and (storage.foldername(name))[1] = 'items'
        and (storage.foldername(name))[2] = (select auth.uid())::text
    );

-- Policy xa ?nh t? bucket lost-found (ch? ch? s? h?u ?nh)
drop policy if exists "lost_found_storage_delete" on storage.objects;
create policy "lost_found_storage_delete"
    on storage.objects
    for delete
    to authenticated
    using (
        bucket_id = 'lost-found'
        and (storage.foldername(name))[1] = 'items'
        and (storage.foldername(name))[2] = (select auth.uid())::text
    );
