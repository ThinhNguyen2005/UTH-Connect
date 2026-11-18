# ERP-24 — Hướng dẫn thực thi và phiếu review

Trạng thái: **chưa thực thi; chưa có phê duyệt TV1**. Tài liệu này cụ thể hóa năm dòng trong `erp-24-scenarios.csv`; các hành vi và quyền là giả thuyết cần xác nhận, không khẳng định Odoo hiện hỗ trợ. Không dùng dữ liệu khách thật hoặc giao dịch tiền thật.

## Chuẩn bị

1. Có môi trường Odoo/POS thử nghiệm riêng, tài khoản thu ngân và quản lý theo quyền được TV1 xác nhận, ca thử và sản phẩm A/B với giá trong CSV. Ghi phiên bản, cấu hình và dữ liệu thử. Mỗi kịch bản dùng ca riêng hoặc ghi rõ các giao dịch khác để đối soát độc lập.
2. Kiểm tra có phương thức thanh toán thử, cách mô phỏng phản hồi chưa rõ, in lỗi và hoàn tiền. Nếu chưa có chức năng hoặc không thể mô phỏng an toàn, ghi **Blocked** và lý do; không coi là Pass.
3. Ghi ngày, người chạy, ID ca/đơn thử, kết quả thực tế và đường dẫn bằng chứng đã che dữ liệu nhạy cảm. So sánh từng kỳ vọng với thực tế; sai hoặc thiếu hành vi là Fail hoặc Blocked tùy nguyên nhân.

| ID | Điểm kiểm tra sau khi làm các bước trong CSV | Bằng chứng cần lưu | Kết quả thực tế |
| --- | --- | --- | --- |
| POS-01 | Tổng đơn 40.000; thu 50.000, trả 10.000; một đơn đã trả; tiền mặt dự kiến 140.000 với ca riêng. | Giỏ, xác nhận thanh toán/hóa đơn, báo cáo ca và mã đơn thử. | Chưa chạy |
| POS-02 | Giỏ sau sửa 20.000; hủy trước thu; không có khoản thu/doanh thu từ giỏ đó. Lưu vết hủy chỉ kiểm nếu quy định đã được duyệt. | Giỏ trước hủy và báo cáo/nhật ký chứng minh không phát sinh thu. | Chưa chạy |
| POS-03 | Khi chưa rõ không hoàn tất; nếu nguồn thanh toán xác nhận thành công thì không thu lại; nếu xác nhận thất bại và chưa bị thu thì mới thử lại. Sau cùng tối đa một khoản thu thành công cho đơn. | Trạng thái chờ, kết quả xác minh giả lập, tham chiếu và danh sách giao dịch. | Chưa chạy |
| POS-04 | Đơn gốc 40.000; hoàn một món 20.000; liên kết đơn gốc, thuần 20.000; thử yêu cầu vượt số còn có thể hoàn và ghi kết quả. | Đơn gốc, phê duyệt thử, chứng từ hoàn và báo cáo. | Chưa chạy |
| POS-05 | Lỗi in không làm thu lại; có bằng chứng tìm lại đơn. Cuối ca dự kiến 140.000, đếm 139.000, chênh lệch −1.000; ghi nhận và chuyển quản lý. | Đơn đã trả, bằng chứng xử lý lỗi in, báo cáo ca và xác nhận quản lý. | Chưa chạy |

Không ghi Pass khi chỉ đọc tài liệu. Nếu Odoo có hành vi khác với kỳ vọng dự thảo, ghi lại thực tế và đưa TV1 quyết định thay vì tự thay đổi kết quả hoặc quy tắc.

## Phiếu TV1 review

| Nội dung | Quyết định | Người duyệt / ngày / liên kết |
| --- | --- | --- |
| Sơ đồ, gồm xử lý giao dịch thanh toán chưa rõ | Chờ review | Chưa có |
| Vai trò thu ngân, quản lý ca, khách hàng | Chờ review | Chưa có |
| POS-01 đến POS-05 và dữ liệu thử | Chờ review | Chưa có |
| Điều kiện hủy, hoàn, quyền và xử lý chênh lệch | Chờ review | Chưa có |
| Đối chiếu SRS ERP-05 và phạm vi Odoo thực tế | Chờ ERP-05 / review | Chưa có |

Trước Done: TV1 ghi quyết định và link review; có PR/commit truy cập được; từng kịch bản có kết quả thực tế và bằng chứng; lỗi được xử lý hoặc có quyết định chấp nhận của người có thẩm quyền; liên kết tất cả vào KAN-146.
