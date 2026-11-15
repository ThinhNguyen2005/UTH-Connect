-- Reviewable additive setup for the currently missing marketplace backend.
-- Requires public.profiles and Supabase Storage. Does not remove existing data.
create table if not exists public.book_listings (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references public.profiles(id) on delete restrict,
  title text not null check (length(btrim(title)) between 1 and 160),
  subject text not null check (length(btrim(subject)) between 1 and 100),
  condition text not null check (condition in ('NEW', 'GOOD', 'USED')),
  deal_type text not null check (deal_type in ('SALE', 'EXCHANGE')),
  price_vnd bigint check (
    (deal_type = 'SALE' and price_vnd is not null and price_vnd > 0) or
    (deal_type = 'EXCHANGE' and price_vnd is null)
  ),
  description text not null default '' check (length(description) <= 2000),
  seller_name text not null check (length(btrim(seller_name)) between 1 and 100),
  contact text not null check (length(btrim(contact)) between 5 and 254),
  image_path text,
  image_paths text[] not null default '{}',
  seller_avatar_url text,
  status text not null default 'AVAILABLE' check (status in ('AVAILABLE', 'SOLD')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists book_listings_created_idx on public.book_listings (created_at desc);
create index if not exists book_listings_owner_idx on public.book_listings (owner_user_id);
create index if not exists book_listings_status_idx on public.book_listings (status);
alter table public.book_listings enable row level security;
grant select on public.book_listings to authenticated;
grant insert (id, owner_user_id, title, subject, condition, deal_type, price_vnd, description, seller_name, contact, image_path, image_paths, seller_avatar_url, status, created_at, updated_at) on public.book_listings to authenticated;
grant update (title, subject, condition, deal_type, price_vnd, description, seller_name, contact, image_path, image_paths, seller_avatar_url, status, updated_at) on public.book_listings to authenticated;
grant delete on public.book_listings to authenticated;
create policy book_listings_read on public.book_listings for select to authenticated using (true);
create policy book_listings_own_insert on public.book_listings for insert to authenticated
  with check (owner_user_id = (select auth.uid()));
create policy book_listings_own_update on public.book_listings for update to authenticated
  using (owner_user_id = (select auth.uid())) with check (owner_user_id = (select auth.uid()));
create policy book_listings_own_delete on public.book_listings for delete to authenticated
  using (owner_user_id = (select auth.uid()));
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('book_listings', 'book_listings', true, 5242880, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do nothing;
create policy book_storage_read on storage.objects for select to authenticated
  using (bucket_id = 'book_listings');
create policy book_storage_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'book_listings' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy book_storage_update on storage.objects for update to authenticated
  using (bucket_id = 'book_listings' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'book_listings' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy book_storage_delete on storage.objects for delete to authenticated
  using (bucket_id = 'book_listings' and (storage.foldername(name))[1] = (select auth.uid())::text);
