-- Claim due scheduled notifications that have never started inbox fan-out.
create index if not exists notifications_scheduled_publish_idx
  on public.notifications (published_at, id)
  where published_at > created_at;

-- Do not replay scheduled notifications that were already due before this
-- recovery job was introduced. Future scheduled rows remain eligible.
insert into public.notification_dispatch_progress (
  dedupe_key,
  fanout_complete,
  updated_at
)
select
  'notifications:' || notification.id::text,
  true,
  statement_timestamp()
from public.notifications as notification
where notification.published_at > notification.created_at
  and notification.published_at <= statement_timestamp()
on conflict (dedupe_key) do nothing;

create or replace function public.claim_due_notification_fanouts(p_limit integer default 10)
returns table(source_payload jsonb)
language sql
security invoker
set search_path = ''
as $$
  with due_notifications as materialized (
    select notification.*
    from public.notifications as notification
    where notification.published_at <= now()
      and notification.published_at > notification.created_at
      and not exists (
        select 1
        from public.notification_dispatch_progress as progress
        where progress.dedupe_key = 'notifications:' || notification.id::text
      )
    order by notification.published_at, notification.id
    limit least(greatest(coalesce(p_limit, 10), 1), 50)
    for update of notification skip locked
  ), claimed as (
    insert into public.notification_dispatch_progress as progress (
      dedupe_key,
      source_payload,
      fanout_complete,
      updated_at
    )
    select
      'notifications:' || notification.id::text,
      jsonb_build_object(
        'type', 'UPDATE',
        'schema', 'public',
        'table', 'notifications',
        'record', to_jsonb(notification),
        'old_record', null
      ),
      false,
      now()
    from due_notifications as notification
    on conflict (dedupe_key) do nothing
    returning progress.source_payload
  )
  select claimed.source_payload
  from claimed;
$$;

revoke all on function public.claim_due_notification_fanouts(integer) from public, anon, authenticated;
grant execute on function public.claim_due_notification_fanouts(integer) to service_role;
