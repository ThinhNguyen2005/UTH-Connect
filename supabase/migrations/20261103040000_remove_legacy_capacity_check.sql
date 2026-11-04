-- Defensive follow-up for databases that applied the original capacity CHECK.
-- The old condition "current_registered <= max_participants" treats zero as a
-- zero-seat event. The current application contract treats zero as unlimited.
-- Keep events_capacity_consistent from 20261103010000 (which supports zero).
do $$
declare v_name name;
begin
  for v_name in
    select c.conname from pg_constraint c
    where c.conrelid = 'public.events'::regclass and c.contype = 'c'
      and pg_get_constraintdef(c.oid) like '%current_registered <= max_participants%'
      and pg_get_constraintdef(c.oid) not like '%max_participants = 0%'
  loop
    execute format('alter table public.events drop constraint %I', v_name);
  end loop;
end;
$$;

-- Counted registrations are queried by event and active status.
create index if not exists registrations_event_status_idx
  on public.registrations (event_id, status);
