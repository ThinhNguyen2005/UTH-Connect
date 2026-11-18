# BÁO CÁO CÔNG VIỆC NGÀY 17/11/2026

Dự án: UTH Canteen ERP
Task: KAN-122 — UI, Security & QA
Branch: feat/kan-122-ui-security-qa
Ngày dự án D1–D15: chưa xác định.
Trạng thái: NOT DONE — bàn giao phần khả thi, còn blocker.

## 1. Nội dung thực hiện

Fetch origin; kiểm tra Phase 0, source Kan120@b99ecdf6 và dependency. Tạo worktree KAN-122 từ origin/develop@ed7cecbb; giữ nguyên thay đổi cũ và không đưa backend/commit Kan120 vào branch.

Sau xác nhận OK của người dùng, triển khai List/Form/Search và action product.template trong meal_views.xml. Có tên, mã món, nhóm POS, giá kèm tiền tệ, trạng thái; tìm kiếm và lọc đang hoạt động/đã lưu trữ; nhóm theo danh mục sản phẩm. Giữ nguyên quyền POS; chưa áp dụng quyền CRUD mới.

Tạo hai patch: sửa thứ tự nạp menu sau action và nối menu món ăn hiện có vào view/action mới. Đã apply thật trên snapshot riêng; không sửa nhánh Kan120 hoặc backend models.

Bổ sung validator tĩnh, 11 test QA, thiết kế/ma trận bảo mật, test cases, UAT checklist, demo/screenshot plan, AC checklist và PR draft. Không tạo Python model mới.

File code: views/meal_views.xml; patches/KAN-122-menu-load-order.patch; patches/KAN-122-meal-views.patch; qa/validate_module.py; qa/test_validate_module.py; qa/test_meal_views.py; qa/README.md, đều dưới canteen-erp/. Tài liệu và logs tại docs/reports/KAN-122/.

## 2. Kết quả

| Kiểm tra | Kết quả | Bằng chứng |
| --- | --- | --- |
| QA unit tests | PASS: 11 tests | checker-tests.txt |
| Backend simulation | PASS: 5 tests | simulation-tests.txt |
| Trước sửa load order | FAIL: 2 forward refs | static-before.txt |
| Sau hai patch | PASS: 0 static defects | static-final.txt |
| Patch apply/check | PASS trên snapshot riêng | patch-*.txt, meal-patch-*.txt |
| Odoo 20 fields/schema | PASS: fields và List/Search RelaxNG | odoo20-source-check.txt |
| Odoo functional/security/integration/regression | BLOCKED | KAN-122-test-results.md |
| UAT/PR/reviewer | PENDING APPROVAL | Checklist và PR draft |

Log nằm trong evidence/. Form đã parse và đối chiếu fields, chưa kiểm thử runtime. Static/simulation PASS không chứng minh Odoo cài được hoặc quyền đúng nghiệp vụ. AC13 có báo cáo Word; các AC runtime, ảnh/demo và review chưa đạt. Xem KAN-122-acceptance-checklist.md.

Chưa stage/commit/push/tạo PR; không tự merge. Chỉ thêm source thuộc KAN-122 và tài liệu, không sửa Android/KAN-108.

## 3. Tồn đọng

Backend KAN-120 chưa có trên develop; patch mới áp dụng trong snapshot tạm. Cần tích hợp dependency qua review rồi chuyển patch thành diff manifest/menu thực tế trên KAN-122.

Chưa có Odoo runtime/DB test; Docker engine không kết nối. Không tự cài phần mềm hoặc thay đổi môi trường chung. Source Odoo 20.0 tải để tham khảo không phải runtime.

Model đã chốt product.template; quyền CRUD cụ thể và phạm vi công ty/cơ sở chưa được chỉ định. Giữ nguyên ACL hiện tại, không dùng ẩn menu/domain để thay bảo mật. Danh sách available_in_pos có thể gồm sản phẩm POS ngoài căn-tin.

Rủi ro SQL/report và quyền ghi SQL view cần backend/security review. Chưa khẳng định hết lỗi nghiêm trọng. UAT, demo/video và screenshot chưa thực hiện.

## 4. Hình ảnh minh chứng

Đã chụp 1 ảnh trang báo cáo QA trong Chrome headless: `evidence/20-static-qa-report.png`. Dữ liệu lấy từ unittest vừa chạy: 11 tests PASS. Đã mở ảnh kiểm tra và chèn vào Word.

Hình 1 — Báo cáo QA tĩnh KAN-122 (QA01). Đây không phải giao diện Odoo hoặc kiểm thử quyền thực tế. Odoo runtime BLOCKED; UAT PENDING.

Nguồn: screenshot-qa-run.txt, screenshot-qa-run.json và qa-results.html trong evidence/. Các ảnh Odoo/UI/ACL/install/upgrade/UAT/demo vẫn chưa có.

## 5. Kế hoạch tiếp theo

1. Chốt quyền CRUD/phạm vi bản ghi cho nhóm POS.
2. Cung cấp môi trường Odoo/DB test được phép và tích hợp dependency KAN-120 qua review.
3. Áp dụng patch trên source đã tích hợp; chạy install/upgrade, functional/security/regression bằng user không phải admin.
4. Xử lý lỗi, thu thập screenshot thật, tổ chức UAT/demo và cập nhật AC.
5. Chờ phê duyệt trước stage/commit/push/PR theo bước 12.5.

Không có báo cáo giả cho D1–D15; đây là công việc thực tế ngày 17/11/2026, chưa phải nghiệm thu D15.
