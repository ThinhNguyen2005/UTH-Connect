alter table public.notification_inbox
  add column if not exists push_delivered boolean not null default false;
