-- Restrict student notification reads and read-state updates to their own rows.
alter table public.notifications
  add constraint notifications_personal_type_check
  check (type in ('HOUSING_APPROVED', 'HOUSING_REJECTED', 'EVENT_REMINDER', 'SYSTEM'))
  not valid;

drop policy if exists "notifications_select" on public.notifications;
create policy notifications_select on public.notifications
  for select to authenticated
  using (
    (select public.is_admin())
    or (
      published_at <= statement_timestamp()
      and (
        user_id = (select auth.uid())
        or recipient_id = (select auth.uid())
        or (user_id is null and recipient_id is null)
      )
    )
  );

drop policy if exists "notifications_update" on public.notifications;
drop policy if exists notifications_update_own_read on public.notifications;
create policy notifications_update_own_read on public.notifications
  for update to authenticated
  using (
    user_id = (select auth.uid())
    or recipient_id = (select auth.uid())
  )
  with check (
    (user_id = (select auth.uid()) or recipient_id = (select auth.uid()))
    and is_read is true
  );

-- Preserve existing staff editing behavior; students use the own-row policy above.
create policy notifications_update_staff on public.notifications
  for update to authenticated
  using ((select public.is_admin()) or (select public.is_organizer()))
  with check ((select public.is_admin()) or (select public.is_organizer()));

create or replace function public.protect_notification_fields()
returns trigger
language plpgsql
as $$
begin
  if not (select public.is_admin() or public.is_organizer()) then
    if (to_jsonb(new) - 'is_read') is distinct from (to_jsonb(old) - 'is_read') then
      raise exception 'Students may only update notification read state'
        using errcode = '42501';
    end if;
    if new.is_read is not true then
      raise exception 'Students may only mark notifications as read'
        using errcode = '42501';
    end if;
  end if;
  return new;
end;
$$;
