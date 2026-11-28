-- KAN-182: client writes go through checked RPCs, never directly to tables.
create table public.incident_reports (
    id uuid primary key default gen_random_uuid(),
    code text not null unique,
    reporter_id uuid not null references auth.users(id),
    client_request_id uuid not null,
    is_anonymous_public boolean not null default false,
    campus text not null check (char_length(btrim(campus)) > 0),
    building text,
    room_code text not null check (char_length(btrim(room_code)) > 0 and room_code = upper(btrim(room_code))),
    location_detail text,
    category text not null check (category in (
        'DOOR_LOCK', 'ELECTRICAL', 'WATER_PLUMBING', 'AC_FAN', 'PROJECTOR_IT',
        'FURNITURE', 'HYGIENE', 'SAFETY', 'OTHER'
    )),
    severity text not null check (severity in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title text not null check (char_length(btrim(title)) between 5 and 120),
    description text not null check (char_length(btrim(description)) between 10 and 1000),
    image_paths text[] not null default '{}',
    resolution_image_paths text[] not null default '{}',
    status text not null default 'SUBMITTED' check (status in (
        'SUBMITTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'REOPENED',
        'CLOSED', 'REJECTED', 'DUPLICATE', 'CANCELLED'
    )),
    assigned_to uuid references public.profiles(id),
    assigned_unit text,
    resolution_note text,
    rejection_reason text,
    duplicate_of_id uuid references public.incident_reports(id),
    row_version integer not null default 1 check (row_version > 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    resolved_at timestamptz,
    closed_at timestamptz,
    unique (reporter_id, client_request_id),
    check (cardinality(image_paths) <= 4),
    check (cardinality(resolution_image_paths) <= 3),
    check (duplicate_of_id is null or duplicate_of_id <> id)
);

create index incident_reports_location_status_idx on public.incident_reports (campus, room_code, status);
create index incident_reports_reporter_created_idx on public.incident_reports (reporter_id, created_at desc);

create table public.incident_status_logs (
    id uuid primary key default gen_random_uuid(),
    incident_id uuid not null references public.incident_reports(id),
    from_status text,
    to_status text not null,
    actor_id uuid not null references auth.users(id),
    note text,
    created_at timestamptz not null default now()
);
create index incident_status_logs_incident_created_idx on public.incident_status_logs (incident_id, created_at);

create table public.incident_confirmations (
    incident_id uuid not null references public.incident_reports(id),
    user_id uuid not null references auth.users(id),
    created_at timestamptz not null default now(),
    primary key (incident_id, user_id)
);

alter table public.incident_reports enable row level security;
alter table public.incident_status_logs enable row level security;
alter table public.incident_confirmations enable row level security;

revoke all on public.incident_reports, public.incident_status_logs, public.incident_confirmations from public, anon, authenticated;
grant select on public.incident_reports, public.incident_status_logs, public.incident_confirmations to authenticated;

create policy incident_reports_owner_or_admin_select on public.incident_reports
    for select to authenticated
    using (reporter_id = (select auth.uid()) or (select public.is_admin()));

create policy incident_status_logs_owner_or_admin_select on public.incident_status_logs
    for select to authenticated
    using (exists (
        select 1 from public.incident_reports r
        where r.id = incident_id and (r.reporter_id = (select auth.uid()) or (select public.is_admin()))
    ));

create policy incident_confirmations_owner_or_admin_select on public.incident_confirmations
    for select to authenticated
    using (user_id = (select auth.uid()) or (select public.is_admin()));

-- Store object keys, never a client-supplied hostname. Clients resolve these
-- keys against their configured Supabase project when displaying images.
create sequence public.incident_report_code_seq;
revoke all on sequence public.incident_report_code_seq from public, anon, authenticated;

create function public.create_incident_report(
    p_client_request_id uuid,
    p_campus text,
    p_room_code text,
    p_location_detail text,
    p_category text,
    p_severity text,
    p_title text,
    p_description text,
    p_image_paths text[],
    p_is_anonymous_public boolean
) returns uuid
language plpgsql security definer set search_path = '' as $$
declare
    v_user_id uuid := auth.uid();
    v_existing_id uuid;
    v_id uuid;
    v_paths text[] := coalesce(p_image_paths, '{}');
begin
    if v_user_id is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
        raise exception 'INCIDENT_UNAUTHENTICATED' using errcode = '42501';
    end if;
    if p_client_request_id is null then
        raise exception 'INCIDENT_INVALID_REQUEST_ID' using errcode = '22023';
    end if;

    -- Serialize retries and rate-limit checks for a single reporter.
    perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(v_user_id::text, 0));
    select r.id into v_existing_id from public.incident_reports r
      where r.reporter_id = v_user_id and r.client_request_id = p_client_request_id;
    if v_existing_id is not null then
        return v_existing_id;
    end if;

    if nullif(btrim(p_campus), '') is null or nullif(btrim(p_room_code), '') is null
      or p_category is null or p_category not in (
        'DOOR_LOCK', 'ELECTRICAL', 'WATER_PLUMBING', 'AC_FAN', 'PROJECTOR_IT',
        'FURNITURE', 'HYGIENE', 'SAFETY', 'OTHER'
      ) or p_severity is null or p_severity not in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
      or p_title is null or char_length(btrim(p_title)) not between 5 and 120
      or p_description is null or char_length(btrim(p_description)) not between 10 and 1000
      or cardinality(v_paths) > 4 then
        raise exception 'INCIDENT_INVALID_INPUT' using errcode = '22023';
    end if;

    if exists (
        select 1 from unnest(v_paths) as path(value)
        where path.value is null
          or path.value !~ ('^' || v_user_id::text || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.webp$')
          or not exists (
              select 1 from storage.objects o
              where o.bucket_id = 'media_uploads' and o.name = path.value
          )
    ) then
        raise exception 'INCIDENT_INVALID_IMAGE' using errcode = '22023';
    end if;

    if (select count(*) from public.incident_reports r where r.reporter_id = v_user_id and r.status = 'SUBMITTED') >= 5
      or (select count(*) from public.incident_reports r where r.reporter_id = v_user_id and r.created_at >= now() - interval '24 hours') >= 10 then
        raise exception 'INCIDENT_RATE_LIMIT' using errcode = 'P0001';
    end if;

    insert into public.incident_reports (
        code, reporter_id, client_request_id, campus, room_code, location_detail,
        category, severity, title, description, image_paths, is_anonymous_public
    ) values (
        'INC-' || to_char(now(), 'YYYY') || '-' || lpad(nextval('public.incident_report_code_seq')::text, 6, '0'),
        v_user_id, p_client_request_id, btrim(p_campus), upper(btrim(p_room_code)),
        nullif(btrim(p_location_detail), ''), p_category, p_severity,
        btrim(p_title), btrim(p_description), v_paths, coalesce(p_is_anonymous_public, false)
    ) returning id into v_id;

    insert into public.incident_status_logs (incident_id, from_status, to_status, actor_id)
      values (v_id, null, 'SUBMITTED', v_user_id);
    return v_id;
end;
$$;

revoke all on function public.create_incident_report(uuid, text, text, text, text, text, text, text, text[], boolean)
  from public, anon, authenticated;
grant execute on function public.create_incident_report(uuid, text, text, text, text, text, text, text, text[], boolean)
  to authenticated;
