-- ============================================================================
-- UTH Connect — Ghép trọ: chốt chặn tầng INSERT cho amenities và report_count
-- Migration: 20261113020000_housing_insert_guards.sql
-- ----------------------------------------------------------------------------
-- Review CodeRabbit lượt 4 (13/11):
--   * amenities trước đây nhận mọi phần tử text — client lỗi/giả mạo có thể
--     lưu giá trị ngoài enum RoomAmenity làm sập mapper phía app. Thêm CHECK
--     containment (kèm chặn phần tử NULL vì <@ không tự loại NULL).
--   * guard status/report_count chỉ chạy BEFORE UPDATE: INSERT vẫn nhận
--     report_count client gửi kèm, đủ 3 là tin mới tự ẩn. Trigger BEFORE INSERT
--     riêng luôn đưa số đếm về 0 — chỉ RPC báo tin được tăng nó.
--   * Dọn phần tử amenities hỏng của tin cũ TRƯỚC khi bật constraint, giữ đúng
--     thứ tự (WITH ORDINALITY) như cách dọn ảnh ở 20261112020000.
-- Bản rebuild 20261108000000 đã mang cùng constraint + trigger cho deploy mới;
-- migration này áp dụng cho staging đang chạy. Idempotent: chạy lại không lỗi.
-- ============================================================================

begin;

-- 1. Dọn phần tử amenities ngoài enum / NULL của tin cũ ----------------------
-- Chỉ đụng hàng vi phạm; hàng sạch giữ nguyên (không phí lượt trigger).
update public.housing_posts
set amenities = coalesce(
    (
        select array_agg(item order by ord)
        from unnest(amenities) with ordinality as t(item, ord)
        where item is not null
            and item in (
                'MEZZANINE', 'AIR_CONDITIONER', 'MACHINE_WASH', 'FREE_HOURS',
                'GOVERNMENT_UTILITY_RATES', 'SECURED_PARKING', 'COOKING',
                'HIGH_SPEED_WIFI', 'HOT_WATER', 'BALCONY', 'KEY_CARD'
            )
    ),
    '{}'
)
where exists (
    select 1 from unnest(amenities) as item
    where item is null
        or item not in (
            'MEZZANINE', 'AIR_CONDITIONER', 'MACHINE_WASH', 'FREE_HOURS',
            'GOVERNMENT_UTILITY_RATES', 'SECURED_PARKING', 'COOKING',
            'HIGH_SPEED_WIFI', 'HOT_WATER', 'BALCONY', 'KEY_CARD'
        )
);

-- 2. CHECK amenities khớp enum RoomAmenity phía Android -----------------------
do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'housing_posts_amenities_check'
            and conrelid = 'public.housing_posts'::regclass
    ) then
        alter table public.housing_posts
            add constraint housing_posts_amenities_check check (
                amenities <@ array[
                    'MEZZANINE', 'AIR_CONDITIONER', 'MACHINE_WASH', 'FREE_HOURS',
                    'GOVERNMENT_UTILITY_RATES', 'SECURED_PARKING', 'COOKING',
                    'HIGH_SPEED_WIFI', 'HOT_WATER', 'BALCONY', 'KEY_CARD'
                ]::text[] and array_position(amenities, null) is null
            );
    end if;
end $$;

-- 3. report_count luôn khởi điểm 0 khi INSERT ---------------------------------
create or replace function public.trg_housing_reset_report_count()
returns trigger
language plpgsql
as $$
begin
    new.report_count := 0;
    return new;
end;
$$;

drop trigger if exists housing_reset_report_count on public.housing_posts;
create trigger housing_reset_report_count
    before insert on public.housing_posts
    for each row execute function public.trg_housing_reset_report_count();

commit;