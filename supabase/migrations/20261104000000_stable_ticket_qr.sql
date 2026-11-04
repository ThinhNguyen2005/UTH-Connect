-- Stable, server-signed ticket QR. A registered student can display it before
-- check-in opens; check_in_ticket_qr still enforces the event time window.
create or replace function public.issue_ticket_qr(p_event_id uuid)
returns text language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := (select auth.uid());
  v_secret bytea;
  v_payload text;
begin
  if v_uid is null then raise exception 'AUTH_REQUIRED' using errcode='42501'; end if;
  if not exists (select 1 from public.profiles p where p.id=v_uid
    and p.student_id_verified and p.student_id is not null
    and length(btrim(p.student_id)) between 5 and 32) then
    raise exception 'STUDENT_NOT_VERIFIED' using errcode='42501';
  end if;
  if not exists (select 1 from public.registrations r where r.event_id=p_event_id
    and r.user_id=v_uid and r.status='REGISTERED') then
    raise exception 'NOT_REGISTERED';
  end if;
  if not exists (select 1 from public.events e where e.id=p_event_id
    and e.status in ('PUBLISHED','HAPPENING')) then
    raise exception 'TICKET_UNAVAILABLE';
  end if;
  select secret into strict v_secret from app.qr_signing_keys where singleton;
  v_payload := p_event_id::text || '.' || v_uid::text;
  return v_payload || '.' || encode(extensions.hmac(convert_to(v_payload,'UTF8'),v_secret,'sha256'),'hex');
end;
$$;

create or replace function public.check_in_ticket_qr(p_token text)
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_actor uuid := (select auth.uid());
  v_parts text[];
  v_event_id uuid;
  v_student uuid;
  v_secret bytea;
  v_expected text;
  v_event public.events%rowtype;
begin
  if v_actor is null then raise exception 'AUTH_REQUIRED' using errcode='42501'; end if;
  if p_token is null or length(p_token)>220 then raise exception 'INVALID_QR'; end if;
  v_parts := string_to_array(p_token,'.');
  if coalesce(array_length(v_parts,1),0)<>3
    or v_parts[1] !~ '^[0-9a-f-]{36}$'
    or v_parts[2] !~ '^[0-9a-f-]{36}$'
    or v_parts[3] !~ '^[0-9a-f]{64}$' then raise exception 'INVALID_QR'; end if;
  v_event_id := v_parts[1]::uuid;
  v_student := v_parts[2]::uuid;
  select secret into strict v_secret from app.qr_signing_keys where singleton;
  v_expected := encode(extensions.hmac(convert_to(v_parts[1]||'.'||v_parts[2],'UTF8'),v_secret,'sha256'),'hex');
  if v_expected<>v_parts[3] then raise exception 'INVALID_QR'; end if;
  if not exists (select 1 from public.profiles p where p.id=v_student
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
    or now()<v_event.start_at-interval '30 minutes'
    or now()>v_event.end_at+interval '30 minutes' then raise exception 'CHECKIN_NOT_OPEN'; end if;
  if not exists (select 1 from public.registrations r where r.event_id=v_event_id
    and r.user_id=v_student and r.status='REGISTERED') then raise exception 'NOT_REGISTERED'; end if;
  if exists (select 1 from public.attendance a where a.event_id=v_event_id and a.user_id=v_student) then
    raise exception 'ALREADY_CHECKED_IN';
  end if;
  insert into public.attendance(event_id,user_id,checked_in_by,status)
    values(v_event_id,v_student,v_actor,'PRESENT');
end;
$$;

revoke all on function public.issue_ticket_qr(uuid), public.check_in_ticket_qr(text) from public, anon, authenticated;
grant execute on function public.issue_ticket_qr(uuid), public.check_in_ticket_qr(text) to authenticated;
