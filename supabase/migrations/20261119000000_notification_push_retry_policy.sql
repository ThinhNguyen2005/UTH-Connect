alter table public.notification_inbox
  add column if not exists push_attempts integer not null default 0 check (push_attempts >= 0),
  add column if not exists push_retry_at timestamptz;

alter table public.notification_inbox
  drop constraint if exists notification_inbox_push_status_check;

alter table public.notification_inbox
  add constraint notification_inbox_push_status_check
  check (push_status in ('PENDING', 'PROCESSING', 'SENT', 'FAILED'));

create index if not exists notification_inbox_due_push_idx
  on public.notification_inbox(push_retry_at, id)
  where push_status = 'PENDING';
