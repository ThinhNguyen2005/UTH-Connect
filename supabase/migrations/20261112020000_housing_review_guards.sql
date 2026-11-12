-- ============================================================================
-- UTH Connect — Ghép trọ: chặn URL lạ khi ghi + khóa đọc tin OPEN cho SV đã xác thực
-- Migration: 20261112020000_housing_review_guards.sql
-- ----------------------------------------------------------------------------
-- Review GitHub 12/11:
--   * images/contact_zalo_url trước đây nhận mọi chuỗi — tin có thể tham chiếu
--     ảnh ngoài bucket hoặc link Zalo mở sang miền lạ. CHECK không chứa được
--     subquery nên dùng trigger BEFORE INSERT OR UPDATE (chặn MỌI đường ghi,
--     kể cả PostgREST trực tiếp; RPC create/update cũ đã gỡ ở bản public_restore).
--     Regex ảnh chỉ khóa tên bucket + host Supabase, không khóa project ref để
--     chạy được cả trên Preview branch.
--   * policy select cũ cho mọi authenticated đọc tin OPEN; app vốn chỉ mở cho
--     sinh viên UTH đã xác thực (email trường + MSSV) nên siết thêm ở tầng DB:
--     OPEN chỉ đọc được khi profiles.student_id_verified = true, chủ tin vẫn
--     đọc tin của mình ở mọi trạng thái.
-- Idempotent: create or replace + drop/create trigger/policy, chạy lại được.
-- ============================================================================

begin;

-- 1. Trigger chặn URL ngoài bucket / ngoài miền Zalo -------------------------
create or replace function public.trg_housing_url_guards()
returns trigger
language plpgsql
as $$
declare
    v_image text;
begin
    foreach v_image in array coalesce(new.images, '{}') loop
        if v_image !~ '^https://[a-z0-9-]+\.supabase\.co/storage/v1/object/public/housing-images/' then
            raise exception 'HOUSING_IMAGE_BUCKET_INVALID' using errcode = 'P0001';
        end if;
    end loop;
    if coalesce(new.contact_zalo_url, '') <> ''
        and new.contact_zalo_url !~* '^https://zalo\.me(/|$)' then
        raise exception 'HOUSING_ZALO_URL_INVALID' using errcode = 'P0001';
    end if;
    return new;
end;
$$;

drop trigger if exists housing_url_guards on public.housing_posts;
create trigger housing_url_guards
    before insert or update on public.housing_posts
    for each row execute function public.trg_housing_url_guards();

-- 2. Tin OPEN chỉ cho hồ sơ đã xác thực MSSV đọc ------------------------------
-- Exists chéo sang profiles (không self-table) để né recursion 42P17.
drop policy if exists housing_posts_select on public.housing_posts;
create policy housing_posts_select on public.housing_posts
    for select to authenticated
    using (
        author_id = (select auth.uid())
        or (
            status = 'OPEN'
            and exists (
                select 1 from public.profiles p
                where p.id = (select auth.uid()) and p.student_id_verified = true
            )
        )
    );

commit;