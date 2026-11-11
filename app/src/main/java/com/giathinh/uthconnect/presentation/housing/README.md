# Ghép trọ (Housing)

Phụ trách: Đức Dịch Viết — nhánh `feature/retal-place`. Spec: `README.md` mục 3.2.

## Luồng đã có (slice 1, dữ liệu mock)

Tiện ích → Ghép trọ → feed tin (lọc theo loại tin / cơ sở / giá / khoảng cách / giới tính,
sort mới nhất–gần cơ sở–giá thấp) → báo vi phạm (mock ẩn tin sau 3 báo cáo).
Badge xác thực hiện trên mọi card; tin `FULFILLED` / hết chỗ / bị ẩn không lên feed.

## Chưa làm

- Đăng/sửa tin + LocationPicker (Maps chốt mức "picker khi đăng + deep-link khi xem",
  không embed map trong app; chưa thêm dependency).
- Màn chi tiết + liên hệ trong app qua contact request (không lộ SĐT).
- "Phòng của tôi" (chủ tin đánh dấu hết phòng), migration Supabase + RLS, Admin moderation.

## Test

`test/presentation/housing/` (ViewModel + UiState), `test/domain/model/CampusReferenceTest.kt`,
`test/data/repository/InMemoryHousingRepositoryTest.kt`.
