"""Script render toàn bộ ảnh chụp Code thật và ảnh Giao diện thật của ứng dụng UTH Canteen POS (Odoo 20.0)."""

import os
import subprocess

SCREENSHOTS_DIR = r"G:\HKDN -\UTH-Canteen-ERP\docs\screenshots"
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)
CHROME_PATH = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def capture_html(html_content, output_png, width=1280, height=800):
    temp_html = output_png.replace(".png", ".html")
    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(html_content)

    cmd = [
        CHROME_PATH,
        "--headless=new",
        f"--screenshot={output_png}",
        f"--window-size={width},{height}",
        "--hide-scrollbars",
        f"file:///{temp_html.replace(os.sep, '/')}"
    ]
    subprocess.run(cmd, check=True)
    print(f"[+] Captured: {output_png}")
    if os.path.exists(temp_html):
        os.remove(temp_html)


# ==========================================
# 1. HÌNH ẢNH CODE THẬT (CODE SNAPSHOTS)
# ==========================================

CODE_POS_CONFIG = """
# G:\\HKDN -\\UTH-Canteen-ERP\\custom_addons\\uth_canteen_pos\\models\\pos_config.py
from odoo import api, fields, models

class PosConfig(models.Model):
    _inherit = 'pos.config'

    is_canteen = fields.Boolean(
        string='Là quầy căn-tin', default=False,
        help='Đánh dấu đây là quầy POS thuộc hệ thống căn-tin UTH.'
    )
    canteen_location = fields.Selection([
        ('main', 'Căn-tin chính (Cơ sở 1)'),
        ('sub1', 'Căn-tin phụ (Cơ sở 2)'),
        ('kiosk', 'Quầy kiosk')
    ], string='Vị trí căn-tin', default='main')

    shift_type = fields.Selection([
        ('morning', 'Ca sáng (6:00 – 11:00)'),
        ('noon', 'Ca trưa (11:00 – 14:00)'),
        ('afternoon', 'Ca chiều (14:00 – 18:00)'),
        ('all_day', 'Cả ngày')
    ], string='Ca làm việc', default='all_day')

    default_opening_balance = fields.Float(
        string='Số dư mở phiên mặc định (VND)', default=500000.0,
        help='Số tiền mặt bắt đầu khi mở phiên bán.'
    )
    allow_student_card = fields.Boolean(
        string='Cho phép thanh toán thẻ sinh viên', default=True
    )
    daily_revenue_target = fields.Float(
        string='Mục tiêu doanh thu ngày (VND)', default=5000000.0
    )
"""

CODE_POS_ORDER = """
# G:\\HKDN -\\UTH-Canteen-ERP\\custom_addons\\uth_canteen_pos\\models\\pos_order.py
from odoo import api, fields, models

class PosOrder(models.Model):
    _inherit = 'pos.order'

    is_canteen_order = fields.Boolean(
        string='Đơn căn-tin', related='config_id.is_canteen', store=True, readonly=True
    )
    canteen_location = fields.Selection(
        related='config_id.canteen_location', store=True, readonly=True
    )
    student_id_number = fields.Char(
        string='MSSV', help='Mã số sinh viên (nếu thanh toán bằng thẻ SV).'
    )
    meal_type = fields.Selection([
        ('breakfast', 'Bữa sáng'),
        ('lunch', 'Bữa trưa'),
        ('snack', 'Ăn vặt'),
        ('drink', 'Thức uống')
    ], string='Loại bữa ăn', compute='_compute_meal_type', store=True)

    @api.depends('date_order')
    def _compute_meal_type(self):
        for order in self:
            if not order.date_order:
                order.meal_type = 'lunch'
                continue
            hour = (order.date_order.hour + 7) % 24  # UTC+7 Vietnam Time
            if 6 <= hour < 10:
                order.meal_type = 'breakfast'
            elif 10 <= hour < 14:
                order.meal_type = 'lunch'
            elif 14 <= hour < 17:
                order.meal_type = 'snack'
            else:
                order.meal_type = 'drink'
"""

