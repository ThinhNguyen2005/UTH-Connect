# KAN-122 — kiểm tra tĩnh và bản sửa tích hợp

Backend đang ở `origin/Kan120`, chưa có trên develop. Thư mục này không chứa bản sao backend, module thay thế hoặc quyền mới.

## Chạy kiểm tra

```powershell
python -B -m unittest discover -s canteen-erp/qa -v
python -B canteen-erp/qa/validate_module.py <snapshot>/canteen-erp/custom_addons/uth_canteen_pos
```

Validator chỉ đọc file: parse Python/XML, kiểm tra file manifest, ID cục bộ, thứ tự ref/action/parent/ref() trong eval và định dạng ACL. Không chạy eval hoặc import code backend.

Giới hạn: không xác minh external IDs của dependency, fields/XPath/schema Odoo, quyền hiệu lực, record rules, view hoạt động, hoặc các tham chiếu nằm trong chuỗi domain/context/QWeb. Quy tắc ID trùng dành cho snapshot này; module cố ý cập nhật cùng một ID nhiều lần cần reviewer đánh giá thay vì tự xóa record.

## Thử patch trong môi trường tích hợp riêng

Nguồn đã kiểm tra: `origin/Kan120` tại `b99ecdf63cf2d28c8947bad342d9c3bfafa08c7e`.

Xuất snapshot bằng `git archive` vào thư mục tạm riêng, không checkout đè workspace. Từ thư mục snapshot chứa `canteen-erp/`:

```powershell
git apply --check <KAN-122-worktree>/canteen-erp/patches/KAN-122-menu-load-order.patch
git apply <KAN-122-worktree>/canteen-erp/patches/KAN-122-menu-load-order.patch
```

Chạy validator trước và sau patch. Trước: hai local action chưa được định nghĩa. Sau: không còn lỗi trong phạm vi kiểm tra tĩnh. `git apply --reverse --check` kiểm tra patch đã được áp dụng đúng; không tự chạy apply lần hai.

Patch chỉ chuyển `views/menu_views.xml` sau các report/wizard action trong manifest. Không sửa model, ACL hoặc nhánh Kan120. Khi backend được tích hợp qua review vào develop, chuyển patch thành thay đổi manifest thực tế trên nhánh KAN-122 và chạy lại kiểm tra. Nếu upstream đã sửa, bỏ patch tương ứng sau khi xác minh; không áp dụng mù.

Chưa có Odoo runtime. Không dùng kết quả này thay cho install/upgrade, functional, security hoặc UAT. Mã nguồn module không nằm trong PR KAN-122; dependency vẫn BLOCKED.

## Views sau xác nhận model

File views/meal_views.xml mới chứa List/Form/Search và action product.template. Copy vào đúng đường dẫn của snapshot, áp dụng KAN-122-meal-views.patch sau patch load order. Patch thứ hai nối menu sẵn có, không tạo menu trùng. Đây là phần mở rộng module backend, không phải addon độc lập cài được từ branch KAN-122.

Giữ nguyên ACL POS. Domain available_in_pos chỉ lọc danh sách, không là rule bảo mật hoặc cô lập riêng căn-tin. Muốn xem món đã lưu trữ, bỏ filter đang hoạt động rồi bật lưu trữ. Bỏ available_in_pos trên form sẽ làm món rời danh sách theo domain.

Nguồn chính thức dùng kiểm tra tĩnh: [Odoo 20 product.template](https://raw.githubusercontent.com/odoo/odoo/20.0/addons/product/models/product_template.py), [POS fields](https://raw.githubusercontent.com/odoo/odoo/20.0/addons/point_of_sale/models/product_template.py), [List schema](https://raw.githubusercontent.com/odoo/odoo/20.0/odoo/addons/base/rng/list_view.rng), [Search schema](https://raw.githubusercontent.com/odoo/odoo/20.0/odoo/addons/base/rng/search_view.rng). Chưa chạy server Odoo; xem SHA256 trong evidence/odoo20-source-check.txt.
