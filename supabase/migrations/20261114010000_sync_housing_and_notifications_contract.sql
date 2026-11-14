-- ============================================================================
-- Migration: 20261114010000_sync_housing_and_notifications_contract.sql
-- Mục đích: Đồng bộ cấu hình dữ liệu giữa Web Admin (Next.js) và Android (Kotlin)
-- ============================================================================

-- 1. BẢNG HOUSING_POSTS: Đồng bộ cột image_urls và trạng thái kiểm duyệt
-- ----------------------------------------------------------------------------

-- Thêm cột image_urls nếu chưa có (chuyển dữ liệu từ cột images sang nếu có)
do $$
begin
    if not exists (
        select 1 from information_schema.columns 
        where table_name = 'housing_posts' and column_name = 'image_urls'
    ) then
        alter table public.housing_posts add column image_urls text[] not null default '{}';
        update public.housing_posts set image_urls = images where images is not null and images <> '{}';
    end if;
end $$;

-- Thêm cột lý do từ chối kiểm duyệt
alter table public.housing_posts add column if not exists rejection_reason text default null;

-- Nâng cấp Check Constraint cho cột status hỗ trợ các trạng thái kiểm duyệt
alter table public.housing_posts drop constraint if exists housing_posts_status_check;
alter table public.housing_posts 
    add constraint housing_posts_status_check 
    check (status in ('pending', 'approved', 'rejected', 'fulfilled', 'OPEN', 'HIDDEN_BY_REPORTS'));

-- Cập nhật trigger kiểm tra URL ảnh áp dụng cho cả images và image_urls
create or replace function public.trg_housing_url_guards()
returns trigger
language plpgsql
as $$
declare
    v_image text;
begin
    -- Đồng bộ hai chiều giữa images và image_urls để tương thích ngược 100%
    if new.image_urls is not null and new.image_urls <> '{}' and (new.images is null or new.images = '{}') then
        new.images := new.image_urls;
    elsif new.images is not null and new.images <> '{}' and (new.image_urls is null or new.image_urls = '{}') then
        new.image_urls := new.images;
    end if;

    -- Kiểm tra regex bucket cho toàn bộ URL ảnh
    foreach v_image in array coalesce(new.image_urls, new.images, '{}') loop
        if v_image !~ '^https://[a-z0-9-]+\\.supabase\\.co/storage/v1/object/public/housing-images/' then
            raise exception 'HOUSING_IMAGE_BUCKET_INVALID' using errcode = 'P0001';
        end if;
    end loop;

    -- Kiểm tra link Zalo
    if coalesce(new.contact_zalo_url, '') <> ''
        and new.contact_zalo_url !~* '^https://zalo\\.me(/|$)' then
        raise exception 'HOUSING_ZALO_URL_INVALID' using errcode = 'P0001';
    end if;

    return new;
end;
$$;


-- 2. BẢNG NOTIFICATIONS: Hỗ trợ thông báo cá nhân (Personal Notifications Hub)
-- ----------------------------------------------------------------------------

alter table public.notifications 
    add column if not exists user_id uuid references public.profiles(id) on delete cascade,
    add column if not exists type text not null default 'SYSTEM',
    add column if not exists is_read boolean not null default false;

-- Đồng bộ user_id với recipient_id cũ nếu có
update public.notifications 
set user_id = recipient_id 
where user_id is null and recipient_id is not null;

-- Tạo index tăng tốc truy vấn thông báo chưa đọc của người dùng
create index if not exists notifications_user_id_unread_idx 
    on public.notifications (user_id, is_read, created_at desc);

-- Cập nhật RLS Policies cho bảng notifications
alter table public.notifications enable row level security;

-- Policy Select: Người dùng xem thông báo của mình hoặc thông báo chung toàn trường
drop policy if exists "notifications_select" on public.notifications;
create policy "notifications_select" on public.notifications
    for select to authenticated
    using (
        user_id = (select auth.uid()) 
        or recipient_id = (select auth.uid()) 
        or recipient_id is null 
        or (select is_admin())
    );

-- Policy Update: Người dùng được cập nhật trạng thái đã đọc (is_read) cho thông báo của chính mình
drop policy if exists "notifications_update" on public.notifications;
create policy "notifications_update" on public.notifications
    for update to authenticated
    using (
        user_id = (select auth.uid()) 
        or recipient_id = (select auth.uid()) 
        or (select is_admin() or is_organizer())
    )
    with check (
        user_id = (select auth.uid()) 
        or recipient_id = (select auth.uid()) 
        or (select is_admin() or is_organizer())
    );