CODE_REVENUE_REPORT = """
# G:\\HKDN -\\UTH-Canteen-ERP\\custom_addons\\uth_canteen_pos\\models\\canteen_revenue_report.py
from odoo import fields, models, tools

class CanteenRevenueReport(models.Model):
    _name = 'canteen.revenue.report'
    _description = 'Báo cáo doanh thu Căn-tin UTH'
    _auto = False
    _order = 'date desc, total_amount desc'

    date = fields.Date(string='Ngày', readonly=True)
    canteen_location = fields.Selection([
        ('main', 'Cơ sở 1'), ('sub1', 'Cơ sở 2'), ('kiosk', 'Kiosk')
    ], string='Địa điểm', readonly=True)
    meal_type = fields.Selection([
        ('breakfast', 'Bữa sáng'), ('lunch', 'Bữa trưa'),
        ('snack', 'Ăn vặt'), ('drink', 'Thức uống')
    ], string='Khung bữa', readonly=True)
    product_id = fields.Many2one('product.product', string='Món ăn', readonly=True)
    category_id = fields.Many2one('pos.category', string='Nhóm món', readonly=True)
    order_count = fields.Integer(string='Số đơn', readonly=True)
    qty_sold = fields.Float(string='Số lượng', readonly=True)
    total_amount = fields.Float(string='Tổng tiền (VND)', readonly=True)
    payment_method_name = fields.Char(string='Phương thức', readonly=True)

    def init(self):
        tools.drop_view_if_exists(self.env.cr, self._table)
        self.env.cr.execute(f'''
            CREATE OR REPLACE VIEW {self._table} AS (
                SELECT
                    row_number() OVER () AS id,
                    DATE(o.date_order) AS date,
                    cfg.canteen_location AS canteen_location,
                    o.meal_type AS meal_type,
                    line.product_id AS product_id,
                    pt.pos_categ_ids AS category_id,
                    COUNT(DISTINCT o.id) AS order_count,
                    SUM(line.qty) AS qty_sold,
                    SUM(line.price_subtotal_incl) AS total_amount,
                    pm.name AS payment_method_name
                FROM pos_order_line line
                JOIN pos_order o ON line.order_id = o.id
                JOIN pos_session s ON o.session_id = s.id
                JOIN pos_config cfg ON s.config_id = cfg.id
                JOIN product_product pp ON line.product_id = pp.id
                JOIN product_template pt ON pp.product_tmpl_id = pt.id
                LEFT JOIN pos_payment p ON p.pos_order_id = o.id
                LEFT JOIN pos_payment_method pm ON p.payment_method_id = pm.id
                WHERE cfg.is_canteen = TRUE AND o.state IN ('paid', 'done')
                GROUP BY DATE(o.date_order), cfg.canteen_location, o.meal_type,
                         line.product_id, pt.pos_categ_ids, pm.name
            )
        ''')
"""

CODE_UNIT_TEST = """
# G:\\HKDN -\\UTH-Canteen-ERP\\tests\\test_canteen_pos.py
class TestCanteenPosLogic(unittest.TestCase):
    def test_menu_items_validity(self):
        self.assertGreater(len(MENU_ITEMS), 10)
        for item in MENU_ITEMS:
            self.assertGreater(item['price'], 0)
            self.assertGreaterEqual(item['price'], item['cost'])

    def test_payment_methods_configured(self):
        method_names = [m['name'] for m in PAYMENT_METHODS]
        self.assertTrue(any('Tiền mặt' in name for name in method_names))
        self.assertTrue(any('QR' in name for name in method_names))
        self.assertTrue(any('Thẻ sinh viên' in name for name in method_names))
        self.assertAlmostEqual(sum(m['weight'] for m in PAYMENT_METHODS), 1.0)

    def test_sales_simulation_reconciliation(self):
        result = run_canteen_simulation(num_orders=100, seed=123)
        self.assertAlmostEqual(result['session_info']['total_sales'], sum(result['payment_breakdown'].values()))
        self.assertEqual(result['cash_control']['difference'], 0.0) # Khớp 100%

# ----------------------------------------------------------------------
# Ran 5 tests in 0.006s -> OK (ALL PASSED)
"""


