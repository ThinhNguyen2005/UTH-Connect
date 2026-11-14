-- Reclaim an installation's device row when a previous session ended implicitly
-- (process death, expired refresh token, or an offline logout).
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

  delete from public.notification_devices
  where device_id = p_device_id or token = p_token;

  insert into public.notification_devices (device_id, user_id, token, platform, updated_at)
  values (p_device_id, current_user_id, p_token, p_platform, now())
  on conflict (device_id) do update set
    user_id = excluded.user_id,
    token = excluded.token,
    platform = excluded.platform,
    updated_at = now();
end;
$$;

revoke all on function public.claim_notification_device(text, text, text) from public, anon;
grant execute on function public.claim_notification_device(text, text, text) to authenticated;
