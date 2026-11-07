-- Preserve the event fields collected by the organizer form.
alter table public.events
  add column if not exists category text not null default '',
  add column if not exists managers text[] not null default '{}';

create or replace function public.create_event_v2(
  p_title text, p_description text, p_location text, p_campus text,
  p_start_at timestamptz, p_end_at timestamptz, p_max_participants integer,
  p_points integer, p_organizer_name text, p_category text, p_managers text[]
) returns uuid language plpgsql security definer set search_path = '' as $$
declare
  v_actor uuid := (select auth.uid());
  v_id uuid;
  v_name text;
begin
  if v_actor is null or not public.is_organizer() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;
  if length(btrim(coalesce(p_title, ''))) < 3 or length(p_title) > 180
    or length(btrim(coalesce(p_location, ''))) = 0 or length(btrim(coalesce(p_campus, ''))) = 0
    or p_start_at is null or p_end_at is null or p_start_at <= now() or p_end_at <= p_start_at
    or p_max_participants is null or p_max_participants < 0 or p_points is null or p_points < 0 then
    raise exception 'INVALID_EVENT' using errcode = '22023';
  end if;
  select display_name into v_name from public.profiles where id = v_actor;
  insert into public.events(
    title,description,location,campus,start_at,end_at,max_participants,points,
    organizer_id,organizer_name,category,managers,status
  ) values (
    btrim(p_title),coalesce(p_description,''),btrim(p_location),btrim(p_campus),
    p_start_at,p_end_at,p_max_participants,p_points,v_actor,
    coalesce(nullif(btrim(p_organizer_name),''),v_name),coalesce(p_category,''),coalesce(p_managers,'{}'),'DRAFT'
  ) returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.edit_event_v2(
  p_event_id uuid, p_title text, p_description text, p_location text,
  p_campus text, p_start_at timestamptz, p_end_at timestamptz,
  p_max_participants integer, p_points integer, p_organizer_name text,
  p_category text, p_managers text[]
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
  if v_event.status in ('COMPLETED','CANCELLED') then raise exception 'EVENT_LOCKED'; end if;
  if length(btrim(coalesce(p_title, ''))) < 3 or length(p_title) > 180
    or length(btrim(coalesce(p_location, ''))) = 0 or length(btrim(coalesce(p_campus, ''))) = 0
    or p_start_at is null or p_end_at is null or p_end_at <= p_start_at
    or p_max_participants is null or p_max_participants < 0
    or (p_max_participants > 0 and p_max_participants < v_event.current_registered)
    or p_points is null or p_points < 0 then
    raise exception 'INVALID_EVENT' using errcode = '22023';
  end if;
  update public.events set title = btrim(p_title), description = coalesce(p_description,''),
    location = btrim(p_location), campus = btrim(p_campus), start_at = p_start_at,
    end_at = p_end_at, max_participants = p_max_participants, points = p_points,
    organizer_name = coalesce(nullif(btrim(p_organizer_name),''),organizer_name),
    category = coalesce(p_category,''), managers = coalesce(p_managers,'{}')
  where id = p_event_id;
end;
$$;

revoke all on function public.create_event_v2(text,text,text,text,timestamptz,timestamptz,integer,integer,text,text,text[]),
  public.edit_event_v2(uuid,text,text,text,text,timestamptz,timestamptz,integer,integer,text,text,text[])
  from public, anon, authenticated;
grant execute on function public.create_event_v2(text,text,text,text,timestamptz,timestamptz,integer,integer,text,text,text[]),
  public.edit_event_v2(uuid,text,text,text,text,timestamptz,timestamptz,integer,integer,text,text,text[])
  to authenticated;
