-- Run on a disposable local/staging Supabase database after applying migrations.
-- psql "$LOCAL_DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/media_uploads_contract.sql
begin;

do $$
begin
    assert exists (
        select 1 from storage.buckets where id = 'media_uploads'
        and public and file_size_limit = 153600 and allowed_mime_types = array['image/webp']
    ), 'media_uploads bucket limits differ from the Android contract';
end $$;

select set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111111', true);
set local role authenticated;

insert into storage.objects (bucket_id, name)
values ('media_uploads', '11111111-1111-4111-8111-111111111111/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa.webp');

do $$
declare
    invalid_path text;
    affected integer;
begin
    foreach invalid_path in array array[
        '22222222-2222-4222-8222-222222222222/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa.webp',
        '11111111-1111-4111-8111-111111111111/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa.jpg',
        '11111111-1111-4111-8111-111111111111/nested/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa.webp'
    ] loop
        begin
            insert into storage.objects (bucket_id, name) values ('media_uploads', invalid_path);
            raise exception 'Unexpected INSERT permission for %', invalid_path;
        exception when insufficient_privilege then null;
        end;
    end loop;

    update storage.objects set metadata = '{}'::jsonb where bucket_id = 'media_uploads';
    get diagnostics affected = row_count;
    assert affected = 0, 'Unexpected UPDATE permission';
    delete from storage.objects where bucket_id = 'media_uploads';
    get diagnostics affected = row_count;
    assert affected = 0, 'Unexpected DELETE permission';
end $$;

reset role;
select set_config('request.jwt.claim.sub', '', true);
set local role anon;
do $$
begin
    assert exists (
        select 1 from storage.objects where bucket_id = 'media_uploads'
        and name = '11111111-1111-4111-8111-111111111111/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa.webp'
    ), 'Public SELECT should be allowed';
    begin
        insert into storage.objects (bucket_id, name)
        values ('media_uploads', '11111111-1111-4111-8111-111111111111/bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb.webp');
        raise exception 'Unexpected anonymous INSERT permission';
    exception when insufficient_privilege then null;
    end;
end $$;

rollback;
-- MIME and size enforcement belong to the Storage HTTP API, not direct SQL INSERT.
