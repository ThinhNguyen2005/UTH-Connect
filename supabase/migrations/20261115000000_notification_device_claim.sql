-- Register an installation without allowing one user to take another user's row.
create or replace function public.claim_notification_device(
  p_device_id text,
  p_token text,
  p_platform text default 'ANDROID'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  current_user_id uuid := auth.uid();
  device_owner_id uuid;
  token_owner_id uuid;
begin
  if current_user_id is null then
    raise exception 'AUTH_REQUIRED' using errcode = '28000';
  end if;
  if coalesce(btrim(p_device_id), '') = '' or coalesce(btrim(p_token), '') = '' then
    raise exception 'DEVICE_TOKEN_REQUIRED' using errcode = '22023';
  end if;
  if p_platform not in ('ANDROID', 'WEB') then
    raise exception 'UNSUPPORTED_DEVICE_PLATFORM' using errcode = '22023';
  end if;

  select user_id into device_owner_id
    from public.notification_devices
   where device_id = p_device_id
   for update;
  select user_id into token_owner_id
    from public.notification_devices
   where token = p_token
   for update;
  if (device_owner_id is not null and device_owner_id <> current_user_id)
      or (token_owner_id is not null and token_owner_id <> current_user_id) then
    raise exception 'DEVICE_OWNERSHIP_REQUIRED' using errcode = '42501';
  end if;

  delete from public.notification_devices
  where user_id = current_user_id and (device_id = p_device_id or token = p_token);

  insert into public.notification_devices (device_id, user_id, token, platform, updated_at)
  values (p_device_id, current_user_id, p_token, p_platform, now())
  on conflict (device_id) do update set
    token = excluded.token,
    platform = excluded.platform,
    updated_at = now()
    where public.notification_devices.user_id = current_user_id;
  if not exists (
    select 1 from public.notification_devices
     where device_id = p_device_id and user_id = current_user_id
  ) then
    raise exception 'DEVICE_OWNERSHIP_REQUIRED' using errcode = '42501';
  end if;
end;
$$;

revoke all on function public.claim_notification_device(text, text, text) from public, anon;
grant execute on function public.claim_notification_device(text, text, text) to authenticated;
