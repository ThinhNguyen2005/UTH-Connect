# BÁO CÁO SO SÁNH CHUYÊN SÂU: ODOO EDUCATION ENTERPRISE VS ODOO COMMUNITY 20.0
## HỆ THỐNG QUẢN LÝ CĂN-TIN SINH VIÊN TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI TP.HCM (UTH CANTEEN ERP)

**Mã task**: KAN-120  
**Tác giả**: Tạ Huy Thiên Văn  
**Dự án**: UTH Canteen ERP – Odoo Community 20.0  
**Ngày lập**: 17/11/2026  

---

## 1. TỔNG QUAN VÀ BỐI CẢNH DỰ ÁN

Hệ thống **UTH Canteen ERP** được xây dựng nhằm hiện đại hóa quy trình bán hàng, kiểm soát doanh thu, quản lý tồn kho nguyên liệu và hỗ trợ thanh toán không tiền mặt (VietQR, MoMo, Thẻ sinh viên Smart Card) tại hệ thống căn-tin các cơ sở của Trường Đại học Giao thông Vận tải TP.HCM.

Để đảm bảo hiệu quả đầu tư và tính khả thi trong giai đoạn **MVP 3 tuần (D1–D15)**, việc lựa chọn giữa hai phiên bản **Odoo Community 20.0 (Mã nguồn mở miễn phí)** và **Odoo Education Enterprise (Bản quyền dành cho giáo dục)** là bài toán chiến lược quan trọng.

---

## 2. BẢNG SO SÁNH TỔNG QUAN

| Tiêu chí | Odoo Community 20.0 | Odoo Education Enterprise | Đánh giá đối với Căn-tin UTH |
| :--- | :--- | :--- | :--- |
| **Bản quyền & Chi phí** | Mã nguồn mở hoàn toàn (LGPL-3), miễn phí 100%. | Miễn phí tài khoản Education cho sinh viên/giảng viên; có phí license khi triển khai thương mại thực tế. | Community tối ưu cho MVP và làm chủ mã nguồn; Enterprise cần phê duyệt gói bản quyền của Odoo S.A. |
| **Giao diện người dùng (UI/UX)** | Web Responsive cơ bản, chạy trên trình duyệt máy tính và tablet. | Giao diện chuẩn hóa cao cấp, giao diện Touchscreen di động siêu mượt, hỗ trợ Dark Mode. | Cả 2 đều đáp ứng tốt quầy thu ngân màn hình cảm ứng tại quầy căn-tin. |
| **Point of Sale (POS) nâng cao** | Quản lý phiên cơ bản, tiền mặt, phương thức thanh toán tùy biến, in bill trình duyệt. | Hỗ trợ IoT Box (kết nối trực tiếp máy in bill USB/LAN, cân điện tử, ngăn kéo đựng tiền không cần driver phụ). | Bản Community hoàn toàn đáp ứng được thông qua việc kết nối máy in nhiệt và web receipt. |
| **Chương trình Khuyến mãi & Thẻ SV (Loyalty & Rewards)** | Giới hạn; cần cài đặt thêm module ngoài (OCA / custom code). | Tích hợp sẵn hệ thống tích điểm, voucher giảm giá sinh viên, thẻ thành viên trả trước (Gift cards/eWallet). | Với MVP, module custom `uth_canteen_pos` đã giải quyết được việc định danh thẻ sinh viên qua MSSV. |
| **Tự gọi món (Self-Ordering / Kiosk)** | Không có sẵn trong core; cần tự phát triển web app riêng. | Có sẵn tính năng Kiosk tự phục vụ và QR Self-ordering cho sinh viên ngồi tại bàn quét mã đặt món. | Rất tiềm năng cho Phase 2 của dự án khi mở rộng sang Kiosk tự động tại Cơ sở 2. |
| **Báo cáo & Phân tích (Reporting & BI)** | Pivot View, Graph View (Bar, Pie, Line) và xuất Excel/PDF cơ bản. | Tích hợp **Odoo Spreadsheet** (Excel trực tiếp trong web), Dashboard thời gian thực, phân tích Cohort. | Community đáp ứng hoàn toàn nhu cầu báo cáo doanh thu theo ngày/ca/nhóm món thông qua SQL View. |
| **Ứng dụng Di động (Mobile App)** | Sử dụng trình duyệt mobile (Chrome/Safari) hoặc PWA. | Ứng dụng Native Odoo Mobile trên Google Play và Apple App Store, hỗ trợ Push Notification. | Sinh viên và thu ngân có thể truy cập Web App một cách tiện lợi. |
| **Khả năng Tùy biến & Làm chủ mã nguồn** | Toàn quyền can thiệp mã nguồn backend (Python) và frontend (OWL framework), không bị khóa vendor (No Vendor Lock-in). | Một số module lõi đóng mã nguồn (closed-source), phụ thuộc vào server nâng cấp của Odoo. | Community vượt trội về tính linh hoạt cho sinh viên và đội ngũ kỹ thuật UTH tự bảo trì. |

