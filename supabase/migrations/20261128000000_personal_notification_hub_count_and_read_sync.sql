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
