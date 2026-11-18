begin;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('media_uploads', 'media_uploads', true, 153600, array['image/webp'])
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists media_uploads_public_read on storage.objects;
create policy media_uploads_public_read on storage.objects
    for select to anon, authenticated
    using (bucket_id = 'media_uploads');

drop policy if exists media_uploads_owner_insert on storage.objects;
create policy media_uploads_owner_insert on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'media_uploads'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and name ~ '^[^/]+/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.webp$'
    );

-- No UPDATE/DELETE grants: upload a new UUID rather than overwriting an object.
-- Bucket MIME checks are not a byte-level image validator; callers must encode WebP.
commit;
