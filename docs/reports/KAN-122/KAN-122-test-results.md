# KAN-122 — Kết quả kiểm thử 17/11/2026

Nguồn backend: b99ecdf63cf2d28c8947bad342d9c3bfafa08c7e. Static/simulation không phải runtime.

| Test ID | Scenario | Expected | Actual | Result | Evidence |
| --- | --- | --- | --- | --- | --- |
| QA01 | QA tests | Không lỗi | 11 tests OK | PASS | evidence/checker-tests.txt |
| QA02 | Simulation | Không lỗi | 5 tests OK | PASS | evidence/simulation-tests.txt |
| QA03 | Before fix | Action trước menu | 2 forward refs | FAIL | evidence/static-before.txt |
| QA04 | After fix | Không lỗi tĩnh | 0 defects | PASS | evidence/static-final.txt |
| QA05 | Patch check/apply/reverse | Áp dụng đúng | Exit 0 | PASS | evidence/patch-*.txt; evidence/meal-patch-*.txt |
| QA06 | Odoo 20 source/schema | Field tồn tại; list/search đúng schema | Field AST và RelaxNG PASS | PASS | evidence/odoo20-source-check.txt |
| FT01 | Mở menu Canteen | Menu mở theo quyền | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT02 | Quản lý suất ăn | Mở action product.template mới | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT03 | List View | Tên/mã/nhóm/giá hiển thị đúng | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT04 | Form View | Fields/giá/currency đúng; lưu theo quyền | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT05 | Create | Theo CRUD được duyệt | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT06 | Write | Theo CRUD được duyệt | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT07 | Search/Filter | Khớp fixture; group categ_id đúng | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT08 | Validation | Server từ chối; không lưu một phần | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| FT09 | Thông báo lỗi | Thông báo rõ, không rò thông tin | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST01 | Authorized Read | Đọc trong phạm vi | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST02 | Unauthorized Read | Server từ chối | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST03 | Create | Đúng ma trận đã duyệt | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST04 | Write | Đúng ma trận đã duyệt | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST05 | Unlink | Đúng ma trận; dữ liệu không được phép còn nguyên | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST06 | Group Inheritance | Không phát sinh quyền vượt ma trận | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST07 | Record Rules | Đúng phạm vi đã duyệt hoặc xác nhận không áp dụng | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST08 | Direct Access | Server vẫn kiểm soát quyền | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST09 | Menu Visibility | Đúng nhóm, không coi menu là ACL | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| ST10 | Đổi group | Quyền cập nhật đúng | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT01 | Install | Không lỗi XML/SQL/ACL | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT02 | Upgrade | Dữ liệu giữ nguyên; module hoạt động | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT03 | XML references | ID/XPath hợp lệ | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT04 | ACL loading | Đúng ma trận | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT05 | Menu actions | Đúng action; không menu chết | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT06 | Backend compatibility | Tương thích schema/model thực tế | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| IT07 | Logs | Không traceback ngoài dự kiến | Chưa chạy: thiếu runtime/DB; quyền chờ duyệt | BLOCKED | evidence/environment.txt |
| RT01 | Regression POS | Luồng cũ không hồi quy | Chưa chạy Odoo | BLOCKED | evidence/environment.txt |

## Bước thực hiện khi có môi trường

Chỉ dùng DB test và fixture riêng; chốt version/SHA/ma trận CRUD trước khi chạy. Không dùng admin làm bằng chứng duy nhất.
- FT01: Đăng nhập POS test; mở Canteen. Expected: Menu mở theo quyền.
- FT02: Mở menu món ăn hiện có. Expected: Mở action product.template mới.
- FT03: Mở danh sách fixture. Expected: Tên/mã/nhóm/giá hiển thị đúng.
- FT04: Mở fixture từ list. Expected: Fields/giá/currency đúng; lưu theo quyền.
- FT05: Tạo fixture bằng từng role. Expected: Theo CRUD được duyệt.
- FT06: Sửa fixture rồi tải lại. Expected: Theo CRUD được duyệt.
- FT07: Tìm tên/mã/nhóm; bỏ filter hoạt động trước khi bật lưu trữ. Expected: Khớp fixture; group categ_id đúng.
- FT08: Bỏ tên required rồi lưu. Expected: Server từ chối; không lưu một phần.
- FT09: Thử dữ liệu sai/quyền thiếu. Expected: Thông báo rõ, không rò thông tin.
- ST01: Đọc fixture bằng role hợp lệ. Expected: Đọc trong phạm vi.
- ST02: Đọc cùng ID bằng role không hợp lệ. Expected: Server từ chối.
- ST03: Gọi create bằng từng nhóm. Expected: Đúng ma trận đã duyệt.
- ST04: Gọi write bằng từng nhóm. Expected: Đúng ma trận đã duyệt.
- ST05: Xóa fixture riêng bằng từng nhóm. Expected: Đúng ma trận; dữ liệu không được phép còn nguyên.
- ST06: Kiểm tra nhóm trực tiếp/kế thừa. Expected: Không phát sinh quyền vượt ma trận.
- ST07: Thử bản ghi ngoài công ty/cơ sở. Expected: Đúng phạm vi đã duyệt hoặc xác nhận không áp dụng.
- ST08: Truy cập ORM/RPC/URL dù menu ẩn. Expected: Server vẫn kiểm soát quyền.
- ST09: Mở menu bằng từng role. Expected: Đúng nhóm, không coi menu là ACL.
- ST10: Thu hồi/cấp nhóm; mở phiên mới; thử CRUD. Expected: Quyền cập nhật đúng.
- IT01: Cài dependency/module vào DB test sạch. Expected: Không lỗi XML/SQL/ACL.
- IT02: Upgrade DB có fixture, so dữ liệu. Expected: Dữ liệu giữ nguyên; module hoạt động.
- IT03: Resolve IDs và view trên Odoo. Expected: ID/XPath hợp lệ.
- IT04: Kiểm tra ACL sau cài bằng user thường. Expected: Đúng ma trận.
- IT05: Click từng menu. Expected: Đúng action; không menu chết.
- IT06: Chạy luồng POS và report. Expected: Tương thích schema/model thực tế.
- IT07: Đọc log sau test. Expected: Không traceback ngoài dự kiến.

RT01: mở phiên → bán fixture → thanh toán → đóng phiên → đối chiếu báo cáo; kiểm tra cả quầy ngoài căn-tin.
QA03 giữ FAIL trước sửa, không thay bằng PASS sau sửa. Form chỉ parse/field check; runtime chưa chạy.
