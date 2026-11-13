-- ============================================================================
-- UTH Connect — Ghép trọ: lưu trạng thái chủ tin TẮT liên hệ Zalo
-- Migration: 20261113010000_housing_zalo_opt_out.sql
-- ----------------------------------------------------------------------------
-- Review GitHub 13/11: công tắc "cho phép Zalo" trên form đăng tin trước đây
-- chỉ lưu contact_zalo_url rỗng khi tắt — màn detail không phân biệt được
-- "chủ động tắt" với "tin đời cũ chưa nhập", vẫn fallback SĐT và hiện nút
-- Nhắn Zalo trái ý chủ tin. Cột mới ghi rõ lựa chọn tắt; detail ẩn nút và
-- không fallback khi false.
--   * Tin đời cũ (kể cả tin đã tắt bằng bản app trước) nhận default true —
--     không thể phân biệt hồi tố, giữ hành vi fallback cũ cho tới khi chủ sửa.
--   * App mới đọc thiếu cột vẫn coi là true (chạy được trước migration),
--     nhưng GHI tin cần cột này — chạy migration trước khi đăng tin bằng APK mới.
-- Idempotent: add column if not exists, chạy lại được.
-- ============================================================================

begin;

alter table public.housing_posts
    add column if not exists contact_zalo_enabled boolean not null default true;

comment on column public.housing_posts.contact_zalo_enabled is
    'false = chủ tin tắt liên hệ Zalo (ẩn nút, không fallback SĐT); tin đời cũ default true.';

commit;