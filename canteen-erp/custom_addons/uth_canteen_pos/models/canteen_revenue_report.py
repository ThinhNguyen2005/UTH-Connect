"""Canteen revenue SQL view and reporting model."""

from odoo import api, fields, models, tools


class CanteenRevenueReport(models.Model):
    """Aggregate revenue report for UTH Canteen POS."""

    _name = "canteen.revenue.report"
    _description = "Báo cáo doanh thu Căn-tin UTH"
    _auto = False
    _order = "date desc, total_amount desc"

    id = fields.Id()
    date = fields.Date(string="Ngày", readonly=True)
    canteen_location = fields.Selection(
        [
            ("main", "Căn-tin chính (Cơ sở 1)"),
            ("sub1", "Căn-tin phụ (Cơ sở 2)"),
            ("kiosk", "Quầy kiosk"),
        ],
        string="Địa điểm",
        readonly=True,
    )
    meal_type = fields.Selection(
        [
            ("breakfast", "Bữa sáng"),
            ("lunch", "Bữa trưa"),
            ("snack", "Ăn vặt"),
            ("drink", "Thức uống"),
        ],
        string="Khung bữa ăn",
        readonly=True,
    )
    product_id = fields.Many2one("product.product", string="Món ăn / Thức uống", readonly=True)
    category_id = fields.Many2one("pos.category", string="Nhóm món", readonly=True)
    order_count = fields.Integer(string="Số lượt mua", readonly=True)
    qty_sold = fields.Float(string="Số lượng bán", readonly=True)
    total_amount = fields.Float(string="Tổng doanh thu (VND)", readonly=True)
    payment_method_name = fields.Char(string="Phương thức thanh toán", readonly=True)

    def init(self):
        """Create or replace the PostgreSQL view for fast revenue aggregation."""
        tools.drop_view_if_exists(self.env.cr, self._table)
        self.env.cr.execute(f"""
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
                WHERE cfg.is_canteen = TRUE
                  AND o.state IN ('paid', 'done', 'invoiced')
                GROUP BY
                    DATE(o.date_order),
                    cfg.canteen_location,
                    o.meal_type,
                    line.product_id,
                    pt.pos_categ_ids,
                    pm.name
            )
        """)
