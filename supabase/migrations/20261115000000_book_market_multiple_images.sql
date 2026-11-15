-- Preserve the original cover column for older clients and rows.
alter table public.book_listings
    add column if not exists image_paths text[] not null default '{}',
    add column if not exists seller_avatar_url text;

grant insert (image_paths, seller_avatar_url) on public.book_listings to authenticated;
grant update (image_paths, seller_avatar_url) on public.book_listings to authenticated;
