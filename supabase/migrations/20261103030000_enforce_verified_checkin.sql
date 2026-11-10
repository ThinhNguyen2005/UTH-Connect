-- Supplementary hardening for projects that already applied earlier migrations.
-- A verified student must have a non-empty, properly assigned student number.
-- NOT VALID preserves legacy inconsistent rows for a separate administrative
-- review, while PostgreSQL enforces the condition for all future writes.
alter table public.profiles add constraint profiles_verified_student_id_present
  check (not student_id_verified or
         (student_id is not null and length(btrim(student_id)) between 5 and 32))
  not valid;

create or replace function public.admin_verify_student(p_user_id uuid, p_student_id text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_student_id text := btrim(p_student_id);
begin
  if (select auth.uid()) is null or not public.is_admin() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if v_student_id is null or length(v_student_id) not between 5 and 32 then
    raise exception 'INVALID_STUDENT_ID' using errcode = '22023';
  end if;
  if exists (select 1 from public.profiles where id = p_user_id
    and student_id_verified and student_id is distinct from v_student_id) then
    raise exception 'STUDENT_ID_ALREADY_VERIFIED' using errcode = '42501';
  end if;
  update public.profiles set student_id = v_student_id, student_id_verified = true
    where id = p_user_id;
  if not found then raise exception 'USER_NOT_FOUND'; end if;
end;
$$;

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
  if not found or not v_profile.student_id_verified
    or v_profile.student_id is null
    or length(btrim(v_profile.student_id)) not between 5 and 32 then
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

-- A registration created before a student's identity was verified must not
-- be enough to obtain a live QR or be checked in later.
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
  if not exists (select 1 from public.profiles p where p.id = v_uid
    and p.student_id_verified and p.student_id is not null
    and length(btrim(p.student_id)) between 5 and 32) then
    raise exception 'STUDENT_NOT_VERIFIED' using errcode='42501';
  end if;
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
  if coalesce(array_length(v_parts,1),0) <> 5
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
  if not exists (select 1 from public.profiles p where p.id = v_student
    and p.student_id_verified and p.student_id is not null
    and length(btrim(p.student_id)) between 5 and 32) then
    raise exception 'STUDENT_NOT_VERIFIED' using errcode='42501';
  end if;
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

-- CREATE OR REPLACE preserves the explicitly restricted EXECUTE grants
-- established by 20261103010000_secure_core_workflows.sql.
