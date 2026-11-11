-- ============================================================================
-- UTH Connect — Ghép trọ: dựng lại housing_posts theo schema đầy đủ
-- Migration: 20261108000000_housing_schema_rebuild.sql
-- ----------------------------------------------------------------------------
-- Bối cảnh: hai migration 20261107000000 cùng tạo housing_posts với schema lệch
-- nhau; trên staging bản RBAC (price/location/room_type/ACTIVE) chạy trước nên
-- bản create_housing_posts bị `create table if not exists` bỏ qua âm thầm.
-- Migration này dựng lại bảng theo đúng domain model Android (HousingListing).
--
-- CHỈ chạy trên staging chưa có dữ liệu thật: bảng cũ bị DROP. Kiểm tra
-- `select count(*) from public.housing_posts;` trước khi chạy; nếu có dữ liệu
-- phải viết bước chuyển đổi riêng thay vì DROP.
-- ============================================================================

begin;

-- 1. Dựng lại bảng -----------------------------------------------------------
drop table if exists public.housing_posts;

create table public.housing_posts (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references public.profiles(id) on delete cascade,
    type text not null check (type in ('ROOM_FOR_RENT', 'LOOKING_FOR_ROOMMATE', 'LOOKING_FOR_ROOM')),
    campus text not null check (campus in ('CS1_QUANG_TRUNG', 'CS2_THU_DUC', 'CS3_TO_KY')),
    title text not null check (length(btrim(title)) between 5 and 150),
    description text not null default '' check (length(description) <= 2000),
    host_name text not null,
    host_student_id text not null,
    contact_phone text not null,
    contact_zalo_url text not null default '',
    images text[] not null default '{}',
    monthly_rent_vnd integer not null check (monthly_rent_vnd > 0),
    deposit_vnd integer not null default 0 check (deposit_vnd >= 0),
    utilities_included boolean not null default false,
    available_slots integer not null default 1 check (available_slots >= 0),
    gender_preference text not null default 'ANY' check (gender_preference in ('ANY', 'MALE', 'FEMALE')),
    schedule_note text not null default '',
    amenities text[] not null default '{}',
    address_label text not null,
    latitude double precision not null default 0.0,
    longitude double precision not null default 0.0,
    area_m2 integer check (area_m2 is null or area_m2 > 0),
    verified boolean not null default false,
    verification_method text check (verification_method is null or verification_method in ('EMAIL_UTH', 'MSSV')),
    status text not null default 'OPEN' check (status in ('OPEN', 'FULFILLED', 'HIDDEN_BY_REPORTS')),
    report_count integer not null default 0 check (report_count >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index housing_posts_author_id_idx on public.housing_posts (author_id);
create index housing_posts_open_idx on public.housing_posts (created_at desc) where status = 'OPEN';

-- 2. RLS ---------------------------------------------------------------------
alter table public.housing_posts enable row level security;

-- Tin OPEN ai cũng đọc được; chủ tin đọc được tin của mình ở mọi trạng thái.
create policy housing_posts_select on public.housing_posts
    for select to authenticated
    using (status = 'OPEN' or author_id = (select auth.uid()));

-- Tối đa 2 tin OPEN mỗi người (README 3.2). Count ở đây chặn đường vòng;
-- trigger trg_housing_open_limit ниже giữ invariant khi insert đồng thời.
create policy housing_posts_insert on public.housing_posts
    for insert to authenticated
    with check (
        author_id = (select auth.uid())
        and (select count(*) from public.housing_posts
             where author_id = (select auth.uid()) and status = 'OPEN') < 2
    );

create policy housing_posts_update on public.housing_posts
    for update to authenticated
    using (author_id = (select auth.uid()))
    with check (author_id = (select auth.uid()));

create policy housing_posts_delete on public.housing_posts
    for delete to authenticated
    using (author_id = (select auth.uid()));

-- 3. Giới hạn 2 tin mở chịu được race ---------------------------------------
-- Khóa dòng profiles của chính người đăng để hai insert đồng thời phải xếp
-- hàng; RLS count một mình không chống được race vì cả hai cùng đọc count cũ.
create or replace function public.trg_housing_open_limit()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if new.status = 'OPEN' then
        perform 1 from public.profiles where id = new.author_id for update;
        if (select count(*) from public.housing_posts
            where author_id = new.author_id and status = 'OPEN') >= 2 then
            raise exception 'HOUSING_OPEN_LIMIT' using errcode = 'P0001';
        end if;
    end if;
    return new;
end;
$$;

create trigger housing_open_limit
    before insert on public.housing_posts
    for each row execute function public.trg_housing_open_limit();

-- 4. status/report_count chỉ đổi qua RPC ------------------------------------
-- Chủ tin không được tự đổi trạng thái kiểm duyệt hay xóa số đếm báo cáo;
-- markFulfilled đi qua RPC để hợp lệ. RPC security definer bỏ qua RLS nên
-- cũng không bị trigger này chặn.
create or replace function public.trg_housing_guard_status()
returns trigger
language plpgsql
as $$
begin
    if new.status is distinct from old.status or new.report_count is distinct from old.report_count then
        raise exception 'HOUSING_STATUS_RPC_ONLY' using errcode = 'P0001';
    end if;
    new.updated_at := now();
    return new;
end;
$$;

create trigger housing_guard_status
    before update on public.housing_posts
    for each row execute function public.trg_housing_guard_status();

-- Chủ tin đánh dấu hết phòng (README 3.2): gỡ khỏi feed, giữ bản ghi.
create or replace function public.mark_housing_listing_fulfilled(p_listing_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    update public.housing_posts
    set status = 'FULFILLED', available_slots = 0, updated_at = now()
    where id = p_listing_id and author_id = (select auth.uid());
    if not found then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
end;
$$;

-- Báo tin ảo: backend giữ report_count, đủ 3 báo cáo thì ẩn (README 3.2).
-- Client không được cộng số hay tự đổi trạng thái.
create or replace function public.report_housing_listing(p_listing_id uuid, p_reason text)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_count integer;
begin
    if p_reason is null or length(btrim(p_reason)) = 0 then
        raise exception 'HOUSING_REPORT_REASON_REQUIRED' using errcode = 'P0001';
    end if;
    update public.housing_posts
    set report_count = report_count + 1
    where id = p_listing_id and status = 'OPEN'
    returning report_count into v_count;
    if not found then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
    if v_count >= 3 then
        update public.housing_posts
        set status = 'HIDDEN_BY_REPORTS'
        where id = p_listing_id;
    end if;
end;
$$;

revoke all on function public.mark_housing_listing_fulfilled(uuid) from public, anon;
revoke all on function public.report_housing_listing(uuid, text) from public, anon;
grant execute on function public.mark_housing_listing_fulfilled(uuid) to authenticated;
grant execute on function public.report_housing_listing(uuid, text) to authenticated;

-- 5. Storage ảnh tin ----------------------------------------------------------
-- Bucket public-read; mỗi người chỉ ghi/xóa trong thư mục trùng auth.uid().
insert into storage.buckets (id, name, public)
values ('housing-images', 'housing-images', true)
on conflict (id) do nothing;

do $$
begin
    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'housing_images_public_read') then
        create policy housing_images_public_read on storage.objects
            for select to public
            using (bucket_id = 'housing-images');
    end if;
    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'housing_images_owner_insert') then
        create policy housing_images_owner_insert on storage.objects
            for insert to authenticated
            with check (bucket_id = 'housing-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
    end if;
    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'housing_images_owner_update') then
        create policy housing_images_owner_update on storage.objects
            for update to authenticated
            using (bucket_id = 'housing-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
    end if;
    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'housing_images_owner_delete') then
        create policy housing_images_owner_delete on storage.objects
            for delete to authenticated
            using (bucket_id = 'housing-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
    end if;
end $$;

commit;