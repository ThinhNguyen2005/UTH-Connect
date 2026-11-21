# KAN-146 — Đối chiếu SRS ERP-05

Ngày đối chiếu: 21/11/2026. Nguồn: [SRS tại commit 8175fad](https://github.com/ThinhNguyen2005/uth-canteen-erp/blob/8175fad3e9b28e42c2bdccd9c15a8e8f5f8931b2/docs/srs.md), [PR ERP-05 đã merge](https://github.com/ThinhNguyen2005/uth-canteen-erp/pull/3) và [KAN-127](https://nguyenthinhk52005.atlassian.net/browse/KAN-127), hiện Done. Kết quả này chỉ là đối chiếu tài liệu; không chứng minh hệ thống đã chạy hay TV1 đã duyệt KAN-146.

| Nội dung | SRS | Đối chiếu dự thảo ERP-24 |
| --- | --- | --- |
| Bán tại quầy | UC03: ca mở; chọn món, hình thức ăn tại bàn/mang đi, ghi chú; thu tiền mặt hoặc chuyển khoản; xác nhận và in hóa đơn. | Sơ đồ và POS-01 bao phủ chọn món, tiền mặt, xác nhận và hóa đơn. Cần TV1 duyệt bổ sung lựa chọn hình thức phục vụ, ghi chú và luồng gửi bếp vào phạm vi khảo sát. |
| Hủy trước thu | UC03 ngoại lệ: hủy đơn nháp, giải phóng bàn. | POS-02 bao phủ hủy trước thu; cần xác nhận cách lưu vết và kiểm tra giải phóng bàn nếu dùng bàn. |
| Thanh toán điện tử chưa rõ | UC03 nêu VietQR tĩnh; UC09 yêu cầu thu ngân xác nhận đủ tiền trước Validate. | POS-03 đề xuất đối chiếu trước khi thử lại; SRS chưa nêu nguồn xác minh, cơ chế trạng thái chờ hoặc tích hợp tự động. Không coi các cơ chế này đã được triển khai. |
| Hoàn một phần | UC03 và UC09 không đặc tả chính sách hoàn tiền hoặc cấp duyệt. | POS-04 là đề xuất, cần TV1 xác nhận chính sách, quyền và giới hạn hoàn; chưa thể suy ra hỗ trợ thực tế từ SRS. |
| Đóng ca và chênh lệch | UC09: thu ngân kiểm đếm, nhập tiền thực tế; quản lý kiểm tra chênh lệch và xác nhận vào sổ. | Phù hợp hướng POS-05; phương thức xử lý lỗi in và chính sách chênh lệch cần xác nhận thêm. |

SRS mô tả cả luồng bán tại quầy UC03 và thanh toán đơn QR sau phục vụ UC09. Hai luồng có thời điểm thanh toán khác nhau; cần giữ riêng khi review, không giả định một luồng áp dụng cho tất cả đơn. Các kịch bản vẫn chưa được thực thi và chưa có phê duyệt TV1.
