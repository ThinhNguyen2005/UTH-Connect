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
    company_id = fields.Many2one("res.company", string="Công ty", readonly=True)
    order_count = fields.Integer(string="Số lượt mua", readonly=True)
    qty_sold = fields.Float(string="Số lượng bán", readonly=True)
    total_amount = fields.Float(string="Tổng doanh thu (VND)", readonly=True)

    def init(self):
        """Create or replace the PostgreSQL view for fast revenue aggregation."""
        tools.drop_view_if_exists(self.env.cr, self._table)
        self.env.cr.execute(f"""
            CREATE OR REPLACE VIEW {self._table} AS (
                SELECT
                    o.id AS id,
                    DATE(o.date_order AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Ho_Chi_Minh') AS date,
                    o.company_id AS company_id,
                    cfg.canteen_location AS canteen_location,
                    o.meal_type AS meal_type,
                    1 AS order_count,
                    COALESCE(lines.qty_sold, 0) AS qty_sold,
                    o.amount_total AS total_amount
                FROM pos_order o
                JOIN pos_session s ON o.session_id = s.id
                JOIN pos_config cfg ON s.config_id = cfg.id
                LEFT JOIN LATERAL (
                    SELECT SUM(line.qty) AS qty_sold
                    FROM pos_order_line line WHERE line.order_id = o.id
                ) lines ON TRUE
                WHERE cfg.is_canteen = TRUE
                  AND o.state IN ('paid', 'done', 'invoiced')
            )
        """)
