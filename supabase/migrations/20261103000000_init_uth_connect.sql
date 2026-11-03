-- ============================================================================
-- UTH Connect — Khởi tạo schema Supabase (Postgres) + Row Level Security
-- ----------------------------------------------------------------------------
-- Mục đích: chuyển mô hình dữ liệu Cloud Firestore (xem docs/DATABASE_SCHEMA.md)
-- và luật bảo mật (firestore.rules) sang Supabase Postgres.
--
-- Tuân thủ Supabase agent skill "supabase" (Security Checklist) và
-- "supabase-postgres-best-practices":
--   * Bật RLS trên MỌI bảng ở schema public (checklist #5).
--   * Chính sách dùng `TO authenticated` + vị từ quyền sở hữu, KHÔNG dùng
--     auth.role() (đã deprecated). UPDATE luôn có cả USING và WITH CHECK.
--   * Vai trò (role) là nguồn sự thật ở public.profiles, KHÔNG đọc từ
--     user_metadata (checklist: user_metadata do user tự sửa được).
--   * Hàm quyền là SECURITY INVOKER; hàm nghiệp vụ chạy quyền cao đặt ở schema
--     riêng `app` (không expose qua Data API) và dùng SECURITY DEFINER.
--   * Khoá chính uuid, cột FK được index, timestamp có timezone, điểm dùng numeric.
--   * Composite index mô phỏng firestore.indexes.json.
--
-- Cách áp dụng: xem mục "Apply & verify" ở cuối file hoặc README ở chat.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 0. Kiểu enum
-- ---------------------------------------------------------------------------
create type public.user_role           as enum ('student', 'organizer', 'admin');
create type public.event_status        as enum ('UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED');
create type public.registration_status as enum ('REGISTERED', 'CANCELLED');
create type public.attendance_status   as enum ('PRESENT', 'ABSENT');
create type public.notification_category as enum ('ACADEMIC', 'EXAMINATION', 'TUITION', 'ACTIVITIES', 'GENERAL');

-- Schema riêng cho hàm nghiệp vụ SECURITY DEFINER (không truy cập được qua REST).
create schema if not exists app;
revoke all on schema app from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- 1. Bảng
-- ---------------------------------------------------------------------------

-- profiles: thông tin tài khoản, id đồng nhất với auth.users.id
create table public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  student_id   text unique,                       -- MSSV
  email        text not null,
  display_name text not null default '',
  class_name   text,
  major        text,
  faculty      text,
  avatar_url   text,
  phone_number text,
  role         public.user_role not null default 'student',
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);
-- student_id là cột đối chiếu quyền sở hữu ở schedules/grades -> index
create index profiles_student_id_idx on public.profiles (student_id);

-- events
create table public.events (
  id                    uuid primary key default gen_random_uuid(),
  title                 text not null,
  description           text not null default '',
  banner_url            text,
  location              text,
  campus                text,
  start_at              timestamptz not null,
  end_at                timestamptz not null,
  registration_deadline timestamptz,
  max_participants      integer not null default 0 check (max_participants >= 0),
  current_registered    integer not null default 0 check (current_registered >= 0),
  points                integer not null default 0,
  status                public.event_status not null default 'UPCOMING',
  organizer_id          uuid references public.profiles (id) on delete set null,
  organizer_name        text,
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now(),
  check (end_at >= start_at),
  check (current_registered <= max_participants)
);
create index events_organizer_id_idx        on public.events (organizer_id);       -- index FK
create index events_campus_status_start_idx on public.events (campus, status, start_at);
create index events_status_start_idx        on public.events (status, start_at);
-- Partial index cho luồng phổ biến nhất: xem sự kiện sắp diễn ra (nguồn: query-partial-indexes)
create index events_upcoming_start_idx      on public.events (start_at) where status = 'UPCOMING';

-- registrations (subcollection events/{id}/registrations/{uid})
create table public.registrations (
  event_id      uuid not null references public.events (id) on delete cascade,
  user_id       uuid not null references public.profiles (id) on delete cascade,
  student_id    text,
  display_name  text,
  email         text,
  status        public.registration_status not null default 'REGISTERED',
  registered_at timestamptz not null default now(),
  primary key (event_id, user_id)                -- event_id là khoá trái của PK -> đã có index
);
create index registrations_user_id_idx on public.registrations (user_id);  -- index FK

