begin;

create or replace function public.get_housing_reports(p_listing_id uuid)
returns table (reason text, created_at timestamptz)
language plpgsql security definer set search_path = ''
as $$
begin
    if (select auth.uid()) is null or not public.is_admin() then
        raise exception 'HOUSING_ADMIN_ONLY' using errcode = 'P0001';
    end if;
    return query select r.reason, r.created_at
        from public.housing_reports r
        where r.listing_id = p_listing_id
        order by r.created_at desc, r.id;
end;
$$;

create or replace function public.hide_housing_listing(p_listing_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if (select auth.uid()) is null or not public.is_admin() then
        raise exception 'HOUSING_ADMIN_ONLY' using errcode = 'P0001';
    end if;
    perform 1 from public.housing_posts where id = p_listing_id for update;
    if not found then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
    perform set_config('housing.bypass_guard', 'on', true);
    update public.housing_posts set status = 'HIDDEN_BY_REPORTS', updated_at = now()
        where id = p_listing_id;
end;
$$;

create or replace function public.delete_housing_listing(p_listing_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if (select auth.uid()) is null or not public.is_admin() then
        raise exception 'HOUSING_ADMIN_ONLY' using errcode = 'P0001';
    end if;
    -- shortcut: Storage files are retained; add durable Storage API cleanup before promising image erasure.
    delete from public.housing_posts where id = p_listing_id;
    if not found then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
end;
$$;

revoke all on function public.get_housing_reports(uuid) from public, anon;
grant execute on function public.get_housing_reports(uuid) to authenticated;
revoke all on function public.hide_housing_listing(uuid) from public, anon;
grant execute on function public.hide_housing_listing(uuid) to authenticated;
revoke all on function public.delete_housing_listing(uuid) from public, anon;
grant execute on function public.delete_housing_listing(uuid) to authenticated;

commit;
