"""Regression tests for the real-order PDF aggregation."""

import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest


MODULE_PATH = (
    Path(__file__).resolve().parents[1]
    / "custom_addons" / "uth_canteen_pos" / "wizard" / "revenue_totals.py"
)
spec = importlib.util.spec_from_file_location("revenue_totals", MODULE_PATH)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class RevenueTotalsTest(unittest.TestCase):
    def test_empty_period(self):
        self.assertEqual(module.summarize_orders([]), {
            "orders": 0, "quantity": 0.0, "revenue": 0.0,
        })

    def test_multiple_lines_and_refunds_count_orders_once(self):
        def line(qty, amount):
            return SimpleNamespace(qty=qty, price_subtotal_incl=amount)

        orders = [
            SimpleNamespace(amount_total=55000, lines=[line(2, 40000), line(1, 15000)]),
            SimpleNamespace(amount_total=-20000, lines=[line(-1, 20000)]),
        ]
        self.assertEqual(module.summarize_orders(orders), {
            "orders": 2, "quantity": 2.0, "revenue": 35000.0,
        })
