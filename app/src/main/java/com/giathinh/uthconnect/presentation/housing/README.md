# Ghép trọ (Housing)

Phụ trách: Đức Dịch Viết — nhánh `feature/housing-duc`. Spec: `README.md` mục 3.2.

## Luồng đã có (dữ liệu thật trên Supabase)

Tiện ích → Ghép trọ → feed tin `housing_posts` (lọc theo loại tin / cơ sở / giá /
khoảng cách / giới tính, sort mới nhất–gần cơ sở–giá thấp). Khoảng cách tới cơ sở
là khoảng cách ĐƯỜNG ĐI thật qua OSRM (fallback đường thẳng khi OSRM lỗi).
Badge "Sinh viên UTH đã xác thực" hiện trên card của chủ tin đã xác thực MSSV;
tin `FULFILLED` / hết chỗ / bị ẩn không lên feed.

- **Chi tiết + liên hệ công khai**: sinh viên đã đăng nhập xem được địa chỉ đầy đủ,
  tọa độ chính xác, SĐT và Zalo của chủ tin (cổng duyệt contact-request đã bỏ theo
  quyết định sản phẩm 11/11). Nút Gọi điện / Nhắn Zalo / Chỉ đường mở app ngoài
  bằng deep-link; link Zalo được kiểm tra theo quy tắc miền trước khi mở. Chủ tin
  tắt Zalo (`contact_zalo_enabled = false`) thì ẩn nút Nhắn Zalo, không fallback SĐT.
- **Đăng tin**: form đủ trường (3 loại tin, giá, cọc, diện tích, tiện nghi, giới
  tính, tối đa 6 ảnh upload song song lên bucket `housing-images`), picker vị trí
  OpenStreetMap (OSMDroid, không API key) chấm pin + GPS + geocode địa chỉ, nháp
  cục bộ theo tài khoản (DataStore), validation trước khi gửi.
- **Quản lý tin của chủ**: sửa tin, đánh dấu hết phòng, xóa tin (ảnh cần dọn ghi
  hàng đợi DataStore rồi trả kết quả ngay, worker nền dọn — không chặn thao tác
  lẫn feed, chỉ dọn ảnh đúng tài khoản; hàng đợi lỗi đĩa thì dọn đồng bộ ngay);
  giới hạn 2 tin OPEN mỗi người và ngưỡng 3 báo cáo ẩn tin enforced
  bằng RLS + trigger + RPC trong `supabase/migrations`.
- **Tương tác feed**: lưu tin yêu thích (cục bộ), báo vi phạm kèm lý do.

## Chưa làm

- Admin moderation UI (tin bị ẩn hiện chỉ xử lý ở backend).
- Tìm kiếm toàn văn / gợi ý theo khu vực ngoài bộ lọc hiện có.

## Test

`test/presentation/housing/` (ViewModel + UiState cho feed, chi tiết, đăng tin,
picker), `test/domain/model/CampusReferenceTest.kt`,
`androidTest/presentation/housing/HousingScreenUiTest.kt`.