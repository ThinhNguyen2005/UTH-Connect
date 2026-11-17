"""POS Configuration extension for UTH Canteen."""

from odoo import api, fields, models


class PosConfig(models.Model):
    """Extend POS config with canteen-specific fields."""

    _inherit = "pos.config"

    is_canteen = fields.Boolean(
        string="Là quầy căn-tin",
        default=False,
        help="Đánh dấu đây là quầy POS thuộc hệ thống căn-tin UTH.",
    )
    canteen_location = fields.Selection(
        [
            ("main", "Căn-tin chính (Cơ sở 1)"),
            ("sub1", "Căn-tin phụ (Cơ sở 2)"),
            ("kiosk", "Quầy kiosk"),
        ],
        string="Vị trí căn-tin",
        default="main",
    )
    shift_type = fields.Selection(
        [
            ("morning", "Ca sáng (6:00 – 11:00)"),
            ("noon", "Ca trưa (11:00 – 14:00)"),
            ("afternoon", "Ca chiều (14:00 – 18:00)"),
            ("all_day", "Cả ngày"),
        ],
        string="Ca làm việc",
        default="all_day",
    )
    default_opening_balance = fields.Float(
        string="Số dư mở phiên mặc định (VND)",
        default=500000.0,
        help="Số tiền mặt bắt đầu khi mở phiên bán.",
    )
    allow_student_card = fields.Boolean(
        string="Cho phép thanh toán thẻ sinh viên",
        default=True,
    )
    daily_revenue_target = fields.Float(
        string="Mục tiêu doanh thu ngày (VND)",
        default=5000000.0,
    )

    @api.depends("name", "canteen_location")
    def _compute_display_name(self):
        for config in self:
            location_label = dict(
                self._fields["canteen_location"].selection
            ).get(config.canteen_location, "")
            if config.is_canteen and location_label:
                config.display_name = f"{config.name} – {location_label}"
            else:
                config.display_name = config.name
