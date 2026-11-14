create table if not exists public.notification_dispatch_progress (
  dedupe_key text primary key,
  next_recipient_offset integer not null default 0 check (next_recipient_offset >= 0),
  updated_at timestamptz not null default now()
);

create index if not exists notification_dispatch_progress_updated_idx
  on public.notification_dispatch_progress(updated_at);

alter table public.notification_dispatch_progress enable row level security;
revoke all on public.notification_dispatch_progress from anon, authenticated;
grant all on public.notification_dispatch_progress to service_role;
