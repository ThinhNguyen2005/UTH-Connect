# UTH Canteen ERP – Odoo Community 20.0

> MVP 3 tuần — Hệ thống quản lý căn-tin Trường Đại học Giao thông Vận tải TP.HCM (UTH)

## Tổng quan dự án

| Mục | Chi tiết |
|-----|---------|
| **Phần mềm** | Odoo Community 20.0 |
| **Module chính** | Point of Sale (POS) |
| **Mục tiêu** | Quản lý bán hàng căn-tin: sản phẩm, giá bán, thanh toán, phiên bán, doanh thu |
| **Task Jira** | KAN-120 |

## Cấu trúc thư mục

```
UTH-Canteen-ERP/
├── custom_addons/
│   └── uth_canteen_pos/          # Custom module cho POS căn-tin
│       ├── __manifest__.py
│       ├── __init__.py
│       ├── models/               # Model mở rộng POS
│       ├── views/                # Giao diện XML
│       ├── data/                 # Dữ liệu mẫu (sản phẩm, giá, thanh toán)
│       ├── security/             # Quyền truy cập
│       ├── reports/              # Báo cáo doanh thu
│       ├── wizard/               # Wizard tổng hợp
│       └── static/description/   # Mô tả module
├── config/
│   └── odoo.conf                 # Cấu hình Odoo server
├── docs/
│   ├── reports/                  # Báo cáo nghiệm thu
│   └── screenshots/              # Ảnh chụp demo
├── scripts/
│   └── setup_database.py         # Script khởi tạo DB
└── tests/                        # Test cases
```

## Cài đặt & Chạy

### Yêu cầu
- Python 3.11+
- PostgreSQL 16+
- Odoo Community 20.0 source

### Bước 1: Clone và cài đặt
```bash
git clone <repo-url>
cd UTH-Canteen-ERP
pip install -r requirements.txt
```

### Bước 2: Cấu hình database
```bash
# Tạo user và database PostgreSQL
python scripts/setup_database.py
```

### Bước 3: Chạy Odoo
```bash
python odoo-bin -c config/odoo.conf -i uth_canteen_pos
```

## Phạm vi KAN-120

1. **Cấu hình POS**: Tạo quầy POS "Căn-tin UTH", cấu hình giao diện
2. **Sản phẩm & Giá bán**: Danh mục món ăn, giá bán VND
3. **Phương thức thanh toán**: Tiền mặt, chuyển khoản QR, thẻ sinh viên
4. **Phiên bán (POS Session)**: Mở/đóng phiên, kiểm tra tiền đầu/cuối
5. **Kiểm tra doanh thu**: Báo cáo doanh thu theo ngày/tuần/tháng
6. **Báo cáo so sánh**: Odoo Education Enterprise vs Community

## Branching

- `main` – nhánh ổn định
- `develop` – nhánh tích hợp
- `feature/kan-120-*` – nhánh task con
