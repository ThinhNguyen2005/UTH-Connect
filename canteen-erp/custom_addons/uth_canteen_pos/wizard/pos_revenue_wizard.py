"""Wizard for generating canteen revenue report."""

from datetime import datetime, time, timedelta

import pytz

from odoo import api, fields, models
from odoo.exceptions import ValidationError

from .revenue_totals import summarize_orders


class PosRevenueWizard(models.TransientModel):
    """Filter and export revenue report for UTH Canteen."""

    _name = "canteen.revenue.wizard"
    _description = "Bộ lọc Báo cáo Doanh thu Căn-tin"

    date_from = fields.Date(
        string="Từ ngày",
        required=True,
        default=fields.Date.context_today,
    )
    date_to = fields.Date(
        string="Đến ngày",
        required=True,
        default=fields.Date.context_today,
    )
    canteen_location = fields.Selection(
        [
            ("all", "Tất cả cơ sở"),
            ("main", "Căn-tin chính (Cơ sở 1)"),
            ("sub1", "Căn-tin phụ (Cơ sở 2)"),
            ("kiosk", "Quầy kiosk"),
        ],
        string="Địa điểm",
        default="all",
        required=True,
    )
    meal_type = fields.Selection(
        [
            ("all", "Tất cả các ca"),
            ("breakfast", "Bữa sáng (6:00 – 10:00)"),
            ("lunch", "Bữa trưa (10:00 – 14:00)"),
            ("snack", "Ăn vặt (14:00 – 17:00)"),
            ("drink", "Thức uống"),
        ],
        string="Khung bữa ăn",
        default="all",
        required=True,
    )

    @api.constrains("date_from", "date_to")
    def _check_date_range(self):
        for wizard in self:
            if wizard.date_from and wizard.date_to and wizard.date_from > wizard.date_to:
                raise ValidationError("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.")

    def get_revenue_totals(self):
        """Read paid canteen orders for the selected local calendar dates."""
        self.ensure_one()
        self._check_date_range()
        tz = pytz.timezone("Asia/Ho_Chi_Minh")
        start = tz.localize(datetime.combine(self.date_from, time.min))
        end = tz.localize(datetime.combine(self.date_to + timedelta(days=1), time.min))
        domain = [
            ("is_canteen_order", "=", True),
            ("state", "in", ["paid", "done", "invoiced"]),
            ("date_order", ">=", start.astimezone(pytz.UTC).replace(tzinfo=None)),
            ("date_order", "<", end.astimezone(pytz.UTC).replace(tzinfo=None)),
        ]
        if self.canteen_location != "all":
            domain.append(("canteen_location", "=", self.canteen_location))
        if self.meal_type != "all":
            domain.append(("meal_type", "=", self.meal_type))
        return summarize_orders(self.env["pos.order"].search(domain))

    def action_view_revenue_pivot(self):
        """Open the pivot/graph view with selected domain."""
        self.ensure_one()
        domain = [
            ("date", ">=", self.date_from),
            ("date", "<=", self.date_to),
        ]
        if self.canteen_location != "all":
            domain.append(("canteen_location", "=", self.canteen_location))
        if self.meal_type != "all":
            domain.append(("meal_type", "=", self.meal_type))

        return {
            "name": f"Doanh thu Căn-tin ({self.date_from} đến {self.date_to})",
            "type": "ir.actions.act_window",
            "res_model": "canteen.revenue.report",
            "view_mode": "pivot,graph,list",
            "domain": domain,
            "context": {
                "pivot_measures": ["total_amount", "qty_sold", "order_count"],
                "pivot_row_groupby": ["date"],
                "pivot_col_groupby": ["canteen_location"],
            },
        }

    def action_print_revenue_pdf(self):
        """Print PDF report."""
        self.ensure_one()
        return self.env.ref("uth_canteen_pos.action_report_canteen_revenue").report_action(self)
