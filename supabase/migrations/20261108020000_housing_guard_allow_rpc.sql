-- ============================================================================
-- UTH Connect — Ghép trọ: cho RPC đổi status qua guard trigger
-- Migration: 20261108020000_housing_guard_allow_rpc.sql
-- ----------------------------------------------------------------------------
-- Guard trigger 20261108000000 chặn MỌI update đổi status/report_count kể cả
-- update bên trong RPC security definer (trigger không phân biệt ngữ cảnh).
-- Hậu quả: mark_housing_listing_fulfilled bị HOUSING_STATUS_RPC_ONLY.
-- Sửa: hai RPC đặt cờ GUC transaction-local `housing.bypass_guard`; guard chỉ
-- chặn khi cờ vắng mặt. Client không có đường đặt GUC qua PostgREST, chỉ hai
-- RPC security definer này đặt cờ nên không bị lạm dụng.
-- ============================================================================

begin;

create or replace function public.trg_housing_guard_status()
returns trigger
language plpgsql
as $$
begin
    if coalesce(current_setting('housing.bypass_guard', true), '') <> 'on'
        and (new.status is distinct from old.status or new.report_count is distinct from old.report_count) then
        raise exception 'HOUSING_STATUS_RPC_ONLY' using errcode = 'P0001';
    end if;
    new.updated_at := now();
    return new;
end;
$$;

create or replace function public.mark_housing_listing_fulfilled(p_listing_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    perform set_config('housing.bypass_guard', 'on', true);
    update public.housing_posts
    set status = 'FULFILLED', available_slots = 0, updated_at = now()
    where id = p_listing_id and author_id = (select auth.uid());
    if not found then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
end;
$$;

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