-- attendance (subcollection events/{id}/attendance/{uid})
create table public.attendance (
  event_id      uuid not null references public.events (id) on delete cascade,
  user_id       uuid not null references public.profiles (id) on delete cascade,
  checked_in_by uuid references public.profiles (id) on delete set null,
  status        public.attendance_status not null default 'PRESENT',
  checked_in_at timestamptz not null default now(),
  primary key (event_id, user_id)
);
create index attendance_user_id_idx        on public.attendance (user_id);        -- index FK
create index attendance_checked_in_by_idx  on public.attendance (checked_in_by);  -- index FK

-- notifications
create table public.notifications (
  id             uuid primary key default gen_random_uuid(),
  title          text not null,
  summary        text not null default '',
  content        text not null default '',
  category       public.notification_category not null default 'GENERAL',
  author         text,
  published_at   timestamptz not null default now(),
  is_pinned      boolean not null default false,
  attachment_url text,
  image_url      text,
  created_at     timestamptz not null default now()
);
create index notifications_category_pinned_published_idx
  on public.notifications (category, is_pinned desc, published_at desc);
create index notifications_pinned_published_idx
  on public.notifications (is_pinned desc, published_at desc);

-- schedules (thời khoá biểu)
create table public.schedules (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid references public.profiles (id) on delete cascade,  -- liên kết sở hữu
  student_id   text not null,
  semester     text,
  course_code  text,
  course_name  text,
  lecturer_name text,
  room         text,
  campus       text,
  day_of_week  integer check (day_of_week between 2 and 8),
  start_period integer check (start_period between 1 and 12),
  end_period   integer check (end_period between 1 and 12),
  start_time   text,
  end_time     text,
  credits      integer,
  check (end_period >= start_period)
);
create index schedules_user_id_idx on public.schedules (user_id);  -- index FK
create index schedules_student_semester_day_idx
  on public.schedules (student_id, semester, day_of_week, start_period);

-- grades (bảng điểm)
create table public.grades (
  id               uuid primary key default gen_random_uuid(),
  user_id          uuid references public.profiles (id) on delete cascade,
  student_id       text not null,
  semester         text,
  course_code      text,
  course_name      text,
  credits          integer,
  attendance_score numeric(4,1),
  midterm_score    numeric(4,1),
  final_score      numeric(4,1),
  total_score_10   numeric(4,1),
  total_score_4    numeric(4,1),
  letter_grade     text
);
create index grades_user_id_idx on public.grades (user_id);  -- index FK
create index grades_student_semester_idx
  on public.grades (student_id, semester);

-- ---------------------------------------------------------------------------
-- 2. Hàm quyền (SECURITY INVOKER — chạy dưới quyền người gọi, đọc profiles
--    vốn đã cho phép authenticated đọc). Bao bọc `(select auth.uid())` để
--    Postgres cache thay vì gọi theo từng dòng.
-- ---------------------------------------------------------------------------
create or replace function public.is_admin()
returns boolean
language sql stable
set search_path = ''
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.role = 'admin'
  );
$$;

create or replace function public.is_organizer()
returns boolean
language sql stable
set search_path = ''
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.role in ('admin', 'organizer')
  );
$$;

create or replace function public.my_student_id()
returns text
language sql stable
set search_path = ''
as $$
  select p.student_id from public.profiles p
  where p.id = (select auth.uid());
$$;

-- ---------------------------------------------------------------------------
-- 3. Bật RLS trên mọi bảng public
-- ---------------------------------------------------------------------------
alter table public.profiles      enable row level security;
alter table public.events        enable row level security;
alter table public.registrations enable row level security;
alter table public.attendance    enable row level security;
alter table public.notifications enable row level security;
alter table public.schedules     enable row level security;
alter table public.grades        enable row level security;

-- ---------------------------------------------------------------------------
-- 4. Chính sách RLS (cảng ports từ firestore.rules)
-- ---------------------------------------------------------------------------

