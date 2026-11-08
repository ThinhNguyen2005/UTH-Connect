-- ============================================================================
-- UTH Connect — Phân quyền Người dùng (RBAC) & Custom Claims & Module Ghép trọ
-- Migration: 20261107000000_auth_rbac_and_housing.sql
-- ----------------------------------------------------------------------------
-- Mục đích:
-- 1. Bổ sung giá trị 'lecturer' vào kiểu enum public.user_role.
-- 2. Tạo bảng public.user_roles và thiết lập RLS.
-- 3. Nâng cấp các hàm xác thực: current_user_role(), is_admin(), is_organizer(),
--    is_lecturer() hỗ trợ tăng tốc bằng JWT Custom Claims (auth.jwt() -> 'app_metadata' ->> 'role')
--    kèm cơ chế dự phòng tra cứu profiles.
-- 4. Tạo trigger tr_sync_profile_role tự động đồng bộ vai trò từ profiles sang
--    user_roles và auth.users.raw_app_meta_data.
-- 5. Cập nhật RLS notifications: chỉ Admin và Giảng viên / BTC mới có quyền tạo, sửa,
--    xóa thông báo toàn trường. Sinh viên bị chặn nghiêm ngặt (42501).
-- 6. Khởi tạo bảng public.housing_posts (Ghép trọ & KTX) với đầy đủ RLS.
-- ============================================================================

-- 1. Kiểu enum
alter type public.user_role add value if not exists 'lecturer';

-- 2. Bảng user_roles
create table if not exists public.user_roles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  role public.user_role not null default 'student',
  created_at timestamptz not null default now(),
  unique (user_id, role)
);
create index if not exists user_roles_user_id_idx on public.user_roles (user_id);
alter table public.user_roles enable row level security;

do $$
begin
  if not exists (select 1 from pg_policies where tablename = 'user_roles' and policyname = 'user_roles_select') then
    create policy user_roles_select on public.user_roles
      for select to authenticated
      using (user_id = (select auth.uid()) or (select public.is_admin()));
  end if;

  if not exists (select 1 from pg_policies where tablename = 'user_roles' and policyname = 'user_roles_admin_all') then
    create policy user_roles_admin_all on public.user_roles
      for all to authenticated
      using (public.is_admin())
      with check (public.is_admin());
  end if;
end $$;

-- 3. Hàm phân quyền hỗ trợ tăng tốc bằng JWT Custom Claims
create or replace function public.current_user_role() returns text
language sql stable security definer set search_path = ''
as $$
  select coalesce(
    nullif(auth.jwt() -> 'app_metadata' ->> 'role', ''),
    (select p.role::text from public.profiles p where p.id = (select auth.uid())),
    'student'
  );
$$;

create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.profiles p where p.id = (select auth.uid()) and p.role::text = 'admin'
  );
$$;

create or replace function public.is_organizer() returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.profiles p where p.id = (select auth.uid()) and p.role::text in ('admin', 'organizer', 'lecturer')
  );
$$;

create or replace function public.is_lecturer() returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.profiles p where p.id = (select auth.uid()) and p.role::text in ('admin', 'lecturer', 'organizer')
  );
$$;

-- 4. Đồng bộ Role tự động
create or replace function public.handle_profile_role_sync()
returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.user_roles(user_id, role)
  values (new.id, new.role)
  on conflict (user_id, role) do nothing;

  update auth.users
  set raw_app_meta_data = coalesce(raw_app_meta_data, '{}'::jsonb) || jsonb_build_object('role', new.role::text)
  where id = new.id;

  return new;
end;
$$;

do $$ 
begin 
  if not exists (select 1 from pg_trigger where tgname = 'tr_sync_profile_role') then 
    create trigger tr_sync_profile_role
      after insert or update of role on public.profiles
      for each row execute function public.handle_profile_role_sync(); 
  end if; 
end $$;

create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
  v_role public.user_role := 'student';
