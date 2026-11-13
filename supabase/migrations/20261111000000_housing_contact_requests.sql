-- ============================================================================
-- UTH Connect — Ghép trọ: contact request làm cổng mở khóa liên hệ
-- Migration: 20261111000000_housing_contact_requests.sql
-- ----------------------------------------------------------------------------
-- SĐT/Zalo và tọa độ chính xác chuyển sang bảng private ở migration kế tiếp;
-- chỉ chủ tin và requester có request ACCEPTED mới đọc được qua RPC reveal.
-- Bảng này là gate đó: mọi thay đổi đi qua RPC security definer, RLS chỉ mở
-- đường đọc cho hai phía liên quan (requester và chủ tin).
-- Idempotent: chạy lại được trên Supabase Preview branch.
-- ============================================================================

begin;

-- 1. Enum trạng thái ---------------------------------------------------------
do $$
begin
    if not exists (select 1 from pg_type where typname = 'housing_contact_status') then
        create type public.housing_contact_status as enum ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED');
    end if;
end $$;

-- 2. Bảng --------------------------------------------------------------------
create table if not exists public.housing_contact_requests (
    id uuid primary key default gen_random_uuid(),
    listing_id uuid not null references public.housing_posts(id) on delete cascade,
    requester_id uuid not null references public.profiles(id) on delete cascade,
    status public.housing_contact_status not null default 'PENDING',
    message text not null default '' check (length(message) <= 500),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists housing_contact_requests_listing_idx
    on public.housing_contact_requests (listing_id, status);
create index if not exists housing_contact_requests_requester_idx
    on public.housing_contact_requests (requester_id, created_at desc);

-- Mỗi cặp (tin, người hỏi) chỉ một request đang hiệu lực; REJECTED/CANCELLED
-- vẫn cho phép hỏi lại nên không nằm trong index unique.
create unique index if not exists housing_contact_requests_active_uniq
    on public.housing_contact_requests (listing_id, requester_id)
    where status in ('PENDING', 'ACCEPTED');

-- 3. RLS ---------------------------------------------------------------------
alter table public.housing_contact_requests enable row level security;

-- Policy chéo sang housing_posts, không self-table: tránh recursion 42P17
-- như bài học housing_posts_insert (20261108010000).
drop policy if exists housing_contact_requests_select on public.housing_contact_requests;
create policy housing_contact_requests_select on public.housing_contact_requests
    for select to authenticated
    using (
        requester_id = (select auth.uid())
        or exists (
            select 1 from public.housing_posts p
            where p.id = listing_id and p.author_id = (select auth.uid())
        )
    );

-- Không policy insert/update/delete: client không ghi thẳng, chỉ qua RPC.

-- 4. RPC ---------------------------------------------------------------------
create or replace function public.request_housing_contact(p_listing_id uuid, p_message text default '')
returns uuid
language plpgsql security definer set search_path = ''
as $$
declare
    v_author uuid;
    v_listing_status text;
    v_id uuid;
    v_status public.housing_contact_status;
begin
    select author_id, status into v_author, v_listing_status
    from public.housing_posts where id = p_listing_id;
    if not found then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
    if v_listing_status <> 'OPEN' then
        raise exception 'HOUSING_CONTACT_LISTING_CLOSED' using errcode = 'P0001';
    end if;
    if v_author = (select auth.uid()) then
        raise exception 'HOUSING_CONTACT_SELF_REQUEST' using errcode = 'P0001';
    end if;
    select id, status into v_id, v_status
    from public.housing_contact_requests
    where listing_id = p_listing_id
      and requester_id = (select auth.uid())
      and status in ('PENDING', 'ACCEPTED');
    if found then
        if v_status = 'ACCEPTED' then
            raise exception 'HOUSING_CONTACT_ALREADY_ACCEPTED' using errcode = 'P0001';
        end if;
        return v_id;
    end if;
    insert into public.housing_contact_requests (listing_id, requester_id, message)
    values (p_listing_id, (select auth.uid()), coalesce(btrim(p_message), ''))
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.respond_housing_contact(p_request_id uuid, p_accept boolean)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_listing uuid;
    v_author uuid;
    v_status public.housing_contact_status;
begin
    select listing_id, status into v_listing, v_status
    from public.housing_contact_requests where id = p_request_id;
    if not found then
        raise exception 'HOUSING_CONTACT_NOT_FOUND' using errcode = 'P0001';
    end if;
    select author_id into v_author from public.housing_posts where id = v_listing;
    if v_author is distinct from (select auth.uid()) then
        raise exception 'HOUSING_CONTACT_FORBIDDEN' using errcode = 'P0001';
    end if;
    if v_status <> 'PENDING' then
        raise exception 'HOUSING_CONTACT_NOT_PENDING' using errcode = 'P0001';
    end if;
    if p_accept then
        update public.housing_contact_requests
        set status = 'ACCEPTED', updated_at = now()
        where id = p_request_id;
    else
        update public.housing_contact_requests
        set status = 'REJECTED', updated_at = now()
        where id = p_request_id;
    end if;
end;
$$;

create or replace function public.cancel_housing_contact(p_request_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_requester uuid;
    v_status public.housing_contact_status;
begin
    select requester_id, status into v_requester, v_status
    from public.housing_contact_requests where id = p_request_id;
    if not found then
        raise exception 'HOUSING_CONTACT_NOT_FOUND' using errcode = 'P0001';
    end if;
    if v_requester is distinct from (select auth.uid()) then
        raise exception 'HOUSING_CONTACT_FORBIDDEN' using errcode = 'P0001';
    end if;
    if v_status <> 'PENDING' then
        raise exception 'HOUSING_CONTACT_NOT_PENDING' using errcode = 'P0001';
    end if;
    update public.housing_contact_requests
    set status = 'CANCELLED', updated_at = now()
    where id = p_request_id;
end;
$$;

revoke all on function public.request_housing_contact(uuid, text) from public, anon;
revoke all on function public.respond_housing_contact(uuid, boolean) from public, anon;
revoke all on function public.cancel_housing_contact(uuid) from public, anon;
grant execute on function public.request_housing_contact(uuid, text) to authenticated;
grant execute on function public.respond_housing_contact(uuid, boolean) to authenticated;
grant execute on function public.cancel_housing_contact(uuid) to authenticated;

commit;