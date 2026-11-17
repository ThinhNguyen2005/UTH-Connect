"""POS Order extension for UTH Canteen."""

from odoo import api, fields, models


class PosOrder(models.Model):
    """Extend POS order with canteen-specific fields."""

    _inherit = "pos.order"

    is_canteen_order = fields.Boolean(
        string="Đơn căn-tin",
        related="config_id.is_canteen",
        store=True,
        readonly=True,
    )
    canteen_location = fields.Selection(
        related="config_id.canteen_location",
        store=True,
        readonly=True,
    )
    student_id_number = fields.Char(
        string="MSSV",
        help="Mã số sinh viên (nếu thanh toán bằng thẻ SV).",
    )
    meal_type = fields.Selection(
        [
            ("breakfast", "Bữa sáng"),
            ("lunch", "Bữa trưa"),
            ("snack", "Ăn vặt"),
            ("drink", "Thức uống"),
        ],
        string="Loại bữa ăn",
        compute="_compute_meal_type",
        store=True,
    )

    @api.depends("date_order")
    def _compute_meal_type(self):
        for order in self:
            if not order.date_order:
                order.meal_type = "lunch"
                continue
            hour = order.date_order.hour + 7  # UTC+7
            if hour >= 24:
                hour -= 24
            if 6 <= hour < 10:
                order.meal_type = "breakfast"
            elif 10 <= hour < 14:
                order.meal_type = "lunch"
            elif 14 <= hour < 17:
                order.meal_type = "snack"
            else:
                order.meal_type = "drink"
