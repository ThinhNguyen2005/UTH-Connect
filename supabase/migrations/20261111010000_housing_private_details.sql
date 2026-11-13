-- ============================================================================
-- UTH Connect — Ghép trọ: tách tọa độ chính xác + liên hệ sang bảng private
-- Migration: 20261111010000_housing_private_details.sql
-- ----------------------------------------------------------------------------
-- housing_posts từng chứa latitude/longitude/contact_phone/contact_zalo_url
-- trong dòng mà policy OPEN cho mọi authenticated đọc → lộ tọa độ + SĐT qua
-- PostgREST dù UI có ẩn. Migration này:
--   * chuyển 4 cột đó + địa chỉ chính xác sang housing_private_details,
--     RLS SELECT chỉ chủ tin; người khác đọc qua RPC reveal khi ACCEPTED;
--   * public chỉ còn tọa độ xấp xỉ (làm tròn 2 chữ số ≈ lưới 1km) sinh ra
--     server-side trong RPC create/update, client không gửi giá trị xấp xỉ;
--   * create/update ghi cả hai bảng trong một transaction.
-- Idempotent: chạy lại được trên Supabase Preview branch; bước di chuyển dữ
-- liệu chỉ chạy khi cột cũ còn tồn tại.
-- ============================================================================

begin;

-- 1. Bảng private ------------------------------------------------------------
create table if not exists public.housing_private_details (
    listing_id uuid primary key references public.housing_posts(id) on delete cascade,
    latitude double precision not null default 0.0,
    longitude double precision not null default 0.0,
    exact_address text not null default '',
    contact_phone text not null,
    contact_zalo_url text not null default '',
    updated_at timestamptz not null default now()
);

alter table public.housing_private_details enable row level security;

-- Chỉ chủ tin đọc thẳng; requester ACCEPTED đi qua RPC reveal. Policy chéo
-- sang housing_posts, không self-table (né 42P17).
drop policy if exists housing_private_details_select on public.housing_private_details;
create policy housing_private_details_select on public.housing_private_details
    for select to authenticated
    using (
        exists (
            select 1 from public.housing_posts p
            where p.id = listing_id and p.author_id = (select auth.uid())
        )
    );

-- Không policy insert/update/delete: chỉ RPC create/update ghi bảng này.

-- 2. Cột xấp xỉ phía public ---------------------------------------------------
alter table public.housing_posts
    add column if not exists approx_latitude double precision not null default 0.0,
    add column if not exists approx_longitude double precision not null default 0.0;

-- 3. Di chuyển dữ liệu staging (guard: chỉ chạy khi cột cũ còn) --------------
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public'
          and table_name = 'housing_posts'
          and column_name = 'latitude'
    ) then
        insert into public.housing_private_details
            (listing_id, latitude, longitude, exact_address, contact_phone, contact_zalo_url)
        select id, latitude, longitude, address_label, contact_phone, contact_zalo_url
        from public.housing_posts
        on conflict (listing_id) do nothing;

        -- address_label cũ là địa chỉ đầy đủ → đã sang exact_address; public
        -- reset về rỗng, UI fallback nhãn khu vực theo cơ sở.
        update public.housing_posts p
        set approx_latitude = round(d.latitude::numeric, 2)::double precision,
            approx_longitude = round(d.longitude::numeric, 2)::double precision,
            address_label = ''
        from public.housing_private_details d
        where d.listing_id = p.id;

        alter table public.housing_posts
            drop column if exists latitude,
            drop column if exists longitude,
            drop column if exists contact_phone,
            drop column if exists contact_zalo_url;
    end if;
end $$;

-- 4. RPC create/update: một transaction, hai bảng ----------------------------
create or replace function public.create_housing_post(p_payload jsonb)
returns setof public.housing_posts
language plpgsql security definer set search_path = ''
as $$
declare
    v_id uuid;
    v_lat double precision;
    v_lng double precision;
begin
    if coalesce(btrim(p_payload->>'contact_phone'), '') = '' then
        raise exception 'HOUSING_CONTACT_PHONE_REQUIRED' using errcode = 'P0001';
    end if;
    v_lat := coalesce((p_payload->>'latitude')::double precision, 0.0);
    v_lng := coalesce((p_payload->>'longitude')::double precision, 0.0);

    -- Trigger housing_open_limit vẫn chạy trong RPC (definer không bỏ trigger)
    -- nên invariant 2 tin OPEN giữ nguyên kể cả khi insert đồng thời.
    insert into public.housing_posts (
        author_id, type, campus, title, description, host_name, host_student_id,
        images, monthly_rent_vnd, deposit_vnd, utilities_included, available_slots,
        gender_preference, schedule_note, amenities, address_label,
        approx_latitude, approx_longitude, area_m2
    ) values (
        (select auth.uid()),
        p_payload->>'type',
        p_payload->>'campus',
        p_payload->>'title',
        coalesce(p_payload->>'description', ''),
        p_payload->>'host_name',
        p_payload->>'host_student_id',
        coalesce(array(select jsonb_array_elements_text(coalesce(p_payload->'images', '[]'::jsonb))), '{}'),
        (p_payload->>'monthly_rent_vnd')::integer,
        coalesce((p_payload->>'deposit_vnd')::integer, 0),
        coalesce((p_payload->>'utilities_included')::boolean, false),
        coalesce((p_payload->>'available_slots')::integer, 1),
        coalesce(p_payload->>'gender_preference', 'ANY'),
        coalesce(p_payload->>'schedule_note', ''),
        coalesce(array(select jsonb_array_elements_text(coalesce(p_payload->'amenities', '[]'::jsonb))), '{}'),
        coalesce(p_payload->>'address_label', ''),
        round(v_lat::numeric, 2)::double precision,
        round(v_lng::numeric, 2)::double precision,
        (p_payload->>'area_m2')::integer
    )
    returning id into v_id;

    insert into public.housing_private_details
        (listing_id, latitude, longitude, exact_address, contact_phone, contact_zalo_url)
    values (
        v_id, v_lat, v_lng,
        coalesce(p_payload->>'exact_address', ''),
        p_payload->>'contact_phone',
        coalesce(p_payload->>'contact_zalo_url', '')
    );

    return query select * from public.housing_posts where id = v_id;
