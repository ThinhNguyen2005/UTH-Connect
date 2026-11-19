"""POS Session extension for UTH Canteen."""

from odoo import api, fields, models


class PosSession(models.Model):
    """Extend POS session with canteen shift tracking."""

    _inherit = "pos.session"

    shift_type = fields.Selection(
        related="config_id.shift_type",
        store=True,
        readonly=True,
        string="Ca làm việc",
    )
    is_canteen_session = fields.Boolean(
        related="config_id.is_canteen",
        store=True,
        readonly=True,
    )
    expected_opening_balance = fields.Float(
        related="config_id.default_opening_balance",
        readonly=True,
        string="Số dư mở phiên kỳ vọng",
    )
    total_revenue = fields.Float(
        string="Tổng doanh thu phiên",
        compute="_compute_total_revenue",
        store=True,
    )
    canteen_order_count = fields.Integer(
        string="Số đơn hàng",
        compute="_compute_total_revenue",
        store=True,
    )

    @api.depends("order_ids", "order_ids.amount_total", "order_ids.state")
    def _compute_total_revenue(self):
        for session in self:
            orders = session.order_ids.filtered(lambda order: order.state in ("paid", "done", "invoiced"))
            session.total_revenue = sum(orders.mapped("amount_total"))
            session.canteen_order_count = len(orders)
