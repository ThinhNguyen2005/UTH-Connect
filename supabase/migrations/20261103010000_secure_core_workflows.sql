-- Apply AFTER 20261103000000_init_uth_connect.sql.
-- Safe for an already-applied initial migration: preserves rows and UUIDs.
-- All student mutations of registrations/attendance go through checked RPCs.

-- Normalize the enum without replacing existing values or records.
alter type public.event_status rename value 'UPCOMING' to 'PUBLISHED';
alter type public.event_status rename value 'ONGOING' to 'HAPPENING';
alter table public.events alter column status set default 'PUBLISHED';
drop index if exists public.events_upcoming_start_idx;
create index events_published_start_idx on public.events (start_at) where status = 'PUBLISHED';

-- Legacy student_id came from user_metadata: it is unverified until an admin
-- explicitly links it to the user's verified university identity.
alter table public.profiles add column if not exists student_id_verified boolean not null default false;
alter table public.notifications add column if not exists recipient_id uuid references public.profiles(id) on delete cascade;
create index if not exists notifications_recipient_id_idx on public.notifications(recipient_id);

-- Zero capacity means unlimited, matching the existing Firestore repository.
do $$
declare v_name name;
begin
  for v_name in
    select c.conname from pg_constraint c
    where c.conrelid = 'public.events'::regclass and c.contype = 'c'
      and pg_get_constraintdef(c.oid) like '%current_registered <= max_participants%'
  loop
    execute format('alter table public.events drop constraint %I', v_name);
  end loop;
end;
$$;

-- The original migration maintained counts only on INSERT/DELETE, not status
-- transitions. Replace those triggers with locked, controlled RPC workflows.
drop trigger if exists registrations_before_insert on public.registrations;
drop trigger if exists registrations_count_insert on public.registrations;
drop trigger if exists registrations_count_delete on public.registrations;
drop function if exists app.enforce_registration_capacity();
drop function if exists app.maintain_registration_count();
update public.events e set current_registered = (
  select count(*) from public.registrations r
  where r.event_id = e.id and r.status = 'REGISTERED'
);
alter table public.events add constraint events_capacity_consistent
  check (max_participants = 0 or current_registered <= max_participants) not valid;
-- Existing overbooked events remain visible; new changes must respect the bound.

-- Helper functions read roles with definer rights to avoid recursive profile RLS.
create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = ''
as $$ select exists (
  select 1 from public.profiles p
  where p.id = (select auth.uid()) and p.role = 'admin'
); $$;

create or replace function public.is_organizer() returns boolean
language sql stable security definer set search_path = ''
as $$ select exists (
  select 1 from public.profiles p
  where p.id = (select auth.uid()) and p.role in ('admin', 'organizer')
); $$;

-- Never copy student_id from client-editable auth metadata.
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles(id, email, display_name, avatar_url)
  values (new.id, coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data->>'displayName',
             new.raw_user_meta_data->>'full_name',
             split_part(coalesce(new.email, ''), '@', 1)),
    new.raw_user_meta_data->>'avatarUrl')
  on conflict (id) do nothing;
  return new;
end;
$$;

-- Hard permissions: RLS alone cannot prevent owner from changing role, student
-- ID, count, registration status or check-in columns on their own rows.
revoke insert, update, delete on public.profiles from authenticated;
grant update (display_name, avatar_url, phone_number) on public.profiles to authenticated;
revoke update on public.events from authenticated;
grant update (title, description, banner_url, location, campus, start_at,
              end_at, registration_deadline, max_participants, points, status,
              organizer_name) on public.events to authenticated;
revoke insert, update, delete on public.registrations from authenticated;
revoke insert, update, delete on public.attendance from authenticated;
-- Events are cancelled using their status. Hard deletion would cascade to
-- registrations and attendance and erase the audit/history of participation.
revoke delete on public.events from authenticated;
drop policy if exists events_delete on public.events;

drop policy if exists profiles_insert on public.profiles;
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to authenticated
  using (id = (select auth.uid()) or (select public.is_admin()));
drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));

drop policy if exists events_select on public.events;
create policy events_select on public.events for select to authenticated
  using (status <> 'DRAFT' or (select public.is_admin())
    or (organizer_id = (select auth.uid()) and (select public.is_organizer())));

drop policy if exists registrations_select on public.registrations;
create policy registrations_select on public.registrations for select to authenticated
  using (user_id = (select auth.uid()) or (select public.is_admin())
    or exists (select 1 from public.events e where e.id = event_id
      and e.organizer_id = (select auth.uid()) and (select public.is_organizer())));
drop policy if exists registrations_insert on public.registrations;
drop policy if exists registrations_update on public.registrations;
drop policy if exists registrations_delete on public.registrations;

drop policy if exists attendance_select on public.attendance;
create policy attendance_select on public.attendance for select to authenticated
  using (user_id = (select auth.uid()) or (select public.is_admin())
    or exists (select 1 from public.events e where e.id = event_id
      and e.organizer_id = (select auth.uid()) and (select public.is_organizer())));
drop policy if exists attendance_insert on public.attendance;
drop policy if exists attendance_update on public.attendance;
drop policy if exists attendance_delete on public.attendance;

-- A null recipient is a campus-wide announcement. A targeted announcement is
-- visible only to that user or an admin.
drop policy if exists notifications_select on public.notifications;
create policy notifications_select on public.notifications for select to authenticated
  using (recipient_id is null or recipient_id = (select auth.uid())
    or (select public.is_admin()));

-- Disallow access through an unverified or user-submitted student number.
drop policy if exists schedules_select on public.schedules;
create policy schedules_select on public.schedules for select to authenticated
  using (user_id = (select auth.uid()) or (select public.is_admin()));
drop policy if exists grades_select on public.grades;
create policy grades_select on public.grades for select to authenticated
  using (user_id = (select auth.uid()) or (select public.is_admin()));