end;
$$;

create or replace function public.update_housing_post(p_listing_id uuid, p_payload jsonb)
returns setof public.housing_posts
language plpgsql security definer set search_path = ''
as $$
declare
    v_author uuid;
    v_lat double precision;
    v_lng double precision;
begin
    select author_id into v_author from public.housing_posts where id = p_listing_id;
    if not found or v_author is distinct from (select auth.uid()) then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
    if coalesce(btrim(p_payload->>'contact_phone'), '') = '' then
        raise exception 'HOUSING_CONTACT_PHONE_REQUIRED' using errcode = 'P0001';
    end if;
    v_lat := coalesce((p_payload->>'latitude')::double precision, 0.0);
    v_lng := coalesce((p_payload->>'longitude')::double precision, 0.0);

    -- Không đụng status/report_count nên guard trigger pass, không cần GUC
    -- housing.bypass_guard; updated_at do trigger tự đặt.
    update public.housing_posts set
        type = p_payload->>'type',
        campus = p_payload->>'campus',
        title = p_payload->>'title',
        description = coalesce(p_payload->>'description', ''),
        host_name = p_payload->>'host_name',
        host_student_id = p_payload->>'host_student_id',
        images = coalesce(array(select jsonb_array_elements_text(coalesce(p_payload->'images', '[]'::jsonb))), '{}'),
        monthly_rent_vnd = (p_payload->>'monthly_rent_vnd')::integer,
        deposit_vnd = coalesce((p_payload->>'deposit_vnd')::integer, 0),
        utilities_included = coalesce((p_payload->>'utilities_included')::boolean, false),
        available_slots = coalesce((p_payload->>'available_slots')::integer, 1),
        gender_preference = coalesce(p_payload->>'gender_preference', 'ANY'),
        schedule_note = coalesce(p_payload->>'schedule_note', ''),
        amenities = coalesce(array(select jsonb_array_elements_text(coalesce(p_payload->'amenities', '[]'::jsonb))), '{}'),
        address_label = coalesce(p_payload->>'address_label', ''),
        approx_latitude = round(v_lat::numeric, 2)::double precision,
        approx_longitude = round(v_lng::numeric, 2)::double precision,
        area_m2 = (p_payload->>'area_m2')::integer
    where id = p_listing_id;

    update public.housing_private_details set
        latitude = v_lat,
        longitude = v_lng,
        exact_address = coalesce(p_payload->>'exact_address', ''),
        contact_phone = p_payload->>'contact_phone',
        contact_zalo_url = coalesce(p_payload->>'contact_zalo_url', ''),
        updated_at = now()
    where listing_id = p_listing_id;

    return query select * from public.housing_posts where id = p_listing_id;
end;
$$;

-- 5. RPC reveal: cổng đọc duy nhất cho người không phải chủ tin --------------
create or replace function public.reveal_housing_private_details(p_listing_id uuid)
returns table (
    latitude double precision,
    longitude double precision,
    exact_address text,
    contact_phone text,
    contact_zalo_url text
)
language plpgsql security definer set search_path = ''
as $$
begin
    if not exists (select 1 from public.housing_posts where id = p_listing_id) then
        raise exception 'HOUSING_NOT_FOUND' using errcode = 'P0001';
    end if;
    if not exists (
        select 1 from public.housing_posts
        where id = p_listing_id and author_id = (select auth.uid())
    ) and not exists (
        select 1 from public.housing_contact_requests
        where listing_id = p_listing_id
          and requester_id = (select auth.uid())
          and status = 'ACCEPTED'
    ) then
        raise exception 'HOUSING_LOCATION_NOT_REVEALED' using errcode = 'P0001';
    end if;
    return query
    select d.latitude, d.longitude, d.exact_address, d.contact_phone, d.contact_zalo_url
    from public.housing_private_details d
    where d.listing_id = p_listing_id;
end;
$$;

revoke all on function public.create_housing_post(jsonb) from public, anon;
revoke all on function public.update_housing_post(uuid, jsonb) from public, anon;
revoke all on function public.reveal_housing_private_details(uuid) from public, anon;
grant execute on function public.create_housing_post(jsonb) to authenticated;
grant execute on function public.update_housing_post(uuid, jsonb) to authenticated;
grant execute on function public.reveal_housing_private_details(uuid) to authenticated;

commit;