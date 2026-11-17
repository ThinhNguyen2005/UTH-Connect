alter table public.notification_dispatch_progress
  add column if not exists recovery_attempts integer not null default 0 check (recovery_attempts >= 0),
  add column if not exists recovery_retry_at timestamptz,
  add column if not exists fanout_failed boolean not null default false;

create index if not exists notification_dispatch_progress_recovery_due_idx
  on public.notification_dispatch_progress(recovery_retry_at, updated_at)
  where fanout_complete = false and fanout_failed = false;
