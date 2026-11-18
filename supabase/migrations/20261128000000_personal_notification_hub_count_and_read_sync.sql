-- Keep the notification badge exact with one RLS-scoped count query.
create or replace function public.count_unread_user_notifications()
returns bigint
language sql
stable
security invoker
set search_path = ''
as $$
  select count(*)
  from public.notifications
  where is_read is false
    and (
      user_id = (select auth.uid())
      or recipient_id = (select auth.uid())
    );
$$;

revoke all on function public.count_unread_user_notifications() from public, anon;
grant execute on function public.count_unread_user_notifications() to authenticated;

-- Keep the targeted notification row and its per-user push inbox copy aligned.
-- Campus-wide notifications have shared read state and are intentionally excluded.
create or replace function public.sync_notification_read_to_inbox()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
  target_user_id uuid := coalesce(new.recipient_id, new.user_id);
begin
  if target_user_id is not null and target_user_id = (select auth.uid()) then
    update public.notification_inbox
    set read_at = statement_timestamp()
    where source_table = 'notifications'
      and source_id = new.id
      and recipient_id = target_user_id
      and read_at is null;
  end if;
  return new;
end;
$$;

-- Inbox copies created after a notification was read must inherit that state.
-- Lock the source row so inserts and hub read updates cannot race past each other.
create or replace function public.sync_notification_read_on_inbox_insert()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
  source_is_read boolean;
begin
  if new.source_table = 'notifications' then
    select n.is_read
    into source_is_read
    from public.notifications as n
    where n.id = new.source_id
      and coalesce(n.recipient_id, n.user_id) = new.recipient_id
    for update;

    if found and source_is_read then
      new.read_at := coalesce(new.read_at, statement_timestamp());
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists sync_notification_read_on_inbox_insert on public.notification_inbox;
create trigger sync_notification_read_on_inbox_insert
before insert on public.notification_inbox
for each row
execute function public.sync_notification_read_on_inbox_insert();

drop trigger if exists sync_notification_read_to_inbox on public.notifications;
create trigger sync_notification_read_to_inbox
after update of is_read on public.notifications
for each row
when (old.is_read is distinct from new.is_read and new.is_read is true)
execute function public.sync_notification_read_to_inbox();

create or replace function public.sync_inbox_read_to_notification()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.source_table = 'notifications' then
    update public.notifications
    set is_read = true
    where id = new.source_id
      and coalesce(recipient_id, user_id) = new.recipient_id
      and is_read is false;
  end if;
  return new;
end;
$$;

drop trigger if exists sync_inbox_read_to_notification on public.notification_inbox;
create trigger sync_inbox_read_to_notification
after update of read_at on public.notification_inbox
for each row
when (old.read_at is null and new.read_at is not null)
execute function public.sync_inbox_read_to_notification();

-- The two after-row triggers above touch the other table. Serialize read-state
-- UPDATE statements before either statement acquires row locks, preventing the
-- opposite update paths from forming a notification/inbox lock cycle.
create or replace function public.serialize_notification_read_updates()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  perform pg_catalog.pg_advisory_xact_lock(20261128, 1);
  return null;
end;
$$;

drop trigger if exists serialize_notification_read_update on public.notifications;
create trigger serialize_notification_read_update
before update of is_read on public.notifications
for each statement
execute function public.serialize_notification_read_updates();

drop trigger if exists serialize_notification_inbox_read_update on public.notification_inbox;
create trigger serialize_notification_inbox_read_update
before update of read_at on public.notification_inbox
for each statement
execute function public.serialize_notification_read_updates();

-- Reconcile rows read through the push inbox before this migration was applied.
update public.notifications as n
set is_read = true
where n.is_read is false
  and coalesce(n.recipient_id, n.user_id) is not null
  and exists (
    select 1
    from public.notification_inbox as i
    where i.source_table = 'notifications'
      and i.source_id = n.id
      and i.recipient_id = coalesce(n.recipient_id, n.user_id)
      and i.read_at is not null
  );
