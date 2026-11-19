"""Small, Odoo-independent helpers for the PDF revenue report."""


def summarize_orders(orders):
    """Sum eligible orders once each, including negative refund lines."""
    total = 0.0
    quantity = 0.0
    count = 0
    for order in orders:
        count += 1
        total += order.amount_total
        for line in order.lines:
            quantity += line.qty
    return {"orders": count, "quantity": quantity, "revenue": total}
