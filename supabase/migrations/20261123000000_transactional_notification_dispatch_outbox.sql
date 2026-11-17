-- Persist dispatch work in the same transaction as eligible source changes.
-- This lets DISPATCH_PENDING recover work even when pg_net or the initial
-- Edge Function invocation fails before it can create a progress row.

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;

create or replace function private.enqueue_notification_dispatch_outbox()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  should_enqueue boolean := false;
  source_payload jsonb;
begin
  if tg_table_name = 'events' then
    if tg_op = 'INSERT' then
      should_enqueue := new.status::text = 'PUBLISHED';
    else
      should_enqueue := new.status::text = 'PUBLISHED'
        and old.status::text is distinct from 'PUBLISHED';
    end if;
  elsif tg_table_name = 'notifications' then
    if tg_op = 'INSERT' then
      should_enqueue := new.published_at <= statement_timestamp();
    else
      should_enqueue := old.published_at > statement_timestamp()
        and new.published_at <= statement_timestamp();
    end if;
  elsif tg_table_name = 'housing_posts' then
    if tg_op = 'INSERT' then
      should_enqueue := new.status = 'OPEN';
    else
      should_enqueue := new.status = 'OPEN' and old.status is distinct from 'OPEN';
    end if;
  end if;

  if not should_enqueue then
    return new;
  end if;

  source_payload := jsonb_build_object(
    'type', tg_op,
    'schema', 'public',
    'table', tg_table_name,
    'record', to_jsonb(new),
    'old_record', case
      when tg_op = 'UPDATE' and tg_table_name <> 'notifications' then to_jsonb(old)
      else null
    end
  );

  insert into public.notification_dispatch_progress (
    dedupe_key,
    source_payload,
    fanout_complete,
    updated_at
  ) values (
    tg_table_name || ':' || new.id::text,
    source_payload,
    false,
    statement_timestamp()
  )
  on conflict (dedupe_key) do nothing;

  return new;
end;
$$;

revoke all on function private.enqueue_notification_dispatch_outbox() from public, anon, authenticated;

drop trigger if exists enqueue_notification_dispatch_outbox on public.events;
create trigger enqueue_notification_dispatch_outbox
  after insert or update of status on public.events
  for each row execute function private.enqueue_notification_dispatch_outbox();

drop trigger if exists enqueue_notification_dispatch_outbox on public.notifications;
create trigger enqueue_notification_dispatch_outbox
  after insert or update of published_at on public.notifications
  for each row execute function private.enqueue_notification_dispatch_outbox();

drop trigger if exists enqueue_notification_dispatch_outbox on public.housing_posts;
create trigger enqueue_notification_dispatch_outbox
  after insert or update of status on public.housing_posts
  for each row execute function private.enqueue_notification_dispatch_outbox();
