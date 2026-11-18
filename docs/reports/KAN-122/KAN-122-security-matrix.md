# KAN-122 — Ma trận bảo mật

Ngày kiểm tra: 17/11/2026. Nguồn: `origin/Kan120@b99ecdf6`.

## Quyền khai báo hiện tại — chưa kiểm chứng hiệu lực trên runtime

| Role / XML ID | Model | Read | Create | Write | Unlink | Record Rule |
| --- | --- | --- | --- | --- | --- | --- |
| `point_of_sale.group_pos_user` | `canteen.revenue.report` | 1 | 0 | 0 | 0 | Không có rule riêng trong module |
| `point_of_sale.group_pos_manager` | `canteen.revenue.report` | 1 | 1 | 1 | 1 | Không có rule riêng trong module |
| `point_of_sale.group_pos_user` | `canteen.revenue.wizard` | 1 | 1 | 1 | 1 | Hành vi TransientModel cần test trên runtime |
| POS roles | `product.template`, `product.product`, `pos.order`, `pos.config`, `pos.session` | Chưa xác minh | Chưa xác minh | Chưa xác minh | Chưa xác minh | Phụ thuộc Odoo/POS |

ACL có tính cộng gộp; phải kiểm tra các ACL khác và nhóm được kế thừa trước khi kết luận quyền hiệu lực. Không thấy định nghĩa nhóm riêng hoặc record rules riêng trong manifest KAN-120. Các menu hiện có không ghi group restriction; khả năng hiển thị thực tế còn phụ thuộc action/model access.

## Quyết định đang chờ — PROPOSED, chưa áp dụng

1. Đã nhận OK cho product.template và nhóm POS hiện có; view dùng model này. Chưa có xác nhận CRUD cụ thể.
2. Xác nhận POS User chỉ xem hay được tạo/sửa; POS Manager có được xóa món ăn hay cần lưu trữ lịch sử bằng archive.
3. Đề xuất báo cáo SQL tổng hợp chỉ Read cho cả hai nhóm. Chưa thay ACL hiện có khi chưa có xác nhận.
4. Xác nhận phạm vi dữ liệu theo công ty/cơ sở và quyền xem báo cáo. Model doanh thu hiện chưa có company_id; không tạo domain giả để che thiếu backend.
5. Xác nhận có cần nhóm Canteen riêng hay tái sử dụng nhóm POS. Không tạo nhóm mang quyền rộng hoặc implied groups chưa được duyệt.

Giữ nguyên security XML/CSV/rules và quyền POS hiện có. Menu/action không cấp thêm quyền model. Đây là BLOCKED ở bước 6, không phải đã hoàn thành security.

## Kiểm thử bắt buộc khi có môi trường

- Tạo user test POS User, POS Manager, internal user không thuộc nhóm POS và user ở công ty/cơ sở khác nếu nghiệp vụ áp dụng; không dùng dữ liệu production.
- Kiểm tra CRUD qua ORM/RPC và menu/action riêng biệt; menu ẩn không thay ACL.
- Đổi nhóm, mở phiên mới và thử lại; kiểm tra cả nhóm trực tiếp lẫn inherited groups.
- Thử đọc/sửa/xóa bản ghi ngoài phạm vi bằng ID trực tiếp; xác nhận Search và read không rò dữ liệu.
- Chụp cấu hình nhóm/ACL/rules và kết quả user được phép/bị từ chối. Không ghi mật khẩu trong ảnh hoặc log.

Expected của các phép CRUD chỉ được chốt sau khi ma trận nghiệp vụ được duyệt. Không gán PASS từ kiểm tra CSV.
