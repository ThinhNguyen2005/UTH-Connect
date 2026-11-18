# KAN-122 — Báo cáo kỹ thuật

Ngày thực hiện: 17/11/2026. Ngày dự án D1–D15: chưa được xác nhận. Kết quả: **NOT DONE**; đã hoàn thành các phần độc lập khả thi.

## Nguồn Git và phạm vi

- Remote: https://github.com/ThinhNguyen2005/UTH-Connect.git; đã fetch lại trong phiên này.
- Branch: `feat/kan-122-ui-security-qa`; worktree `.worktrees/kan-122`.
- Tạo từ `origin/develop@ed7cecbb`; tại audit HEAD và develop có chênh lệch `0 0`.
- Backend: `origin/Kan120@b99ecdf63cf2d28c8947bad342d9c3bfafa08c7e`.
- Merge-base develop/Kan120: `a54f5b64`. Kan120 còn có các commit Android/web ngoài ERP; không merge/cherry-pick nhánh đó.
- Backend không nằm trong develop hoặc branch KAN-122. Chỉ xuất bản sao vào thư mục tạm để thử patch; không có commit backend trong nhánh KAN-122.
- Không sửa KAN-108/Housing/Android, không cài phần mềm hệ thống, không thay đổi production. Chưa stage/commit/push/tạo PR.

## Bước 1 — Inventory và dependency

| Hạng mục | Đã có trên Kan120 | Còn thiếu / cần xác minh | Phụ thuộc KAN-120 | Trạng thái |
| --- | --- | --- | --- | --- |
| XML Views | Form/search POS, báo cáo pivot/graph/tree, wizard | View đã viết; runtime còn BLOCKED | Có | BLOCKED runtime/nghiệp vụ |
| Menu | Root căn-tin, vận hành, món ăn, báo cáo | Group restrictions đã được duyệt | Có | Chưa kiểm thử runtime |
| Actions | Quầy, phiên, đơn, báo cáo, wizard | Thứ tự nạp hai action so với menu | Có | Sửa trên snapshot; static PASS |
| ACL | 3 dòng CSV cho report/wizard | Ma trận quyền nghiệp vụ; test user thật | Có | BLOCKED |
| Groups | Tham chiếu POS User/POS Manager | Xác nhận nhóm dùng cho KAN-122 | Odoo POS | BLOCKED |
| Tests | 5 test script mô phỏng | ORM/UI/security/install/upgrade | Có | Simulation PASS; runtime BLOCKED |

Manifest khai báo 20.0.1.0.0; điều này không chứng minh đang có runtime Odoo 20.0. Đã tải file product/POS và schema List/Search từ source Odoo 20.0 chính thức để kiểm tra tĩnh; chưa có runtime cài đặt đầy đủ. Không tìm thấy compose trong phần ERP. Python 3.14 có sẵn nhưng không import được Odoo; Docker CLI không kết nối được engine; không thấy psql trên PATH. Không tự cài hoặc khởi động môi trường dùng chung.

## Bước 2 — Worktree

Đã kiểm tra branch/path chưa tồn tại trước khi tạo từ develop. Các thay đổi chưa commit ở workspace gốc được giữ nguyên. Không dùng reset, force push hay merge. Bằng chứng: `evidence/git-audit.txt`.

## Bước 3 — Thiết kế theo source hiện có

Source dùng sản phẩm POS cho món ăn/đồ uống. Người dùng đã trả lời OK cho product.template và nhóm POS hiện có. Quyền CRUD cụ thể chưa được chỉ định; giữ nguyên ACL và không coi OK là phê duyệt quyền mới.

| Màn hình | Model hiện có | Field source đang dùng | Xử lý tiếp |
| --- | --- | --- | --- |
| Danh mục món ăn | product.template | name, default_code, list_price, available_in_pos, pos_categ_ids | Model đã xác nhận; fields đã đối chiếu source; CRUD/runtime còn chờ |
| Đơn căn-tin | pos.order | student_id_number, meal_type, canteen_location | Kiểm tra XPath và kế thừa trên runtime |
| Doanh thu | canteen.revenue.report | date, product_id, category_id, qty_sold, total_amount | Đối chiếu SQL view và quyền trước runtime |
| Bộ lọc | canteen.revenue.wizard | date_from, date_to, canteen_location, meal_type | Kiểm tra validation và hành vi report |

Menu hiện có:

```text
Căn-tin UTH ERP
  Vận hành Căn-tin → Quầy POS / Phiên bán / Đơn hàng
  Món ăn & Bảng giá → Danh sách món ăn, đồ uống / Nhóm món POS
  Báo cáo Doanh thu → Pivot, Graph / Wizard theo ngày, ca
```

Tái sử dụng menu có sẵn và nối vào action dành riêng cho các view mới. View priority 90 và action bindings rõ ràng giúp hạn chế ảnh hưởng lựa chọn view mặc định của các action khác; vẫn cần regression runtime. Giữ validation ở model. ACL, UAT, test cases và demo nằm trong tài liệu đi kèm.

## Bước 4 — XML Views

Đã thêm views/meal_views.xml: List/Form/Search và action product.template. List hiển thị tên, mã món, nhóm POS, giá kèm currency; form có thông tin món/giá/trạng thái. Search theo tên/mã/nhóm; filter đang hoạt động/đã lưu trữ; group theo categ_id. Action lọc available_in_pos=True, mặc định món đang hoạt động và default_available_in_pos=True khi tạo. Đây là phạm vi sản phẩm POS, chưa phải record rule cô lập căn-tin. Không thêm Python model hoặc quyền mới.

Đã kiểm tra fields với source Odoo 20.0 và validate RelaxNG List/Search. Form parse/fields PASS; runtime chưa chạy. Snapshot tích hợp có 12 XML. Existing views không bị sửa; menu được nối vào action mới bằng patch riêng. Nguồn và SHA256: evidence/odoo20-source-check.txt.

