-- SUPERSEDED — không chạy lại file này.
-- Bản gốc tạo housing_posts trùng tên nhưng lệch schema với phần housing trong
-- 20261107000000_auth_rbac_and_housing.sql; `create table if not exists` khiến
-- bản chạy sau bị bỏ qua âm thầm và staging giữ schema thiếu cột.
-- Schema Ghép trọ chính thức (khớp domain HousingListing, RLS, giới hạn 2 tin
-- mở, RPC báo cáo, bucket ảnh) nằm ở 20261108000000_housing_schema_rebuild.sql.
-- File này giữ lại chỉ để lịch sử migration không bị đứt gãy.
select 1;