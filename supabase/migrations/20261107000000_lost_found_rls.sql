-- Deploy to staging, verify the security contract with two real accounts, then approve production separately.
create table if not exists public.lost_found (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references public.profiles(id) on delete restrict,
  title text not null check (length(btrim(title)) between 1 and 200),
  description text not null check (length(btrim(description)) between 20 and 4000),
  kind text not null check (kind in ('LOST', 'FOUND')),
  category text not null check (category in ('STUDENT_CARD', 'ELECTRONICS', 'WALLET_PAPERS', 'PERSONAL_ITEMS', 'OTHER')),
  location text not null check (length(btrim(location)) between 1 and 250),
  contact_name text not null default '',
  contact_phone text not null check (length(btrim(contact_phone)) between 9 and 20),
  is_resolved boolean not null default false,
  created_at timestamptz not null default now()
);
create index if not exists lost_found_created_idx on public.lost_found (created_at desc);
create index if not exists lost_found_owner_idx on public.lost_found (owner_user_id);

alter table public.lost_found enable row level security;
revoke all on public.lost_found from anon, authenticated;
grant select, insert on public.lost_found to authenticated;
grant update (is_resolved) on public.lost_found to authenticated;

-- A user must be authenticated and verified by an admin to read community contact info.
drop policy if exists lost_found_verified_read on public.lost_found;
create policy lost_found_verified_read on public.lost_found for select to authenticated
  using (exists (select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.student_id_verified));
drop policy if exists lost_found_own_insert on public.lost_found;
create policy lost_found_own_insert on public.lost_found for insert to authenticated
  with check (owner_user_id = (select auth.uid()) and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.student_id_verified));
drop policy if exists lost_found_own_update on public.lost_found;
create policy lost_found_own_update on public.lost_found for update to authenticated
  using (owner_user_id = (select auth.uid()))
  with check (owner_user_id = (select auth.uid()));

-- Prevent direct writes to identity or the creation timestamp even if an older client submits them.
revoke update on public.lost_found from authenticated;
grant update (is_resolved) on public.lost_found to authenticated;
