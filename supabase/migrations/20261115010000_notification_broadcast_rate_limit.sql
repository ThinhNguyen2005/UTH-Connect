-- Bound application-wide housing broadcasts by author instead of relying only
-- on the concurrent OPEN-listing limit.
create table if not exists public.notification_broadcast_limits (
  author_id uuid primary key references public.profiles(id) on delete cascade,
  window_started_at timestamptz not null default now(),
  broadcast_count integer not null default 0 check (broadcast_count >= 0),
  last_source_id uuid
);

alter table public.notification_broadcast_limits enable row level security;
revoke all on public.notification_broadcast_limits from anon, authenticated;
grant all on public.notification_broadcast_limits to service_role;

create table if not exists public.notification_broadcast_events (
  author_id uuid not null references public.profiles(id) on delete cascade,
  source_id uuid not null,
  accepted boolean not null,
  created_at timestamptz not null default now(),
  primary key (author_id, source_id)
);

alter table public.notification_broadcast_events enable row level security;
revoke all on public.notification_broadcast_events from anon, authenticated;
grant all on public.notification_broadcast_events to service_role;

create or replace function public.claim_housing_notification_broadcast(p_author_id uuid, p_source_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_started_at timestamptz;
  v_count integer;
  v_accepted boolean;
begin
  if p_author_id is null or p_source_id is null then
    return false;
  end if;

  insert into public.notification_broadcast_limits (author_id, broadcast_count, last_source_id)
  values (p_author_id, 0, null)
  on conflict (author_id) do nothing;

  select window_started_at, broadcast_count
    into v_started_at, v_count
    from public.notification_broadcast_limits
   where author_id = p_author_id
   for update;

  select accepted
    into v_accepted
    from public.notification_broadcast_events
   where author_id = p_author_id and source_id = p_source_id;
  if found then
    return v_accepted;
  end if;

  if v_started_at <= now() - interval '24 hours' then
    update public.notification_broadcast_limits
       set window_started_at = now(), broadcast_count = 0, last_source_id = null
     where author_id = p_author_id;
    v_count := 0;
  end if;

  if v_count >= 3 then
    insert into public.notification_broadcast_events (author_id, source_id, accepted)
    values (p_author_id, p_source_id, false);
    return false;
  end if;

  update public.notification_broadcast_limits
     set broadcast_count = broadcast_count + 1, last_source_id = p_source_id
   where author_id = p_author_id;
  insert into public.notification_broadcast_events (author_id, source_id, accepted)
  values (p_author_id, p_source_id, true);
  return true;
end;
$$;

revoke all on function public.claim_housing_notification_broadcast(uuid, uuid) from public, anon, authenticated;
grant execute on function public.claim_housing_notification_broadcast(uuid, uuid) to service_role;
