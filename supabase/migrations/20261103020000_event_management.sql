-- Event management is available only through authenticated, checked RPCs.
-- Keep the previous migrations intact for projects where they have run.

revoke insert, update on public.events from authenticated;
revoke insert (title, description, banner_url, location, campus, start_at,
               end_at, registration_deadline, max_participants, points,
               status, organizer_id, organizer_name) on public.events from authenticated;
revoke update (title, description, banner_url, location, campus, start_at,
               end_at, registration_deadline, max_participants, points,
               status, organizer_name) on public.events from authenticated;

create or replace function public.create_event(
  p_title text, p_description text, p_location text, p_campus text,
  p_start_at timestamptz, p_end_at timestamptz, p_max_participants integer
) returns uuid language plpgsql security definer set search_path = '' as $$
declare
  v_actor uuid := (select auth.uid());
  v_id uuid;
  v_name text;
begin
  if v_actor is null or not public.is_organizer() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if length(btrim(coalesce(p_title, ''))) < 3
    or length(p_title) > 180
    or length(btrim(coalesce(p_location, ''))) = 0
    or length(btrim(coalesce(p_campus, ''))) = 0
    or p_start_at is null or p_end_at is null
    or p_start_at <= now() or p_end_at <= p_start_at
    or p_max_participants is null or p_max_participants < 0 then
    raise exception 'INVALID_EVENT' using errcode = '22023';
  end if;
  select display_name into v_name from public.profiles where id = v_actor;
  insert into public.events(
    title,description,location,campus,start_at,end_at,max_participants,
    organizer_id,organizer_name,status
  ) values (
    btrim(p_title),coalesce(p_description,''),btrim(p_location),btrim(p_campus),
    p_start_at,p_end_at,p_max_participants,v_actor,v_name,'DRAFT'
  ) returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.edit_event(
  p_event_id uuid, p_title text, p_description text, p_location text,
  p_campus text, p_start_at timestamptz, p_end_at timestamptz,
  p_max_participants integer
) returns void language plpgsql security definer set search_path = '' as $$
declare
  v_actor uuid := (select auth.uid());
  v_event public.events%rowtype;
begin
  if v_actor is null or not public.is_organizer() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  select * into v_event from public.events where id = p_event_id for update;
  if not found then raise exception 'EVENT_NOT_FOUND'; end if;
  if not public.is_admin() and v_event.organizer_id is distinct from v_actor then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if v_event.status in ('COMPLETED','CANCELLED') then
    raise exception 'EVENT_LOCKED';
  end if;
  if length(btrim(coalesce(p_title, ''))) < 3
    or length(p_title) > 180
    or length(btrim(coalesce(p_location, ''))) = 0
    or length(btrim(coalesce(p_campus, ''))) = 0
    or p_start_at is null or p_end_at is null
    or p_end_at <= p_start_at
    or p_max_participants is null or p_max_participants < 0
    or (p_max_participants > 0 and p_max_participants < v_event.current_registered) then
    raise exception 'INVALID_EVENT' using errcode = '22023';
  end if;
  update public.events set title = btrim(p_title),
    description = coalesce(p_description,''), location = btrim(p_location),
    campus = btrim(p_campus), start_at = p_start_at, end_at = p_end_at,
    max_participants = p_max_participants
  where id = p_event_id;
end;
$$;

create or replace function public.set_event_status(
  p_event_id uuid, p_status public.event_status
) returns void language plpgsql security definer set search_path = '' as $$
declare
  v_actor uuid := (select auth.uid());
  v_event public.events%rowtype;
begin
  if v_actor is null or not public.is_organizer() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  select * into v_event from public.events where id = p_event_id for update;
  if not found then raise exception 'EVENT_NOT_FOUND'; end if;
  if not public.is_admin() and v_event.organizer_id is distinct from v_actor then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if p_status is null then raise exception 'INVALID_EVENT_STATUS'; end if;
  if p_status = v_event.status then return; end if;
  if not (
      (v_event.status = 'DRAFT' and p_status in ('PUBLISHED','CANCELLED'))
      or (v_event.status = 'PUBLISHED' and p_status in ('HAPPENING','CANCELLED'))
      or (v_event.status = 'HAPPENING' and p_status in ('COMPLETED','CANCELLED'))
    ) then
    raise exception 'INVALID_EVENT_TRANSITION';
  end if;
  if p_status = 'PUBLISHED' and v_event.start_at <= now() then
    raise exception 'EVENT_ALREADY_STARTED';
  end if;
  update public.events set status = p_status where id = p_event_id;
end;
$$;

revoke all on function public.create_event(text,text,text,text,timestamptz,timestamptz,integer),
  public.edit_event(uuid,text,text,text,text,timestamptz,timestamptz,integer),
  public.set_event_status(uuid,public.event_status) from public, anon, authenticated;
grant execute on function public.create_event(text,text,text,text,timestamptz,timestamptz,integer),
  public.edit_event(uuid,text,text,text,text,timestamptz,timestamptz,integer),
  public.set_event_status(uuid,public.event_status) to authenticated;
