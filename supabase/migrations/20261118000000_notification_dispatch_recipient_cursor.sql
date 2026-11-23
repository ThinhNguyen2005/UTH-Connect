alter table public.notification_dispatch_progress
  add column if not exists last_recipient_id uuid;
