"""Exercise the report with test orders; Odoo shell rolls the transaction back."""
from datetime import date, datetime
from odoo import Command

config = env["pos.config"].search([("is_canteen", "=", True)], limit=1)
product = env.ref("uth_canteen_pos.product_banh_mi_thit")
session = env["pos.session"].create({"config_id": config.id})
for index in range(30):
    order = env["pos.order"].create({
        "session_id": session.id,
        "company_id": config.company_id.id,
        "date_order": datetime(2026, 11, 19, 4, 0),
        "amount_total": 20000, "amount_paid": 20000,
        "amount_tax": 0, "amount_return": 0,
        "state": "paid",
        "lines": [Command.create({
            "product_id": product.id, "qty": 1, "price_unit": 20000,
            "price_subtotal": 20000, "price_subtotal_incl": 20000,
        })],
    })
    if index == 0:
        methods = config.payment_method_ids[:2]
        assert len(methods) == 2
        for method in methods:
            env["pos.payment"].create({
                "pos_order_id": order.id, "payment_method_id": method.id,
                "amount": 10000,
            })
env["pos.order"].create({
    "session_id": session.id, "company_id": config.company_id.id,
    "date_order": datetime(2026, 11, 19, 4, 0),
    "amount_total": -20000, "amount_paid": -20000,
    "amount_tax": 0, "amount_return": 0, "state": "paid",
    "lines": [Command.create({
        "product_id": product.id, "qty": -1, "price_unit": 20000,
        "price_subtotal": 20000, "price_subtotal_incl": 20000,
    })],
})
env.flush_all()
wizard = env["canteen.revenue.wizard"].create({
    "date_from": date(2026, 11, 19), "date_to": date(2026, 11, 19),
    "canteen_location": config.canteen_location, "meal_type": "all",
})
totals = wizard.get_revenue_totals()
assert totals == {"orders": 31, "quantity": 29, "revenue": 580000}, totals
rows = env["canteen.revenue.report"].search([("date", "=", date(2026, 11, 19))])
assert len(rows) == 31, len(rows)
assert sum(rows.mapped("total_amount")) == 580000
assert sum(rows.mapped("order_count")) == 31
html, _ = env["ir.actions.report"]._render_qweb_html(
    "uth_canteen_pos.action_report_canteen_revenue", [wizard.id],
)
assert b"580,000.00" in html, "Rendered report does not contain the expected total"
print("KAN-120: 30 sales, split payment, refund and HTML report reconciliation passed")
