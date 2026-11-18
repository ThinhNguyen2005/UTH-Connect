alter table public.notification_dispatch_progress
  add column if not exists source_payload jsonb,
  add column if not exists fanout_complete boolean not null default false;

create index if not exists notification_dispatch_progress_incomplete_idx
  on public.notification_dispatch_progress(updated_at)
  where fanout_complete = false;
