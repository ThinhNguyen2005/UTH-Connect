# KAN-122 — Checklist UAT

PENDING APPROVAL. Chưa tổ chức UAT; chưa có người nghiệm thu. Runtime BLOCKED.

| ID | Scenario | Steps | Expected | Actual | Evidence | Người nghiệm thu | Trạng thái |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UAT01 | Điều hướng/tìm món | Mở menu; tìm fixture; mở form | Dễ tìm, fields đúng | Chưa chạy | Chưa có | Chưa chỉ định | PENDING APPROVAL |
| UAT02 | Tạo/sửa | User được phép tạo/sửa fixture, user khác thử lại | Đúng ma trận CRUD | Chưa chạy | Chưa có | Chưa chỉ định | PENDING APPROVAL |
| UAT03 | Phân quyền | Thử URL/ORM trực tiếp và bản ghi ngoài phạm vi | Không vượt quyền | Chưa chạy | Chưa có | Chưa chỉ định | PENDING APPROVAL |
| UAT04 | Validation | Bỏ tên required rồi sửa lại | Từ chối sai, lưu đúng | Chưa chạy | Chưa có | Chưa chỉ định | PENDING APPROVAL |
| UAT05 | Regression | Bán fixture, đóng phiên, xem report | Tổng khớp | Chưa chạy | Chưa có | Chưa chỉ định | PENDING APPROVAL |

Khi chạy: ghi thời điểm/version/SHA/user role, actual, ảnh thật và người nghiệm thu. Không tự ký duyệt; giữ lỗi và lần retest.
