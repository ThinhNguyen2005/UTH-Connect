alter table public.notification_inbox
  add column if not exists push_retry_device_id text;

comment on column public.notification_inbox.push_retry_device_id is
  'Device whose consecutive delivery failures are counted by push_attempts.';
