{
    "name": "UTH Canteen POS",
    "version": "20.0.1.0.0",
    "category": "Point of Sale",
    "summary": "Quản lý bán hàng căn-tin UTH với POS",
    "description": """
        Module mở rộng Point of Sale cho căn-tin
        Trường Đại học Giao thông Vận tải TP.HCM (UTH).

        Tính năng:
        - Cấu hình quầy POS căn-tin
        - Danh mục sản phẩm đồ ăn/thức uống
        - Phương thức thanh toán: tiền mặt, QR, thẻ SV
        - Phiên bán hàng (mở/đóng ca)
        - Báo cáo doanh thu theo ngày/tuần/tháng
    """,
    "author": "Tạ Huy Thiên Văn",
    "website": "https://github.com/ThinhNguyen2005/UTH-Canteen-ERP",
    "license": "LGPL-3",
    "depends": [
        "point_of_sale",
        "product",
        "account",
    ],
    "data": [
        "security/ir.access.csv",
        "data/pos_category_data.xml",
        "data/product_data.xml",
        "data/pos_payment_method_data.xml",
        "data/pos_config_data.xml",
        "views/pos_config_views.xml",
        "views/pos_order_views.xml",
        "views/canteen_dashboard_views.xml",
        "reports/pos_revenue_report_views.xml",
        "reports/pos_revenue_report_template.xml",
        "wizard/pos_revenue_wizard_views.xml",
        "views/menu_views.xml",
    ],
    "demo": [],
    "installable": True,
    "application": True,
    "auto_install": False,
}
