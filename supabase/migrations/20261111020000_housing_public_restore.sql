-- ============================================================================
-- UTH Connect — Ghép trọ: bỏ cổng duyệt liên hệ, trả địa chỉ + liên hệ về public
-- Migration: 20261111020000_housing_public_restore.sql
-- ----------------------------------------------------------------------------
-- Quyết định sản phẩm (11/11/2026): app chỉ mở cho sinh viên UTH đã xác thực
-- (email @ut.edu.vn / @uth.edu.vn + MSSV), nên cổng contact-request ACCEPTED
-- chỉ thêm phiền: tin không địa chỉ thì không ai thuê, chờ duyệt lâu. Địa chỉ
-- đầy đủ, tọa độ chính xác và SĐT/Zalo trở lại đọc được với mọi authenticated.
--
-- Migration này hoàn tác schema của 20261111000000 + 20261111010000:
--   * trả 4 cột nhạy cảm về housing_posts, chép dữ liệu từ bảng private;
--   * address_label công khai nhận lại địa chỉ đầy đủ (đang bị reset rỗng);
--   * gỡ cột xấp xỉ, 6 RPC gate, 2 bảng private/request và enum.
-- Idempotent: chạy lại được trên Supabase Preview branch; bước chép dữ liệu
-- chỉ chạy khi bảng private còn tồn tại.
-- ============================================================================

begin;

-- 1. Trả cột public ----------------------------------------------------------
alter table public.housing_posts
    add column if not exists latitude double precision not null default 0.0,
    add column if not exists longitude double precision not null default 0.0,
    add column if not exists contact_phone text not null default '',
    add column if not exists contact_zalo_url text not null default '';

-- 2. Chép dữ liệu từ bảng private về (guard: chỉ chạy khi bảng còn) ----------
-- Trigger housing_guard_status chỉ chặn đổi status/report_count nên UPDATE
-- này pass; updated_at do trigger tự đặt.
do $$
begin
    if exists (
        select 1 from information_schema.tables
        where table_schema = 'public' and table_name = 'housing_private_details'
    ) then
        update public.housing_posts p
        set latitude = d.latitude,
            longitude = d.longitude,
            contact_phone = d.contact_phone,
            contact_zalo_url = d.contact_zalo_url,
            address_label = case
                when btrim(coalesce(d.exact_address, '')) <> '' then d.exact_address
                else p.address_label
            end
        from public.housing_private_details d
        where d.listing_id = p.id;
    end if;
end $$;

-- 3. Gỡ đối tượng của cổng duyệt ---------------------------------------------
drop function if exists public.reveal_housing_private_details(uuid);
drop function if exists public.create_housing_post(jsonb);
drop function if exists public.update_housing_post(uuid, jsonb);
drop function if exists public.request_housing_contact(uuid, text);
drop function if exists public.respond_housing_contact(uuid, boolean);
drop function if exists public.cancel_housing_contact(uuid);

drop table if exists public.housing_private_details;
drop table if exists public.housing_contact_requests;

do $$
begin
    if exists (select 1 from pg_type where typname = 'housing_contact_status') then
        drop type public.housing_contact_status;
    end if;
end $$;

-- 4. Gỡ cột xấp xỉ — app đọc lại tọa độ chính xác -----------------------------
alter table public.housing_posts
    drop column if exists approx_latitude,
    drop column if exists approx_longitude;

commit;