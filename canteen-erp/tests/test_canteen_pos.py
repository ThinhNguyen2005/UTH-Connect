"""Unit tests for UTH Canteen POS ERP business logic."""

import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from scripts.simulate_canteen_sales import (
    MENU_ITEMS,
    PAYMENT_METHODS,
    run_canteen_simulation,
)


class TestCanteenPosLogic(unittest.TestCase):
    """Test suite for UTH Canteen POS rules and calculations."""

    def test_menu_items_validity(self):
        """All items must have positive price, positive cost, and price >= cost."""
        self.assertGreater(len(MENU_ITEMS), 10, "Menu should have at least 10 items")
        for item in MENU_ITEMS:
            self.assertGreater(item["price"], 0, f"Item {item['name']} must have positive price")
            self.assertGreater(item["cost"], 0, f"Item {item['name']} must have positive cost")
            self.assertGreaterEqual(
                item["price"],
                item["cost"],
                f"Selling price must be >= cost for {item['name']}"
            )
            self.assertIn(
                item["meal"],
                ["breakfast", "lunch", "snack", "drink"],
                f"Invalid meal type for {item['name']}"
            )

    def test_payment_methods_configured(self):
        """Payment methods must include Cash, QR, and Student Card."""
        method_names = [m["name"] for m in PAYMENT_METHODS]
        self.assertTrue(any("Tiền mặt" in name for name in method_names))
        self.assertTrue(any("QR" in name for name in method_names))
        self.assertTrue(any("Thẻ sinh viên" in name for name in method_names))

        total_weight = sum(m["weight"] for m in PAYMENT_METHODS)
        self.assertAlmostEqual(total_weight, 1.0, places=2, msg="Payment weights must sum to 1.0")

    def test_sales_simulation_reconciliation(self):
        """Cash difference must be zero and sum of payments must equal total revenue."""
        result = run_canteen_simulation(num_orders=100, seed=123)

        total_sales = result["session_info"]["total_sales"]
        payment_sum = sum(result["payment_breakdown"].values())
        self.assertAlmostEqual(
            total_sales,
            payment_sum,
            places=2,
            msg="Total sales must match sum of payments"
        )

        cash_control = result["cash_control"]
        expected_cash = cash_control["opening_cash"] + cash_control["cash_transactions"]
        self.assertEqual(cash_control["expected_closing_cash"], expected_cash)
        self.assertEqual(cash_control["difference"], 0.0, "Cash difference must be zero in ideal shift")

    def test_category_breakdown_integrity(self):
        """Category sales total must equal overall total sales."""
        result = run_canteen_simulation(num_orders=80, seed=999)
        total_sales = result["session_info"]["total_sales"]
        category_sum = sum(result["category_breakdown"].values())
        self.assertAlmostEqual(
            total_sales,
            category_sum,
            places=2,
            msg="Category sum must equal total sales"
        )

    def test_profit_margin_calculation(self):
        """Gross profit margin should be realistic (35% to 65% for university canteen)."""
        result = run_canteen_simulation(num_orders=120, seed=42)
        margin = result["session_info"]["profit_margin_pct"]
        self.assertGreaterEqual(margin, 35.0)
        self.assertLessEqual(margin, 65.0)


if __name__ == "__main__":
    unittest.main()
