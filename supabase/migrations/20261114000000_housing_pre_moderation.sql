-- ============================================================================
-- UTH Connect — Ghép trọ: kiểm duyệt trước khi đăng (pre-moderation)
-- Migration: 20261114000000_housing_pre_moderation.sql
-- ----------------------------------------------------------------------------
-- Quyết định sản phẩm 14/11: tin mới đăng = PENDING, chỉ admin duyệt mới thành
-- OPEN và lên feed; admin từ chối = REJECTED kèm rejection_reason. Chủ tin sửa
-- tin OPEN/REJECTED thì quay lại PENDING chờ duyệt lại.
--
-- Giữ nguyên cột images (không đổi tên image_urls) và trigger URL guards.
-- Giới hạn 2 tin tính cả OPEN + PENDING, chỉ enforce ở trigger (đã lock dòng
-- profiles chống race); policy insert bỏ count để lỗi giới hạn luôn là
-- HOUSING_OPEN_LIMIT thay vì 42501 tùy thành phần trạng thái.
--
-- Idempotent: create or replace + drop/create trigger/policy + if not exists,
-- chạy lại được trên Preview branch. Tin OPEN cũ giữ nguyên, không hồi tố.
-- ============================================================================

begin;

-- 1. Bộ trạng thái mới + cột lý do từ chối -----------------------------------
alter table public.housing_posts drop constraint if exists housing_posts_status_check;
alter table public.housing_posts
    add constraint housing_posts_status_check
    check (status in ('PENDING', 'OPEN', 'REJECTED', 'FULFILLED', 'HIDDEN_BY_REPORTS'));

alter table public.housing_posts alter column status set default 'PENDING';

alter table public.housing_posts add column if not exists rejection_reason text;

-- 2. INSERT luôn về PENDING — client không tự duyệt được tin ------------------
-- Tên trigger đứng trước housing_open_limit (alphabet) để trạng thái đã bị ép
-- về PENDING trước khi trigger giới hạn đếm.
create or replace function public.trg_housing_insert_pending()
returns trigger
language plpgsql
as $$
begin
    if coalesce(current_setting('housing.bypass_guard', true), '') <> 'on' then
        new.status := 'PENDING';
        new.rejection_reason := null;
    end if;
    return new;
end;
$$;

drop trigger if exists housing_insert_pending on public.housing_posts;
create trigger housing_insert_pending
    before insert on public.housing_posts
    for each row execute function public.trg_housing_insert_pending();

-- 3. Guard UPDATE: status/report_count chỉ đổi qua RPC; sửa tin = duyệt lại ---
create or replace function public.trg_housing_guard_status()
returns trigger
language plpgsql
as $$
begin
    if coalesce(current_setting('housing.bypass_guard', true), '') <> 'on' then
        if new.status is distinct from old.status or new.report_count is distinct from old.report_count then
            raise exception 'HOUSING_STATUS_RPC_ONLY' using errcode = 'P0001';
        end if;
        if old.status in ('OPEN', 'REJECTED') then
            new.status := 'PENDING';
            new.rejection_reason := null;
        end if;
    end if;
    new.updated_at := now();
    return new;
end;
$$;

-- 4. Giới hạn 2 tin chưa kết thúc (OPEN + PENDING) -----------------------------
-- Chặn khi bước VÀO tập active: INSERT, hoặc UPDATE từ trạng thái không active
-- (REJECTED/FULFILLED/HIDDEN) sang OPEN/PENDING — tức đường chủ tin sửa tin
-- REJECTED để duyệt lại. Approve (PENDING -> OPEN) và sửa OPEN -> PENDING giữ
-- nguyên tổng OPEN+PENDING nên KHÔNG check lại (check lúc duyệt sẽ đếm nhầm
-- chính tin đang duyệt). BEFORE UPDATE chạy sau guard_status (alphabet) nên
-- new.status đã bị ép về PENDING trước khi đếm; dòng đang sửa còn trạng thái cũ
-- trong bảng nên không tự đếm chính nó.
create or replace function public.trg_housing_open_limit()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if new.status in ('OPEN', 'PENDING')
        and (tg_op = 'INSERT' or old.status not in ('OPEN', 'PENDING')) then
        perform 1 from public.profiles where id = new.author_id for update;
        if (select count(*) from public.housing_posts
            where author_id = new.author_id and status in ('OPEN', 'PENDING')) >= 2 then
            raise exception 'HOUSING_OPEN_LIMIT' using errcode = 'P0001';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists housing_open_limit on public.housing_posts;
create trigger housing_open_limit
    before insert or update on public.housing_posts
    for each row execute function public.trg_housing_open_limit();

-- 5. RPC kiểm duyệt của admin -------------------------------------------------
create or replace function public.approve_housing_listing(p_listing_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if (select auth.uid()) is null or not public.is_admin() then
        raise exception 'HOUSING_ADMIN_ONLY' using errcode = 'P0001';
    end if;
    perform 1 from public.housing_posts
        where id = p_listing_id and status = 'PENDING' for update;
    if not found then
        raise exception 'HOUSING_NOT_PENDING' using errcode = 'P0001';
    end if;
    perform set_config('housing.bypass_guard', 'on', true);
    update public.housing_posts
        set status = 'OPEN', rejection_reason = null, updated_at = now()
        where id = p_listing_id;
end;
$$;

create or replace function public.reject_housing_listing(p_listing_id uuid, p_reason text)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if (select auth.uid()) is null or not public.is_admin() then
        raise exception 'HOUSING_ADMIN_ONLY' using errcode = 'P0001';
    end if;
    if coalesce(trim(p_reason), '') = '' then
        raise exception 'HOUSING_REASON_REQUIRED' using errcode = 'P0001';
    end if;
    perform 1 from public.housing_posts
        where id = p_listing_id and status = 'PENDING' for update;
    if not found then
        raise exception 'HOUSING_NOT_PENDING' using errcode = 'P0001';
    end if;
    perform set_config('housing.bypass_guard', 'on', true);
    update public.housing_posts
        set status = 'REJECTED', rejection_reason = trim(p_reason), updated_at = now()
        where id = p_listing_id;
end;
$$;

revoke all on function public.approve_housing_listing(uuid) from public, anon;
grant execute on function public.approve_housing_listing(uuid) to authenticated;
revoke all on function public.reject_housing_listing(uuid, text) from public, anon;
grant execute on function public.reject_housing_listing(uuid, text) to authenticated;

-- 6. RLS: admin đọc mọi tin; insert chỉ kiểm tra chủ tin ----------------------
drop policy if exists housing_posts_select on public.housing_posts;
create policy housing_posts_select on public.housing_posts
    for select to authenticated
    using (
        author_id = (select auth.uid())
        or (select public.is_admin())
        or (
            status = 'OPEN'
            and exists (
                select 1 from public.profiles p
                where p.id = (select auth.uid()) and p.student_id_verified = true
            )
        )
    );

drop policy if exists housing_posts_insert on public.housing_posts;
create policy housing_posts_insert on public.housing_posts
    for insert to authenticated
    with check (author_id = (select auth.uid()));

commit;
