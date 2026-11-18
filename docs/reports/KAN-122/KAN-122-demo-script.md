# KAN-122 — Demo và kế hoạch ảnh

VIDEO PENDING. Chưa demo/chụp giao diện Odoo. Đã có ảnh bổ sung 20-static-qa-report.png: chụp trang báo cáo QA trong browser từ kết quả test vừa chạy, không phải screenshot Odoo hoặc terminal. 19 ảnh bên dưới vẫn chưa thực hiện.

## Luồng demo

1. Xác nhận DB test/version/SHA; đóng cửa sổ có thông tin nhạy cảm.
2. Đăng nhập user POS test; mở menu Canteen và danh sách suất ăn.
3. Mở form; trình bày fields/giá/trạng thái; tạo/sửa fixture theo quyền.
4. Tìm tên/mã/nhóm; bỏ filter hoạt động rồi chọn lưu trữ; group danh mục.
5. Đăng nhập user không có quyền; thử cả menu và ORM/URL trực tiếp.
6. Tái hiện validation; sửa dữ liệu rồi lưu lại.
7. Hiển thị log functional/security/integration/regression thực tế.
8. Tổng kết AC và lỗi còn lại, không đọc PASS cho test chưa chạy.

## Chụp/quay thủ công

Dùng Snipping Tool (Win+Shift+S), chọn riêng cửa sổ ứng dụng/terminal thật, lưu vào evidence/. Terminal phải có lệnh vừa chạy và kết quả/exit code. Ghi test ID, SHA, thời điểm và caption. Không chụp mật khẩu/token/production. Không render log thành screenshot.

Nếu máy có OBS được phép dùng, chọn Window Capture và quay đúng luồng demo; hoặc dùng công cụ quay sẵn có được phép. Không tự cài phần mềm. Kiểm tra video trước khi chia sẻ.

| File dự kiến | Mapping | Nội dung | Trạng thái |
| --- | --- | --- | --- |
| 01-git-branch.png | Bước 2 | Branch/worktree | NOT RUN; chưa có file |
| 02-odoo-module.png | Bước 1 | Module tích hợp | NOT RUN; chưa có file |
| 03-list-view.png | FT03 | Danh sách | NOT RUN; chưa có file |
| 04-form-view.png | FT04 | Form | NOT RUN; chưa có file |
| 05-search-filter.png | FT07 | Search/filter | NOT RUN; chưa có file |
| 06-canteen-menu.png | FT01–02 | Menu | NOT RUN; chưa có file |
| 07-user-groups.png | ST06 | Groups | NOT RUN; chưa có file |
| 08-acl.png | IT04 | ACL | NOT RUN; chưa có file |
| 09-record-rules.png | ST07 | Rules nếu áp dụng | NOT RUN; chưa có file |
| 10-authorized-user.png | ST01 | User có quyền | NOT RUN; chưa có file |
| 11-unauthorized-user.png | ST02/ST08 | User bị từ chối | NOT RUN; chưa có file |
| 12-module-install.png | IT01 | Cài mới | NOT RUN; chưa có file |
| 13-module-upgrade.png | IT02 | Upgrade | NOT RUN; chưa có file |
| 14-functional-tests.png | FT01–09 | Functional thật | NOT RUN; chưa có file |
| 15-security-tests.png | ST01–10 | Security thật | NOT RUN; chưa có file |
| 16-regression-tests.png | RT01 | Regression thật | NOT RUN; chưa có file |
| 17-uat.png | UAT01–05 | Buổi UAT | NOT RUN; chưa có file |
| 18-demo.png | Demo | Demo thật | NOT RUN; chưa có file |
| 19-pr-ci.png | AC14–15 | PR/CI sau phê duyệt | NOT RUN; chưa có file |

Ảnh rules chỉ nếu áp dụng; UAT/PR sau khi hoạt động diễn ra thật. Không tạo file ảnh rỗng.