---

## 3. PHÂN TÍCH CHI TIẾT THEO CÁC PHÂN HỆ CĂN-TIN

### 3.1. Phân hệ Điểm bán hàng (Point of Sale - POS)
- **Odoo Community 20.0**: 
  - Cho phép mở/đóng ca bán hàng (`pos.session`), kiểm kê tiền mặt đầu ca (`opening_balance`) và chốt tiền cuối ca.
  - Hỗ trợ đầy đủ danh mục món ăn, hình ảnh, mã vạch barcode, phân loại món theo nhóm (Điểm tâm, Cơm trưa, Nước ngọt).
  - Tích hợp linh hoạt nhiều phương thức thanh toán: Tiền mặt, Chuyển khoản QR (VietQR, MoMo) và Thẻ sinh viên UTH.
- **Odoo Education Enterprise**:
  - Bổ sung khả năng kết nối IoT Box không dây, cho phép quét thẻ sinh viên NFC/RFID trực tiếp tại quầy trong 0.5 giây.
  - Hỗ trợ phân tách đơn hàng (Split Bill) và gộp bàn ăn (Floor & Table Management).

### 3.2. Phân hệ Báo cáo Doanh thu & Đối soát Tài chính
- **Odoo Community 20.0**:
  - Sử dụng SQL View động (`canteen.revenue.report`) kết hợp Pivot Table và Biểu đồ Graph cho phép lọc đa chiều: theo ngày, theo ca (Sáng/Trưa/Chiều), theo cơ sở (CS1/CS2) và theo nhóm món ăn.
  - Tích hợp Wizard xuất báo cáo PDF chuẩn mực có chữ ký xác nhận của Thu ngân và Quản lý Căn-tin.
- **Odoo Education Enterprise**:
  - Cung cấp công cụ Spreadsheet BI cho phép nhúng công thức tính toán lãi lỗ (P&L), điểm hòa vốn (Break-even Point) tự động cập nhật theo thời gian thực.

---

## 4. KẾT LUẬN VÀ LỘ TRÌNH TRIỂN KHAI CHO UTH

1. **Giai đoạn MVP 3 tuần (Tuần 1 - Tuần 3)**:
   - **Lựa chọn**: Sử dụng **Odoo Community 20.0** kết hợp custom module `uth_canteen_pos`.
   - **Lý do**: Không phát sinh chi phí bản quyền, kiểm soát 100% dữ liệu sinh viên tại máy chủ nội bộ trường, dễ dàng tích hợp với các hệ thống hiện có của UTH (Supabase, MSSV).
   - **Kết quả đạt được**: Hoàn thiện toàn bộ chức năng POS, bảng giá sinh viên, thanh toán QR/thẻ SV, kiểm soát ca bán và báo cáo doanh thu minh bạch.

2. **Giai đoạn Mở rộng (Phase 2 - Triển khai thực tế toàn trường)**:
   - Đăng ký chương trình **Odoo for Education** để nhận gói Enterprise tài trợ cho mục đích đào tạo và vận hành mẫu.
   - Thử nghiệm các tính năng Kiosk tự phục vụ (Self-ordering) và đầu đọc thẻ NFC sinh viên tốc độ cao.