## Bước 5 — Menu và Actions

Hai findings Phase 0 vẫn là lỗi thứ tự tham chiếu trên source hiện tại:

- `views/menu_views.xml` dùng `action_canteen_revenue_report_pivot` trước `reports/pos_revenue_report_views.xml`.
- `views/menu_views.xml` dùng `action_canteen_revenue_wizard` trước `wizard/pos_revenue_wizard_views.xml`.

Patch thứ hai `KAN-122-meal-views.patch` nạp meal_views.xml trước menu và nối menu món ăn hiện có vào action mới, không tạo submenu trùng. Bản sửa load order `canteen-erp/patches/KAN-122-menu-load-order.patch`, chỉ di chuyển một dòng manifest để nạp menu cuối cùng. Đã apply thật trên snapshot tạm và kiểm tra lại. Trước sửa: exit 1, hai lỗi; sau sửa: exit 0, không còn lỗi trong phạm vi static validator. Patch check và reverse-check PASS. Không khẳng định module install PASS.

Do backend chưa có trên develop, patch chưa là thay đổi manifest đã triển khai trong branch. Sau khi dependency được reviewer tích hợp vào develop, chuyển patch thành diff manifest và chạy lại; không đưa toàn bộ backend vào PR.

## Bước 6 — Security

Đã đọc CSV/groups/model và lập ma trận quyền hiện tại, đề xuất cần duyệt. Chưa áp dụng quyền mới. Kết quả: BLOCKED nghiệp vụ/runtime. Xem `KAN-122-security-matrix.md`.

## Bước 7 — Review và tích hợp

- Static checker dùng thư viện chuẩn, đọc AST thay vì thực thi backend/eval.
- Có regression tests cho forward refs, generated model IDs, eval ref, duplicate IDs, malformed/missing XML, ACL sai định dạng/phạm vi và path traversal.
- Đã review patch: không thay models/ACL/dữ liệu, không có thay đổi Android.
- CSV đúng định dạng không đồng nghĩa quyền hợp lệ nghiệp vụ. Fields của view mới đã đối chiếu source Odoo 20.0; external IDs và registry/quyền thực tế chưa xác minh.
- Kiểm tra whitespace/scope và kết quả ở evidence; không có công cụ secret scanner chuyên dụng được chạy. Rà soát file mới không chứa credential/database dump.

## Bước 8 — Kiểm thử

11 QA tests PASS (8 validator, 3 UI contracts); 5 simulation tests backend PASS. Fields và schema List/Search Odoo 20.0 PASS. Before/after static check có log riêng. Functional/security/install/upgrade/regression Odoo BLOCKED. Bảng FT01–FT09, ST01–ST10, IT01–IT07 nằm trong `KAN-122-test-results.md`.

## Bước 9 — UAT và demo

Đã chuẩn bị checklist và demo script. UAT PENDING APPROVAL, chưa diễn ra. Demo/video chưa thực hiện, VIDEO PENDING.

## Bước 10 — Screenshot

Đã chụp trang báo cáo QA thật trong Chrome headless, dựa trên 11 tests vừa thực thi, và mở PNG kiểm tra nội dung. File evidence/20-static-qa-report.png được chèn vào Word. Ảnh ghi rõ STATIC ONLY/Odoo BLOCKED, không giả giao diện Odoo hoặc terminal. Đã tìm được cách chụp trang báo cáo bằng browser headless; phiên desktop vẫn không có cửa sổ để chụp trực tiếp.

Các screenshot Odoo, ACL, install/upgrade, UAT và demo chưa có. Kế hoạch 19 ảnh runtime giữ nguyên trạng thái chưa thực hiện.

## Bước 11 — Báo cáo

Báo cáo ngày thực tế 17/11/2026; không điền D1–D15 hoặc tên người thực hiện. Word A4, Times New Roman 13, tiêu đề 16, giãn dòng 1.3, số trang. Chèn 1 ảnh báo cáo QA tĩnh và chú thích rõ; chưa có ảnh Odoo. File Word được mở lại kiểm tra cấu trúc, chưa kiểm tra bố cục bằng Word GUI.

## Bước 12 — Bàn giao trước commit

Nhánh đúng develop, không có commit mới. Diff là patch UI/load order, static QA và tài liệu. Chuẩn bị nội dung PR trong `KAN-122-PR-draft.md`, không tạo PR thật. Commit/push/PR PENDING APPROVAL theo yêu cầu bước 12.5; merge không được thực hiện.

## Bug và blocker ledger

| ID | Severity | Reproduce / Expected / Actual | Fix status / Retest |
| --- | --- | --- | --- |
| BUG-01 | High | Chạy validator nguồn b99ecdf6; expected action trước menu; actual 2 forward refs | Patch áp dụng snapshot; static retest PASS; runtime BLOCKED |
| DEP-01 | Blocker | develop thiếu uth_canteen_pos | Chờ tích hợp backend qua review; không tự merge |
| ENV-01 | Blocker | Odoo chưa có; Docker engine không kết nối | Runtime tests BLOCKED; cần môi trường test được phép |
| BIZ-01 | Blocker | Đã xác nhận product.template; chưa có ma trận CRUD cụ thể | Giữ nguyên quyền POS; chờ chốt CRUD |
| RISK-01 | Cần kiểm chứng | ACL manager ghi SQL report; báo cáo không có company_id; SQL join thanh toán và pos_categ_ids cần đối chiếu schema | Chuyển cho backend/security review; chưa tái hiện runtime, chưa sửa backend |

Không tuyên bố hệ thống hết lỗi nghiêm trọng. Known issues còn chặn nghiệm thu và production.