begin
  if new.email_confirmed_at is not null and new.email in ('thinhnhg8684@ut.edu.vn', 'thinh@ut.edu.vn', 'nguyenthinhk52005@gmail.com', 'admin@ut.edu.vn') then
    v_role := 'admin'::public.user_role;
  end if;

  insert into public.profiles(id, email, display_name, avatar_url, role)
  values (
    new.id,
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data->>'displayName',
             new.raw_user_meta_data->>'full_name',
             split_part(coalesce(new.email, ''), '@', 1)),
    new.raw_user_meta_data->>'avatarUrl',
    v_role
  )
  on conflict (id) do update set
    email = excluded.email,
    display_name = coalesce(nullif(excluded.display_name, ''), public.profiles.display_name),
    avatar_url = coalesce(excluded.avatar_url, public.profiles.avatar_url);

  insert into public.user_roles(user_id, role)
  values (new.id, v_role)
  on conflict (user_id, role) do nothing;

  new.raw_app_meta_data := coalesce(new.raw_app_meta_data, '{}'::jsonb) || jsonb_build_object('role', v_role::text);

  return new;
end;
$$;

-- 5. RLS notifications: Giảng viên & Admin được đăng/sửa/xóa, Sinh viên bị chặn
alter policy notifications_insert on public.notifications
  with check (public.is_admin() or public.is_organizer());

alter policy notifications_update on public.notifications
  using (public.is_admin() or public.is_organizer())
  with check (public.is_admin() or public.is_organizer());

alter policy notifications_delete on public.notifications
  using (public.is_admin() or public.is_organizer());

-- 6. Khởi tạo bảng public.housing_posts (Module Ghép trọ & KTX)
create table if not exists public.housing_posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles (id) on delete cascade,
  title text not null check (length(btrim(title)) >= 5 and length(title) <= 200),
  description text not null default '',
  price numeric(12, 0) not null default 0 check (price >= 0),
  location text not null default '',
  campus text not null default 'CS1',
  room_type text not null default 'roommate' check (room_type in ('roommate', 'rental', 'dormitory')),
  contact_phone text,
  contact_zalo text,
  images text[] default '{}',
  status text not null default 'ACTIVE' check (status in ('ACTIVE', 'CLOSED', 'HIDDEN')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists housing_posts_author_id_idx on public.housing_posts (author_id);
create index if not exists housing_posts_campus_status_idx on public.housing_posts (campus, status, created_at desc);
create index if not exists housing_posts_active_idx on public.housing_posts (created_at desc) where status = 'ACTIVE';

alter table public.housing_posts enable row level security;

do $$
begin
  if not exists (select 1 from pg_policies where tablename = 'housing_posts' and policyname = 'housing_posts_select') then
    create policy housing_posts_select on public.housing_posts
      for select to authenticated
      using (status = 'ACTIVE' or author_id = (select auth.uid()) or (select public.is_admin()));
  end if;

  if not exists (select 1 from pg_policies where tablename = 'housing_posts' and policyname = 'housing_posts_insert') then
    create policy housing_posts_insert on public.housing_posts
      for insert to authenticated
      with check (author_id = (select auth.uid()));
  end if;

  if not exists (select 1 from pg_policies where tablename = 'housing_posts' and policyname = 'housing_posts_update') then
    create policy housing_posts_update on public.housing_posts
      for update to authenticated
      using (
        (select public.is_admin()) or
        (author_id = (select auth.uid()) and status != 'HIDDEN')
      )
      with check (
        (select public.is_admin()) or
        (author_id = (select auth.uid()) and status in ('ACTIVE', 'CLOSED'))
      );
  end if;

  if not exists (select 1 from pg_policies where tablename = 'housing_posts' and policyname = 'housing_posts_delete') then
    create policy housing_posts_delete on public.housing_posts
      for delete to authenticated
      using (author_id = (select auth.uid()) or (select public.is_admin()));
  end if;
end $$;
