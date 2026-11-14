-- Keep per-inbox device progress durable so a bounded Edge Function invocation
-- can resume from the first device that has not been processed yet.
alter table public.notification_inbox
  add column if not exists push_device_cursor text;