-- Admin actions are audited through the database connection; these RPCs
-- must never be exposed to anonymous users.
create or replace function public.admin_verify_student(p_user_id uuid, p_student_id text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if (select auth.uid()) is null or not public.is_admin() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if length(btrim(p_student_id)) < 5 then
    raise exception 'INVALID_STUDENT_ID' using errcode = '22023';
  end if;
  if exists (select 1 from public.profiles where id = p_user_id
    and student_id_verified and student_id is distinct from btrim(p_student_id)) then
    raise exception 'STUDENT_ID_ALREADY_VERIFIED' using errcode = '42501';
  end if;
  update public.profiles set student_id = btrim(p_student_id), student_id_verified = true
  where id = p_user_id;
  if not found then raise exception 'USER_NOT_FOUND'; end if;
end;
$$;

create or replace function public.admin_set_user_role(p_user_id uuid, p_role public.user_role)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if (select auth.uid()) is null or not public.is_admin() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if p_user_id = (select auth.uid()) and p_role <> 'admin' then
    raise exception 'ADMIN_CANNOT_DEMOTE_SELF';
  end if;
  update public.profiles set role = p_role where id = p_user_id;
  if not found then raise exception 'USER_NOT_FOUND'; end if;
end;
$$;

-- Admin / organizer create events through normal events INSERT RLS.
-- Current counts are server-owned, never user-supplied.
revoke insert on public.events from authenticated;
grant insert (title, description, banner_url, location, campus, start_at,
              end_at, registration_deadline, max_participants, points,
              status, organizer_id, organizer_name) on public.events to authenticated;

-- Registration: serialise all requests per event with FOR UPDATE, and make
-- cancellation/re-registration idempotent. Client-supplied user IDs, names,
-- statuses and counts are ignored.
create or replace function public.register_for_event(p_event_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := (select auth.uid());
  v_event public.events%rowtype;
  v_profile public.profiles%rowtype;
  v_status public.registration_status;
begin
  if v_uid is null then raise exception 'AUTH_REQUIRED' using errcode='42501'; end if;
  select * into v_profile from public.profiles where id = v_uid;
  if not found or not v_profile.student_id_verified then
    raise exception 'STUDENT_NOT_VERIFIED' using errcode='42501';
  end if;
  select * into v_event from public.events where id = p_event_id for update;
  if not found then raise exception 'EVENT_NOT_FOUND'; end if;
  select status into v_status from public.registrations
    where event_id=p_event_id and user_id=v_uid;
  if found and v_status = 'REGISTERED' then return; end if;
  if v_event.status <> 'PUBLISHED' or v_event.start_at <= now()
    or (v_event.registration_deadline is not null and v_event.registration_deadline < now()) then
    raise exception 'REGISTRATION_CLOSED';
  end if;
  if v_event.max_participants > 0 and v_event.current_registered >= v_event.max_participants then
    raise exception 'EVENT_FULL';
  end if;
  insert into public.registrations(event_id,user_id,student_id,display_name,email,status,registered_at)
  values(p_event_id,v_uid,v_profile.student_id,v_profile.display_name,v_profile.email,'REGISTERED',now())
  on conflict (event_id,user_id) do update
    set status='REGISTERED', registered_at=excluded.registered_at,
        student_id=excluded.student_id, display_name=excluded.display_name, email=excluded.email;
  update public.events set current_registered=current_registered+1 where id=p_event_id;
end;
$$;

create or replace function public.cancel_event_registration(p_event_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare v_uid uuid := (select auth.uid()); v_status public.registration_status;
begin
  if v_uid is null then raise exception 'AUTH_REQUIRED' using errcode='42501'; end if;
  perform 1 from public.events where id=p_event_id for update;
  if not found then raise exception 'EVENT_NOT_FOUND'; end if;
  select status into v_status from public.registrations
    where event_id=p_event_id and user_id=v_uid;
  if not found or v_status='CANCELLED' then return; end if;
  if exists (select 1 from public.attendance where event_id=p_event_id and user_id=v_uid) then
    raise exception 'ALREADY_CHECKED_IN';
  end if;
  update public.registrations set status='CANCELLED'
    where event_id=p_event_id and user_id=v_uid;
  update public.events set current_registered=current_registered-1 where id=p_event_id;
end;
$$;

-- Token signing secret never lives in an exposed schema/table or in Android.
-- The hmac functions are provided by pgcrypto (Supabase default: extensions).
create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;
create table if not exists app.qr_signing_keys (
  singleton boolean primary key default true check (singleton),
  secret bytea not null
);
revoke all on app.qr_signing_keys from public, anon, authenticated;
insert into app.qr_signing_keys(singleton,secret)
values (true, extensions.gen_random_bytes(32)) on conflict (singleton) do nothing;

create or replace function public.issue_checkin_token(p_event_id uuid)
returns text language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := (select auth.uid());
  v_event public.events%rowtype;
  v_payload text;
  v_secret bytea;
  v_nonce text;
  v_exp bigint;
begin
  if v_uid is null then raise exception 'AUTH_REQUIRED' using errcode='42501'; end if;
  if not exists (select 1 from public.registrations r
    where r.event_id=p_event_id and r.user_id=v_uid and r.status='REGISTERED') then
    raise exception 'NOT_REGISTERED';
  end if;
  if exists (select 1 from public.attendance a where a.event_id=p_event_id and a.user_id=v_uid) then
    raise exception 'ALREADY_CHECKED_IN';
  end if;
  select * into v_event from public.events where id=p_event_id;
  if not found or v_event.status not in ('PUBLISHED','HAPPENING')
    or now() < v_event.start_at - interval '30 minutes'
    or now() > v_event.end_at + interval '30 minutes' then
    raise exception 'CHECKIN_NOT_OPEN';
  end if;
  select secret into strict v_secret from app.qr_signing_keys where singleton;
  v_exp := extract(epoch from clock_timestamp() + interval '60 seconds')::bigint;
  v_nonce := encode(extensions.gen_random_bytes(16),'hex');
  v_payload := p_event_id::text || '.' || v_uid::text || '.' || v_exp::text || '.' || v_nonce;
  return v_payload || '.' || encode(extensions.hmac(convert_to(v_payload,'UTF8'), v_secret, 'sha256'),'hex');
end;
$$;

create or replace function public.check_in(p_token text)
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_actor uuid := (select auth.uid());
  v_parts text[];
  v_event_id uuid;
  v_student uuid;
  v_exp bigint;
  v_secret bytea;
  v_expected text;
  v_event public.events%rowtype;
begin
  if v_actor is null then raise exception 'AUTH_REQUIRED' using errcode='42501'; end if;
  if p_token is null or length(p_token) > 350 then raise exception 'INVALID_QR'; end if;
  v_parts := string_to_array(p_token,'.');
  if array_length(v_parts,1) <> 5
    or v_parts[1] !~ '^[0-9a-f-]{36}$'
    or v_parts[2] !~ '^[0-9a-f-]{36}$'
    or v_parts[3] !~ '^[0-9]{10,11}$'
    or v_parts[4] !~ '^[0-9a-f]{32}$'
    or v_parts[5] !~ '^[0-9a-f]{64}$' then
    raise exception 'INVALID_QR';
  end if;
  v_event_id := v_parts[1]::uuid;
  v_student := v_parts[2]::uuid;
  v_exp := v_parts[3]::bigint;
  if clock_timestamp() > to_timestamp(v_exp)
    or clock_timestamp() < to_timestamp(v_exp) - interval '60 seconds' then
    raise exception 'QR_EXPIRED';
  end if;
  select secret into strict v_secret from app.qr_signing_keys where singleton;
  v_expected := encode(extensions.hmac(
    convert_to(array_to_string(v_parts[1:4],'.'),'UTF8'),v_secret,'sha256'), 'hex');
  if v_expected <> v_parts[5] then raise exception 'INVALID_QR'; end if;
  select * into v_event from public.events where id=v_event_id for update;
  if not found then raise exception 'EVENT_NOT_FOUND'; end if;
  if not (public.is_admin() or (public.is_organizer() and v_event.organizer_id=v_actor)) then
    raise exception 'FORBIDDEN' using errcode='42501';
  end if;
  if v_event.status not in ('PUBLISHED','HAPPENING')
    or now() < v_event.start_at - interval '30 minutes'
    or now() > v_event.end_at + interval '30 minutes' then
    raise exception 'CHECKIN_NOT_OPEN';
  end if;
  if not exists (select 1 from public.registrations r where r.event_id=v_event_id
    and r.user_id=v_student and r.status='REGISTERED') then
    raise exception 'NOT_REGISTERED';
  end if;
  if exists (select 1 from public.attendance a where a.event_id=v_event_id and a.user_id=v_student) then
    raise exception 'ALREADY_CHECKED_IN';
  end if;
  insert into public.attendance(event_id,user_id,checked_in_by,status)
    values(v_event_id,v_student,v_actor,'PRESENT');
end;
$$;

-- Default function EXECUTE belongs to PUBLIC in PostgreSQL. Explicitly revoke
-- and re-grant only the authenticated RPC surface.
revoke all on function public.is_admin(), public.is_organizer(), public.my_student_id(),
  public.handle_new_user(), public.set_updated_at(),
  public.admin_verify_student(uuid,text), public.admin_set_user_role(uuid,public.user_role),
  public.register_for_event(uuid), public.cancel_event_registration(uuid),
  public.issue_checkin_token(uuid), public.check_in(text) from public, anon, authenticated;
grant execute on function public.is_admin(), public.is_organizer(),
  public.admin_verify_student(uuid,text), public.admin_set_user_role(uuid,public.user_role),
  public.register_for_event(uuid), public.cancel_event_registration(uuid),
  public.issue_checkin_token(uuid), public.check_in(text) to authenticated;

-- Legacy signup metadata must never grant access to schedules or grades.
-- Provision verified student IDs / user_id associations using trusted admin tooling.
