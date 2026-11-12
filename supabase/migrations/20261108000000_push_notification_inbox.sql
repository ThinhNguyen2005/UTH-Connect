-- Persistent per-user inbox and installation tokens for FCM (the same token table
-- accepts WEB registrations later without putting Firebase credentials in clients).
create table if not exists public.notification_devices (
  device_id text primary key,
  user_id uuid not null references public.profiles(id) on delete cascade,
  token text not null unique,
  platform text not null check (platform in ('ANDROID', 'WEB')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists notification_devices_user_id_idx
  on public.notification_devices(user_id) where user_id is not null;
alter table public.notification_devices enable row level security;
revoke all on public.notification_devices from anon;
grant select, insert, update, delete on public.notification_devices to authenticated;
grant all on public.notification_devices to service_role;

drop policy if exists notification_devices_select_own on public.notification_devices;
create policy notification_devices_select_own on public.notification_devices
  for select to authenticated using (user_id = (select auth.uid()));
drop policy if exists notification_devices_insert_own on public.notification_devices;
create policy notification_devices_insert_own on public.notification_devices
  for insert to authenticated with check (user_id = (select auth.uid()));
drop policy if exists notification_devices_update_own on public.notification_devices;
create policy notification_devices_update_own on public.notification_devices
  for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));
drop policy if exists notification_devices_delete_own on public.notification_devices;
create policy notification_devices_delete_own on public.notification_devices
  for delete to authenticated using (user_id = (select auth.uid()));

create table if not exists public.notification_inbox (
  id uuid primary key default gen_random_uuid(),
  recipient_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  summary text not null default '',
  content text not null default '',
  category public.notification_category not null default 'GENERAL',
  author text not null default '',
  target_type text not null check (target_type in ('EVENT', 'ACADEMIC', 'HOUSING', 'SYSTEM')),
  target_id uuid,
  source_table text not null,
  source_id uuid not null,
  dedupe_key text not null,
  published_at timestamptz not null default now(),
  read_at timestamptz,
  push_status text not null default 'PENDING' check (push_status in ('PENDING', 'PROCESSING', 'SENT')),
  push_claimed_at timestamptz,
  constraint notification_inbox_recipient_dedupe unique (recipient_id, dedupe_key)
);
create index if not exists notification_inbox_recipient_published_idx
  on public.notification_inbox(recipient_id, published_at desc);
create index if not exists notification_inbox_pending_push_idx
  on public.notification_inbox(source_table, source_id, push_status, push_claimed_at)
  where push_status <> 'SENT';
alter table public.notification_inbox enable row level security;
revoke all on public.notification_inbox from anon;
grant select on public.notification_inbox to authenticated;
grant update (read_at) on public.notification_inbox to authenticated;
grant all on public.notification_inbox to service_role;

drop policy if exists notification_inbox_read_own on public.notification_inbox;
create policy notification_inbox_read_own on public.notification_inbox
  for select to authenticated using (recipient_id = (select auth.uid()));
drop policy if exists notification_inbox_mark_own_read on public.notification_inbox;
create policy notification_inbox_mark_own_read on public.notification_inbox
  for update to authenticated
  using (recipient_id = (select auth.uid()))
  with check (recipient_id = (select auth.uid()));