-- profiles: mọi người đã đăng nhập đọc được profile cơ bản; chỉ tự sửa hoặc admin
create policy profiles_select on public.profiles
  for select to authenticated using (true);
create policy profiles_insert on public.profiles
  for insert to authenticated with check ((select auth.uid()) = id);
create policy profiles_update on public.profiles
  for update to authenticated
  using  ( (select auth.uid()) = id or public.is_admin() )
  with check ( (select auth.uid()) = id or public.is_admin() );

-- events: mọi sinh viên đọc; chỉ BTC/Admin tạo & sửa; không xoá hộ sự kiện của người khác
create policy events_select on public.events
  for select to authenticated using (true);
create policy events_insert on public.events
  for insert to authenticated
  with check ( public.is_admin() or (public.is_organizer() and organizer_id = (select auth.uid())) );
create policy events_update on public.events
  for update to authenticated
  using  ( public.is_admin() or (public.is_organizer() and organizer_id = (select auth.uid())) )
  with check ( public.is_admin() or (public.is_organizer() and organizer_id = (select auth.uid())) );
create policy events_delete on public.events
  for delete to authenticated
  using  ( public.is_admin() or (public.is_organizer() and organizer_id = (select auth.uid())) );

-- registrations: đọc công khai; sinh viên tự đăng ký/huỷ; BTC cập nhật trạng thái
create policy registrations_select on public.registrations
  for select to authenticated using (true);
create policy registrations_insert on public.registrations
  for insert to authenticated with check ( user_id = (select auth.uid()) );
create policy registrations_update on public.registrations
  for update to authenticated
  using  ( user_id = (select auth.uid()) or public.is_organizer() )
  with check ( user_id = (select auth.uid()) or public.is_organizer() );
create policy registrations_delete on public.registrations
  for delete to authenticated
  using  ( user_id = (select auth.uid()) or public.is_admin() );

-- attendance: chủ nhân hoặc BTC đọc; chỉ BTC ghi (điểm danh QR)
create policy attendance_select on public.attendance
  for select to authenticated using ( user_id = (select auth.uid()) or public.is_organizer() );
create policy attendance_insert on public.attendance
  for insert to authenticated with check ( public.is_organizer() );
create policy attendance_update on public.attendance
  for update to authenticated
  using  ( public.is_organizer() )
  with check ( public.is_organizer() );
create policy attendance_delete on public.attendance
  for delete to authenticated using ( public.is_organizer() );

-- notifications: sinh viên đọc; chỉ Admin/Cán bộ đào tạo ghi
create policy notifications_select on public.notifications
  for select to authenticated using (true);
create policy notifications_insert on public.notifications
  for insert to authenticated with check ( public.is_admin() );
create policy notifications_update on public.notifications
  for update to authenticated
  using  ( public.is_admin() )
  with check ( public.is_admin() );
create policy notifications_delete on public.notifications
  for delete to authenticated using ( public.is_admin() );

-- schedules: chỉ chủ nhân (theo user_id hoặc student_id) hoặc Admin đọc; Admin ghi
create policy schedules_select on public.schedules
  for select to authenticated using (
    user_id = (select auth.uid())
    or (student_id is not null and student_id = public.my_student_id())
    or public.is_admin()
  );
create policy schedules_insert on public.schedules
  for insert to authenticated with check ( public.is_admin() );
create policy schedules_update on public.schedules
  for update to authenticated
  using  ( public.is_admin() )
  with check ( public.is_admin() );
create policy schedules_delete on public.schedules
  for delete to authenticated using ( public.is_admin() );

-- grades: bảo mật tuyệt đối — chỉ chủ nhân hoặc Admin đọc; Admin ghi
create policy grades_select on public.grades
  for select to authenticated using (
    user_id = (select auth.uid())
    or (student_id is not null and student_id = public.my_student_id())
    or public.is_admin()
  );
create policy grades_insert on public.grades
  for insert to authenticated with check ( public.is_admin() );
create policy grades_update on public.grades
  for update to authenticated
  using  ( public.is_admin() )
  with check ( public.is_admin() );
create policy grades_delete on public.grades
  for delete to authenticated using ( public.is_admin() );

