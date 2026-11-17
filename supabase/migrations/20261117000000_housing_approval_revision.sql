begin;

drop function public.approve_housing_listing(uuid);

create function public.approve_housing_listing(
    p_listing_id uuid,
    p_reviewed_updated_at timestamptz
)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_updated_at timestamptz;
begin
    if (select auth.uid()) is null or not public.is_admin() then
        raise exception 'HOUSING_ADMIN_ONLY' using errcode = 'P0001';
    end if;

    select updated_at into v_updated_at
        from public.housing_posts
        where id = p_listing_id and status = 'PENDING'
        for update;
    if not found then
        raise exception 'HOUSING_NOT_PENDING' using errcode = 'P0001';
    end if;
    if v_updated_at is distinct from p_reviewed_updated_at then
        raise exception 'HOUSING_LISTING_CHANGED' using errcode = 'P0001';
    end if;

    perform set_config('housing.bypass_guard', 'on', true);
    update public.housing_posts
        set status = 'OPEN', rejection_reason = null, updated_at = now()
        where id = p_listing_id;
end;
$$;

revoke all on function public.approve_housing_listing(uuid, timestamptz) from public, anon;
grant execute on function public.approve_housing_listing(uuid, timestamptz) to authenticated;

-- Dọn dẹp key ảnh cũ chứa %, .. hoặc không thuộc về author_id của tin trước khi bật guard mới
select set_config('housing.bypass_guard', 'on', true);

update public.housing_posts
set
    images = coalesce(
        (
            select array_agg(img order by ord)
            from unnest(images) with ordinality as t(img, ord)
            where img ~ '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/'
              and split_part(regexp_replace(img, '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/', ''), '?', 1) !~ '(%|\.\.)'
              and split_part(regexp_replace(img, '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/', ''), '?', 1) ~ ('^' || author_id::text || '/')
        ),
        '{}'
    )
where images is not null
  and images <> '{}'
  and exists (
      select 1
      from unnest(images) as img
      where img !~ '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/'
         or split_part(regexp_replace(img, '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/', ''), '?', 1) ~ '(%|\.\.)'
         or split_part(regexp_replace(img, '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/', ''), '?', 1) !~ ('^' || author_id::text || '/')
  );

-- Ràng buộc quyền sở hữu key và chống path traversal trong URL ảnh
create or replace function public.trg_housing_url_guards()
returns trigger
language plpgsql
as $$
declare
    v_image text;
    v_prefix constant text := '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/';
    v_path text;
begin
    foreach v_image in array coalesce(new.images, '{}') loop
        if v_image !~ v_prefix then
            raise exception 'HOUSING_IMAGE_BUCKET_INVALID' using errcode = 'P0001';
        end if;
        v_path := regexp_replace(v_image, v_prefix, '');
        v_path := split_part(v_path, '?', 1);
        if v_path ~ '(%|\.\.)' then
            raise exception 'HOUSING_IMAGE_PATH_INVALID' using errcode = 'P0001';
        end if;
        if new.author_id is null or v_path !~ ('^' || new.author_id::text || '/') then
            raise exception 'HOUSING_IMAGE_OWNERSHIP_INVALID' using errcode = 'P0001';
        end if;
    end loop;

    if coalesce(new.contact_zalo_url, '') <> ''
        and new.contact_zalo_url !~* '^https://zalo\.me(/|$)' then
        raise exception 'HOUSING_ZALO_URL_INVALID' using errcode = 'P0001';
    end if;
    return new;
end;
$$;

commit;