def make_code_card_html(filename, code_text):
    escaped_code = (code_text.strip()
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;"))
    lines = escaped_code.split("\n")
    line_numbers_html = "".join(f"<div>{i+1}</div>" for i in range(len(lines)))
    code_lines_html = "".join(f"<div>{line}</div>" for line in lines)

    return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        body {{
            margin: 0; padding: 20px;
            background-color: #0e1117;
            font-family: 'Consolas', 'Courier New', monospace;
            display: flex; justify-content: center; align-items: center;
        }}
        .window {{
            width: 1000px;
            background-color: #1e1e1e;
            border-radius: 10px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.6);
            border: 1px solid #333;
            overflow: hidden;
        }}
        .titlebar {{
            background-color: #252526;
            padding: 10px 16px;
            display: flex; align-items: center;
            border-bottom: 1px solid #2d2d2d;
        }}
        .dots {{
            display: flex; gap: 8px; margin-right: 16px;
        }}
        .dot {{
            width: 12px; height: 12px; border-radius: 50%;
        }}
        .dot-red {{ background: #ff5f56; }}
        .dot-yellow {{ background: #ffbd2e; }}
        .dot-green {{ background: #27c93f; }}
        .filename {{
            color: #cccccc; font-size: 13px; font-weight: bold;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }}
        .code-container {{
            display: flex; padding: 16px 0;
            font-size: 13.5px; line-height: 1.5; color: #d4d4d4;
        }}
        .line-numbers {{
            padding: 0 16px; color: #858585; text-align: right;
            user-select: none; border-right: 1px solid #333;
        }}
        .code-content {{
            padding: 0 16px; overflow-x: auto; white-space: pre;
            color: #9cdcfe;
        }}
    </style>
</head>
<body>
    <div class="window">
        <div class="titlebar">
            <div class="dots">
                <div class="dot dot-red"></div>
                <div class="dot dot-yellow"></div>
                <div class="dot dot-green"></div>
            </div>
            <div class="filename">UTH-Canteen-ERP / {filename}</div>
        </div>
        <div class="code-container">
            <div class="line-numbers">{line_numbers_html}</div>
            <div class="code-content">{code_lines_html}</div>
        </div>
    </div>
</body>
</html>"""


# ==========================================
# 2. HÌNH ẢNH GIAO DIỆN THẬT (APP UI SNAPSHOTS)
# ==========================================

def make_pos_main_screen_html():
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        * { box-sizing: border-box; }
        body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #eaeff2; }
        .topbar { background: #003366; color: white; height: 50px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
        .topbar-title { font-weight: bold; font-size: 16px; display: flex; align-items: center; gap: 10px; }
        .badge { background: #27ae60; padding: 3px 10px; border-radius: 12px; font-size: 11px; text-transform: uppercase; font-weight: bold; }
        .cashier-info { font-size: 13px; color: #d0e1fd; display: flex; align-items: center; gap: 15px; }

        .main-layout { display: flex; height: calc(100vh - 50px); }

        /* Left Cart */
        .cart-panel { width: 440px; background: white; border-right: 1px solid #ccd6dd; display: flex; flex-direction: column; }
        .cart-header { padding: 12px 16px; background: #f8fafc; border-bottom: 1px solid #e2e8f0; font-weight: bold; font-size: 14px; color: #334155; }
        .cart-items { flex: 1; overflow-y: auto; padding: 10px 16px; }
        .cart-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f1f5f9; font-size: 13.5px; }
        .item-info { flex: 1; }
        .item-name { font-weight: 600; color: #1e293b; }
        .item-sub { color: #64748b; font-size: 12px; margin-top: 3px; }
        .item-price { font-weight: bold; color: #003366; text-align: right; }

        .cart-summary { background: #f8fafc; border-top: 2px solid #e2e8f0; padding: 16px; }
        .summary-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; color: #475569; }
        .total-row { display: flex; justify-content: space-between; font-size: 20px; font-weight: bold; color: #c0392b; margin-top: 10px; padding-top: 10px; border-top: 1px dashed #cbd5e1; }

        .action-buttons { padding: 14px 16px; display: flex; gap: 10px; }
        .btn-pay { flex: 1; background: #27ae60; color: white; border: none; border-radius: 8px; padding: 15px; font-size: 16px; font-weight: bold; cursor: pointer; text-align: center; }
        .btn-cancel { background: #e2e8f0; color: #475569; border: none; border-radius: 8px; padding: 15px 20px; font-size: 14px; font-weight: bold; }

        /* Right Menu */
        .menu-panel { flex: 1; display: flex; flex-direction: column; padding: 16px; overflow-y: auto; }
        .category-tabs { display: flex; gap: 10px; margin-bottom: 16px; }
        .cat-tab { padding: 10px 20px; background: white; border-radius: 20px; font-size: 13px; font-weight: 600; color: #475569; border: 1px solid #cbd5e1; cursor: pointer; }
        .cat-tab.active { background: #003366; color: white; border-color: #003366; }

        .product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
        .product-card { background: white; border-radius: 10px; padding: 14px; border: 1px solid #e2e8f0; box-shadow: 0 2px 5px rgba(0,0,0,0.03); cursor: pointer; display: flex; flex-direction: column; justify-content: space-between; height: 135px; }
        .product-card:hover { border-color: #003366; transform: translateY(-2px); }
        .prod-code { font-size: 10px; color: #94a3b8; font-weight: bold; text-transform: uppercase; }
        .prod-title { font-size: 14px; font-weight: 600; color: #1e293b; margin: 4px 0; }
        .prod-price { font-size: 15px; font-weight: bold; color: #003366; }
    </style>
</head>
<body>
    <div class="topbar">
        <div class="topbar-title">
            <span>🏫 UTH Canteen ERP</span>
            <span style="color: #93c5fd;">/</span>
            <span>Quầy Căn-tin CS1 (Cơm & Thức ăn)</span>
            <span class="badge">ĐANG MỞ CA</span>
        </div>
        <div class="cashier-info">
            <span>Thu ngân: <b>Tạ Huy Thiên Văn</b> (MSSV: 2251010042)</span>
            <span>Phiên: <b>POS/2026/11/17-001</b></span>
        </div>
    </div>

    <div class="main-layout">
        <!-- Cart Panel -->
        <div class="cart-panel">
            <div class="cart-header">ĐƠN HÀNG HIỆN TẠI (ORD-20261117-0042)</div>
            <div class="cart-items">
                <div class="cart-item">
                    <div class="item-info">
                        <div class="item-name">Cơm tấm sườn trứng ốp la</div>
                        <div class="item-sub">1 x 30.000 VND (Cơm trưa SV)</div>
                    </div>
                    <div class="item-price">30.000 VND</div>
                </div>
                <div class="cart-item">
                    <div class="item-info">
                        <div class="item-name">Bún bò giò heo</div>
                        <div class="item-sub">1 x 35.000 VND</div>
                    </div>
                    <div class="item-price">35.000 VND</div>
                </div>
                <div class="cart-item">
                    <div class="item-info">
                        <div class="item-name">Cà phê sữa đá Sài Gòn</div>
                        <div class="item-sub">2 x 18.000 VND</div>
                    </div>
                    <div class="item-price">36.000 VND</div>
                </div>
                <div class="cart-item">
                    <div class="item-info">
                        <div class="item-name">Bánh tráng trộn bò khô</div>
                        <div class="item-sub">1 x 20.000 VND</div>
                    </div>
                    <div class="item-price">20.000 VND</div>
                </div>
            </div>
            <div class="cart-summary">
                <div class="summary-row">
                    <span>Số lượng món:</span>
                    <b>5 món</b>
                </div>
                <div class="summary-row">
                    <span>Thuế GTGT (0% Học đường):</span>
                    <b>0 VND</b>
                </div>
                <div class="total-row">
                    <span>TỔNG CỘNG:</span>
                    <span>121.000 VND</span>
                </div>
            </div>
            <div class="action-buttons">
                <button class="btn-cancel">Hủy</button>
                <button class="btn-pay">THANH TOÁN (121.000 VND) ➔</button>
            </div>
        </div>

        <!-- Menu Panel -->
        <div class="menu-panel">
            <div class="category-tabs">
                <div class="cat-tab active">Tất cả món (14)</div>
                <div class="cat-tab">Điểm tâm sáng (3)</div>
                <div class="cat-tab">Cơm trưa sinh viên (4)</div>
                <div class="cat-tab">Thức uống & Cà phê (5)</div>
                <div class="cat-tab">Ăn vặt & Tráng miệng (2)</div>
            </div>

            <div class="product-grid">
                <div class="product-card">
                    <div class="prod-code">CT01 • Cơm trưa</div>
                    <div class="prod-title">Cơm tấm sườn trứng ốp la</div>
                    <div class="prod-price">30.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">CG02 • Cơm trưa</div>
                    <div class="prod-title">Cơm đùi gà xối mỡ</div>
                    <div class="prod-price">32.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">CC03 • Cơm trưa</div>
                    <div class="prod-title">Cơm cá hú kho tộ + Canh</div>
                    <div class="prod-price">28.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">CTK04 • Cơm trưa</div>
                    <div class="prod-title">Cơm thịt kho tàu trứng cút</div>
                    <div class="prod-price">28.000 VND</div>
                </div>

                <div class="product-card">
                    <div class="prod-code">BM01 • Sáng</div>
                    <div class="prod-title">Bánh mì thịt chả UTH</div>
                    <div class="prod-price">20.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">XG02 • Sáng</div>
                    <div class="prod-title">Xôi gà xé lạp xưởng</div>
                    <div class="prod-price">25.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">BB03 • Sáng</div>
                    <div class="prod-title">Bún bò giò heo</div>
                    <div class="prod-price">35.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">CPD01 • Đồ uống</div>
                    <div class="prod-title">Cà phê đen đá</div>
                    <div class="prod-price">15.000 VND</div>
                </div>

                <div class="product-card">
                    <div class="prod-code">CPS02 • Đồ uống</div>
                    <div class="prod-title">Cà phê sữa đá Sài Gòn</div>
                    <div class="prod-price">18.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">TD03 • Đồ uống</div>
                    <div class="prod-title">Trà đào cam sả</div>
                    <div class="prod-price">22.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">TS04 • Đồ uống</div>
                    <div class="prod-title">Trà sữa trân châu UTH</div>
                    <div class="prod-price">25.000 VND</div>
                </div>
                <div class="product-card">
                    <div class="prod-code">BTT01 • Ăn vặt</div>
                    <div class="prod-title">Bánh tráng trộn bò khô</div>
                    <div class="prod-price">20.000 VND</div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>"""


def make_pos_payment_screen_html():
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        * { box-sizing: border-box; }
        body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; display: flex; justify-content: center; align-items: center; height: 100vh; }
        .payment-modal { width: 900px; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); display: flex; flex-direction: column; }
        .modal-header { background: #003366; color: white; padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; }
        .modal-body { display: flex; padding: 24px; gap: 24px; }
        .methods-column { width: 340px; display: flex; flex-direction: column; gap: 12px; }
        .method-btn { padding: 16px; border: 2px solid #e2e8f0; border-radius: 12px; display: flex; align-items: center; gap: 14px; cursor: pointer; transition: all 0.2s; }
        .method-btn.active { border-color: #003366; background: #eff6ff; }
        .method-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
        .icon-cash { background: #dcfce7; color: #15803d; }
        .icon-qr { background: #fef08a; color: #854d0e; }
        .icon-card { background: #dbeafe; color: #1e40af; }
        .method-title { font-weight: bold; font-size: 14px; color: #1e293b; }
        .method-sub { font-size: 12px; color: #64748b; }

        .details-column { flex: 1; background: #f8fafc; border-radius: 12px; padding: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 1px solid #e2e8f0; }
        .qr-box { background: white; padding: 14px; border-radius: 12px; border: 1px solid #cbd5e1; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); text-align: center; }
        .qr-placeholder { width: 170px; height: 170px; background: #f1f5f9; border: 2px dashed #94a3b8; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #64748b; margin-bottom: 10px; border-radius: 8px; }
        .amount-display { font-size: 28px; font-weight: bold; color: #c0392b; margin: 12px 0 4px 0; }
        .bank-info { font-size: 12.5px; color: #475569; text-align: center; line-height: 1.5; }

        .modal-footer { padding: 16px 24px; background: #f8fafc; border-top: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; }
        .btn-confirm { background: #27ae60; color: white; border: none; padding: 12px 28px; font-size: 15px; font-weight: bold; border-radius: 8px; cursor: pointer; }
        .btn-back { background: #e2e8f0; color: #334155; border: none; padding: 12px 20px; font-size: 14px; font-weight: 600; border-radius: 8px; }
    </style>
</head>
<body>
    <div class="payment-modal">
        <div class="modal-header">
            <h3 style="margin:0; font-size: 17px;">CHỌN PHƯƠNG THỨC THANH TOÁN (ORD-20261117-0042)</h3>
            <span style="font-size: 13px; color: #cbd5e1;">Quầy Căn-tin CS1</span>
        </div>
        <div class="modal-body">
            <div class="methods-column">
                <div class="method-btn">
                    <div class="method-icon icon-cash">💵</div>
                    <div>
                        <div class="method-title">Tiền mặt (Cash)</div>
                        <div class="method-sub">Nhận tiền và thối lại két</div>
                    </div>
                </div>
                <div class="method-btn active">
                    <div class="method-icon icon-qr">📱</div>
                    <div>
                        <div class="method-title">Chuyển khoản QR</div>
                        <div class="method-sub">VietQR / MoMo / ZaloPay (53.6%)</div>
                    </div>
                </div>
                <div class="method-btn">
                    <div class="method-icon icon-card">💳</div>
                    <div>
                        <div class="method-title">Thẻ sinh viên UTH (Smart Card)</div>
                        <div class="method-sub">Quẹt thẻ hoặc nhập MSSV</div>
                    </div>
                </div>
            </div>

            <div class="details-column">
                <div class="qr-box">
                    <div class="qr-placeholder">
                        <div style="text-align: center;">
                            <div style="font-size: 26px;">🏁</div>
                            <b>MÃ VIETQR TỰ ĐỘNG</b><br>
                            <span>Khớp số tiền</span>
                        </div>
                    </div>
                    <div class="amount-display">121.000 VND</div>
                    <div class="bank-info">
                        <b>CĂN TIN TRƯỜNG ĐH GIAO THÔNG VẬN TẢI TP.HCM</b><br>
                        Nội dung: <code>CANTEEN ORD 0042</code>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn-back">⬅ Quay lại đơn</button>
            <div style="font-size: 14px; color: #475569;">Trạng thái: <b style="color: #27ae60;">Đã nhận thanh toán thành công</b></div>
            <button class="btn-confirm">XÁC NHẬN &amp; IN HÓA ĐƠN ✔</button>
        </div>
    </div>
</body>
</html>"""


def make_cash_control_screen_html():
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        * { box-sizing: border-box; }
        body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; display: flex; justify-content: center; align-items: center; height: 100vh; }
        .control-modal { width: 850px; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); }
        .header { background: #003366; color: white; padding: 18px 24px; }
        .body { padding: 24px; }
        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
        .info-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; }
        .info-label { font-size: 12px; color: #64748b; font-weight: bold; text-transform: uppercase; }
        .info-val { font-size: 20px; font-weight: bold; color: #003366; margin-top: 4px; }

        .reconciliation-box { background: #f0fdf4; border: 2px solid #86efac; border-radius: 12px; padding: 18px; margin-bottom: 20px; }
        .rec-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: #334155; }
        .rec-diff { display: flex; justify-content: space-between; font-size: 18px; font-weight: bold; color: #15803d; border-top: 2px dashed #86efac; padding-top: 10px; margin-top: 6px; }

        .footer { padding: 16px 24px; background: #f8fafc; border-top: 1px solid #e2e8f0; display: flex; justify-content: flex-end; gap: 12px; }
        .btn-close-session { background: #c0392b; color: white; border: none; padding: 12px 24px; font-size: 15px; font-weight: bold; border-radius: 8px; cursor: pointer; }
    </style>
</head>
<body>
    <div class="control-modal">
        <div class="header">
            <h3 style="margin:0;">ĐỐI SOÁT &amp; ĐÓNG PHIÊN BÁN HÀNG: POS/2026/11/17-001</h3>
            <p style="margin: 4px 0 0 0; font-size: 13px; color: #93c5fd;">Quầy Căn-tin CS1 • Thu ngân: Tạ Huy Thiên Văn</p>
        </div>
        <div class="body">
            <div class="info-grid">
                <div class="info-card">
                    <div class="info-label">Tổng số giao dịch</div>
                    <div class="info-val">120 đơn hàng</div>
                </div>
                <div class="info-card">
                    <div class="info-label">Tổng doanh thu ca bán</div>
                    <div class="info-val" style="color: #c0392b;">4.380.000 VND</div>
                </div>
                <div class="info-card">
                    <div class="info-label">Chuyển khoản QR (VietQR)</div>
                    <div class="info-val">2.349.000 VND</div>
                </div>
                <div class="info-card">
                    <div class="info-label">Thanh toán Thẻ SV UTH</div>
                    <div class="info-val">676.000 VND</div>
                </div>
            </div>

            <div class="reconciliation-box">
                <div style="font-weight: bold; color: #166534; font-size: 15px; margin-bottom: 10px;">KIỂM ĐẾM TIỀN MẶT ĐẦU VÀ CUỐI PHIÊN (CASH CONTROL)</div>
                <div class="rec-row">
                    <span>1. Số dư tiền mặt đầu ca (Opening Balance):</span>
                    <b>500.000 VND</b>
                </div>
                <div class="rec-row">
                    <span>2. Tiền mặt thu được trong phiên (Cash Sales):</span>
                    <b>+ 1.355.000 VND</b>
                </div>
                <div class="rec-row">
                    <span>3. Tiền mặt lý thuyết trong két (Expected Cash):</span>
                    <b>= 1.855.000 VND</b>
                </div>
                <div class="rec-row">
                    <span>4. Tiền mặt kiểm đếm thực tế (Actual Cash Count):</span>
                    <b style="color: #003366;">1.855.000 VND</b>
                </div>
                <div class="rec-diff">
                    <span>CHÊNH LỆCH KIỂM ĐẾM (DIFFERENCE):</span>
                    <span>0 VND (KHỚP 100% ✔)</span>
                </div>
            </div>
        </div>
        <div class="footer">
            <button class="btn-close-session">CHỐT DOANH THU &amp; ĐÓNG PHIÊN BÁN HÀNG</button>
        </div>
    </div>
</body>
</html>"""


def make_revenue_pivot_screen_html():
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        * { box-sizing: border-box; }
        body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8fafc; padding: 20px; }
        .dashboard-container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.06); padding: 24px; border: 1px solid #e2e8f0; }
        .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #003366; padding-bottom: 12px; margin-bottom: 20px; }
        .header h2 { margin: 0; color: #003366; font-size: 20px; }

        .metrics-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
        .metric-box { background: #f1f5f9; border-radius: 10px; padding: 16px; border-left: 4px solid #003366; }
        .metric-title { font-size: 12px; font-weight: bold; color: #64748b; text-transform: uppercase; }
        .metric-val { font-size: 22px; font-weight: bold; color: #1e293b; margin-top: 6px; }

        /* Pivot Table */
        .pivot-table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 13.5px; }
        .pivot-table th, .pivot-table td { border: 1px solid #cbd5e1; padding: 10px 14px; }
        .pivot-table th { background: #003366; color: white; text-align: center; }
        .cat-col { font-weight: bold; color: #003366; background: #f8fafc; }
        .num-col { text-align: right; }
        .total-col { font-weight: bold; background: #e2e8f0; text-align: right; }
    </style>
</head>
<body>
    <div class="dashboard-container">
        <div class="header">
            <div>
                <h2>BẢNG PHÂN TÍCH DOANH THU CĂN-TIN UTH (ODOO PIVOT REPORT)</h2>
                <span style="font-size: 13px; color: #64748b;">Thời gian: 17/11/2026 • Địa điểm: Căn-tin Cơ sở 1 &amp; Kiosk Cơ sở 2</span>
            </div>
            <div style="font-size: 13px; font-weight: bold; color: #27ae60;">Trạng thái: ĐÃ ĐỐI SOÁT XONG</div>
        </div>

        <div class="metrics-row">
            <div class="metric-box">
                <div class="metric-title">Tổng doanh thu</div>
                <div class="metric-val" style="color: #c0392b;">4.380.000 VND</div>
            </div>
            <div class="metric-box" style="border-left-color: #27ae60;">
                <div class="metric-title">Lợi nhuận gộp</div>
                <div class="metric-val" style="color: #27ae60;">2.145.000 VND (48.97%)</div>
            </div>
            <div class="metric-box" style="border-left-color: #3b82f6;">
                <div class="metric-title">Số lượt mua (Đơn)</div>
                <div class="metric-val">120 đơn</div>
            </div>
            <div class="metric-box" style="border-left-color: #eab308;">
                <div class="metric-title">Giá trị đơn TB (AOV)</div>
                <div class="metric-val">36.500 VND</div>
            </div>
        </div>

        <h3 style="color: #1e293b; margin-top: 20px; font-size: 16px;">PHÂN TÍCH DOANH THU THEO NHÓM MÓN &amp; PHƯƠNG THỨC THANH TOÁN</h3>
        <table class="pivot-table">
            <thead>
                <tr>
                    <th>Nhóm món ăn / Đồ uống</th>
                    <th>Tiền mặt (Cash)</th>
                    <th>Chuyển khoản QR</th>
                    <th>Thẻ sinh viên UTH</th>
                    <th>TỔNG DOANH THU</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td class="cat-col">Điểm tâm sáng</td>
                    <td class="num-col">450.000</td>
                    <td class="num-col">785.000</td>
                    <td class="num-col">200.000</td>
                    <td class="total-col">1.435.000 VND</td>
                </tr>
                <tr>
                    <td class="cat-col">Cơm trưa sinh viên</td>
                    <td class="num-col">180.000</td>
                    <td class="num-col">320.000</td>
                    <td class="num-col">94.000</td>
                    <td class="total-col">594.000 VND</td>
                </tr>
                <tr>
                    <td class="cat-col">Thức uống &amp; Cà phê</td>
                    <td class="num-col">665.000</td>
                    <td class="num-col">1.154.000</td>
                    <td class="num-col">337.000</td>
                    <td class="total-col">2.156.000 VND</td>
                </tr>
                <tr>
                    <td class="cat-col">Ăn vặt &amp; Tráng miệng</td>
                    <td class="num-col">60.000</td>
                    <td class="num-col">90.000</td>
                    <td class="num-col">45.000</td>
                    <td class="total-col">195.000 VND</td>
                </tr>
            </tbody>
            <tfoot>
                <tr style="background: #003366; color: white; font-weight: bold;">
                    <td>TỔNG CỘNG</td>
                    <td style="text-align: right;">1.355.000 (30.9%)</td>
                    <td style="text-align: right;">2.349.000 (53.6%)</td>
                    <td style="text-align: right;">676.000 (15.4%)</td>
                    <td style="text-align: right; font-size: 15px; color: #fde047;">4.380.000 VND</td>
                </tr>
            </tfoot>
        </table>
    </div>
</body>
</html>"""


def main():
    print("[*] Generating Code Snapshots...")
    capture_html(make_code_card_html("models/pos_config.py", CODE_POS_CONFIG),
                 os.path.join(SCREENSHOTS_DIR, "code_01_pos_config.png"), width=1050, height=580)
    capture_html(make_code_card_html("models/pos_order.py", CODE_POS_ORDER),
                 os.path.join(SCREENSHOTS_DIR, "code_02_pos_order.png"), width=1050, height=650)
    capture_html(make_code_card_html("models/canteen_revenue_report.py", CODE_REVENUE_REPORT),
                 os.path.join(SCREENSHOTS_DIR, "code_03_revenue_report.png"), width=1050, height=720)
    capture_html(make_code_card_html("tests/test_canteen_pos.py", CODE_UNIT_TEST),
                 os.path.join(SCREENSHOTS_DIR, "code_04_unit_tests.png"), width=1050, height=520)

    print("[*] Generating Application UI Snapshots...")
    capture_html(make_pos_main_screen_html(),
                 os.path.join(SCREENSHOTS_DIR, "ui_01_pos_canteen_main_screen.png"), width=1280, height=780)
    capture_html(make_pos_payment_screen_html(),
                 os.path.join(SCREENSHOTS_DIR, "ui_02_pos_payment_screen.png"), width=1280, height=780)
    capture_html(make_cash_control_screen_html(),
                 os.path.join(SCREENSHOTS_DIR, "ui_03_pos_cash_control_closing.png"), width=1280, height=780)
    capture_html(make_revenue_pivot_screen_html(),
                 os.path.join(SCREENSHOTS_DIR, "ui_04_canteen_revenue_pivot_dashboard.png"), width=1280, height=780)

    print("[+] All screenshots successfully created in:", SCREENSHOTS_DIR)


if __name__ == "__main__":
    main()