-- ---------------------------------------------------------------------------
-- 5. Trigger tự khởi tạo profile khi đăng ký tài khoản mới
--    SECURITY DEFINER (user mới chưa đủ quyền RLS để tự insert). Chỉ lấy
--    displayName/avatarUrl/studentId từ user_metadata để HIỂN THỊ — không dùng
--    cho phân quyền; `role` luôn mặc định 'student' (cấp quyền qua app_metadata
--    hoặc admin). Trigger function return type `trigger` nên không gọi được qua RPC.
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, email, display_name, avatar_url, student_id)
  values (
    new.id,
    new.email,
    coalesce(
      new.raw_user_meta_data->>'displayName',
      new.raw_user_meta_data->>'full_name',
      split_part(coalesce(new.email, ''), '@', 1)
    ),
    new.raw_user_meta_data->>'avatarUrl',
    new.raw_user_meta_data->>'studentId'
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- 6. Duy trì events.current_registered + chặn vượt sĩ số / quá hạn đăng ký
--    Các trigger chạy nội bộ nên đặt SECURITY DEFINER trong schema `app`
--    (không expose) và thu hồi EXECUTE — bản thân trigger không cần quyền EXECUTE.
-- ---------------------------------------------------------------------------
create or replace function app.enforce_registration_capacity()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_max  integer;
  v_cur  integer;
  v_st   public.event_status;
  v_dl   timestamptz;
begin
  select e.max_participants, e.current_registered, e.status, e.registration_deadline
    into v_max, v_cur, v_st, v_dl
  from public.events e
  where e.id = new.event_id;

  if not found then
    raise exception 'Không tìm thấy sự kiện %', new.event_id;
  end if;
  if v_st <> 'UPCOMING' then
    raise exception 'Sự kiện không mở đăng ký (%)', v_st;
  end if;
  if v_dl is not null and now() > v_dl then
    raise exception 'Đã hết hạn đăng ký';
  end if;
  if v_cur >= v_max then
    raise exception 'Sự kiện đã đạt sĩ số tối đa';
  end if;
  return new;
end;
$$;

create or replace function app.maintain_registration_count()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (tg_op = 'INSERT') then
    update public.events set current_registered = current_registered + 1 where id = new.event_id;
    return new;
  elsif (tg_op = 'DELETE') then
    update public.events set current_registered = greatest(current_registered - 1, 0) where id = old.event_id;
    return old;
  end if;
  return null;
end;
$$;

revoke execute on function app.enforce_registration_capacity(), app.maintain_registration_count()
  from public, anon, authenticated;

create trigger registrations_before_insert
  before insert on public.registrations
  for each row execute function app.enforce_registration_capacity();

create trigger registrations_count_insert
  after insert on public.registrations
  for each row execute function app.maintain_registration_count();

create trigger registrations_count_delete
  after delete on public.registrations
  for each row execute function app.maintain_registration_count();

-- ---------------------------------------------------------------------------
-- 7. Tự cập nhật updated_at
-- ---------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

create trigger events_set_updated_at
  before update on public.events
  for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- 8. Quyền truy cập Data API (GRANT tường minh, kèm RLS ở trên)
--    Supabase có thể không tự expose bảng qua REST tuỳ cấu hình Data API, nên
--    cấp đặc quyền usage/select-dml cho anon/authenticated một cách rõ ràng.
-- ---------------------------------------------------------------------------
grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
-- notifications/events đọc công khai nếu bật anon read policy (hiện để authenticated-only)

-- ============================================================================
-- Apply & verify
-- ----------------------------------------------------------------------------
-- Cách 1 (Supabase CLI):
--   supabase db push        # áp migration này lên project liên kết
-- Cách 2 (SQL Editor / MCP execute_sql sau khi kết nối OAuth):
--   paste toàn bộ file rồi Run.
-- Kiểm tra nhanh:
--   select relname, relrowsecurity from pg_class
--   where relnamespace = 'public'::regnamespace and relkind = 'r'
--   order by relname;                       -- mọi bảng: relrowsecurity = true
--   select count(*) from pg_policies where schemaname = 'public';  -- > 0
-- ============================================================================
