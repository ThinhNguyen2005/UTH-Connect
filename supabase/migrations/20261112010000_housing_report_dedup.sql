-- ============================================================================
-- UTH Connect — Ghép trọ: mỗi người chỉ báo được một tin một lần
-- Migration: 20261112010000_housing_report_dedup.sql
-- ----------------------------------------------------------------------------
-- report_housing_listing cũ tăng report_count vô điều kiện: một người bấm gửi
-- lặp (hoặc gọi lại RPC) đẩy tin lên ngưỡng ẩn 3 báo cáo dù chỉ có 1 người báo.
-- Sửa: lưu vết báo cáo theo cặp (listing_id, reporter_id) lấy từ auth.uid();
-- chỉ tăng report_count khi insert được bản ghi mới. Bảng không mở policy nào
-- nên client không đọc/ghi trực tiếp được — chỉ RPC security definer chạm tới.
-- Idempotent: chạy lại nhiều lần không lỗi (Supabase Preview yêu cầu).
-- ============================================================================

begin;

create table if not exists public.housing_reports (
    id uuid primary key default gen_random_uuid(),
    listing_id uuid not null references public.housing_posts(id) on delete cascade,
    reporter_id uuid not null references public.profiles(id) on delete cascade,
    reason text not null check (length(btrim(reason)) > 0),
    created_at timestamptz not null default now(),
    unique (listing_id, reporter_id)
);

alter table public.housing_reports enable row level security;

create or replace function public.report_housing_listing(p_listing_id uuid, p_reason text)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_count integer;
    v_inserted integer;
begin
    if p_reason is null or length(btrim(p_reason)) = 0 then
        raise exception 'HOUSING_REPORT_REASON_REQUIRED' using errcode = 'P0001';
    end if;
    if not exists (
        select 1 from public.housing_posts where id = p_listing_id and status = 'OPEN'
    ) then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;

    insert into public.housing_reports (listing_id, reporter_id, reason)
    values (p_listing_id, (select auth.uid()), btrim(p_reason))
    on conflict (listing_id, reporter_id) do nothing;
    get diagnostics v_inserted = row_count;
    -- Đã báo rồi thì trả về thành công lặng lẽ, không tăng đếm lần nữa.
    if v_inserted = 0 then
        return;
    end if;

    perform set_config('housing.bypass_guard', 'on', true);
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

commit;