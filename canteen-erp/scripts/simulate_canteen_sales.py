"""Script mô phỏng phiên bán hàng Căn-tin UTH và tính toán đối soát doanh thu.

Mô phỏng quy trình MVP:
1. Mở phiên bán hàng (Opening Session): kiểm đếm tiền mặt đầu ca (500,000 VND)
2. Thực hiện các giao dịch bán hàng sinh viên qua POS
3. Phân bổ các phương thức thanh toán: Tiền mặt, QR MoMo/VietQR, Thẻ sinh viên
4. Đóng phiên bán (Closing Session): đối soát doanh thu thực tế vs lý thuyết
5. Xuất báo cáo tổng kết phiên bán và doanh thu theo nhóm món
"""

import json
import random
from datetime import datetime, timedelta

# Master Data Menu Căn-tin UTH
MENU_ITEMS = [
    # Điểm tâm sáng
    {"code": "BM01", "name": "Bánh mì thịt chả UTH", "category": "Điểm tâm sáng", "price": 20000, "cost": 12000, "meal": "breakfast"},
    {"code": "XG02", "name": "Xôi gà xé lạp xưởng", "category": "Điểm tâm sáng", "price": 25000, "cost": 15000, "meal": "breakfast"},
    {"code": "BB03", "name": "Bún bò giò heo", "category": "Điểm tâm sáng", "price": 35000, "cost": 22000, "meal": "breakfast"},
    # Cơm trưa
    {"code": "CT01", "name": "Cơm tấm sườn trứng ốp la", "category": "Cơm trưa", "price": 30000, "cost": 18000, "meal": "lunch"},
    {"code": "CG02", "name": "Cơm đùi gà xối mỡ", "category": "Cơm trưa", "price": 32000, "cost": 20000, "meal": "lunch"},
    {"code": "CC03", "name": "Cơm cá hú kho tộ + Canh chua", "category": "Cơm trưa", "price": 28000, "cost": 17000, "meal": "lunch"},
    {"code": "CTK04", "name": "Cơm thịt kho tàu trứng cút", "category": "Cơm trưa", "price": 28000, "cost": 16000, "meal": "lunch"},
    # Thức uống
    {"code": "CPD01", "name": "Cà phê đen đá", "category": "Thức uống & Cà phê", "price": 15000, "cost": 6000, "meal": "drink"},
    {"code": "CPS02", "name": "Cà phê sữa đá Sài Gòn", "category": "Thức uống & Cà phê", "price": 18000, "cost": 8000, "meal": "drink"},
    {"code": "TD03", "name": "Trà đào cam sả", "category": "Thức uống & Cà phê", "price": 22000, "cost": 9000, "meal": "drink"},
    {"code": "TS04", "name": "Trà sữa trân châu UTH", "category": "Thức uống & Cà phê", "price": 25000, "cost": 10000, "meal": "drink"},
    {"code": "NS05", "name": "Nước khoáng Aquafina 500ml", "category": "Thức uống & Cà phê", "price": 8000, "cost": 4000, "meal": "drink"},
    # Ăn vặt
    {"code": "BTT01", "name": "Bánh tráng trộn bò khô", "category": "Ăn vặt & Tráng miệng", "price": 20000, "cost": 10000, "meal": "snack"},
    {"code": "SC02", "name": "Sữa chua nếp cẩm", "category": "Ăn vặt & Tráng miệng", "price": 15000, "cost": 7000, "meal": "snack"},
]

PAYMENT_METHODS = [
    {"name": "Tiền mặt (Cash)", "weight": 0.35, "is_cash": True},
    {"name": "Chuyển khoản QR (VietQR / MoMo)", "weight": 0.45, "is_cash": False},
    {"name": "Thẻ sinh viên UTH (Smart Card)", "weight": 0.20, "is_cash": False},
]

STUDENT_IDS = [f"225101{i:04d}" for i in range(1, 200)]


def run_canteen_simulation(num_orders=120, seed=42):
    random.seed(seed)
    session_start = datetime(2026, 11, 17, 6, 30, 0)
    opening_balance = 500000.0  # 500k VND đầu ca

    orders = []
    category_summary = {}
    payment_summary = {pm["name"]: 0.0 for pm in PAYMENT_METHODS}
    item_sold_count = {item["name"]: 0 for item in MENU_ITEMS}

    cur_time = session_start
    total_sales = 0.0
    total_cost = 0.0

    for i in range(1, num_orders + 1):
        # Time progression
        cur_time += timedelta(minutes=random.randint(1, 4))
        hour = cur_time.hour

        # Select meal category based on time
        if hour < 10:
            allowed = [it for it in MENU_ITEMS if it["meal"] in ("breakfast", "drink")]
        elif 10 <= hour < 14:
            allowed = [it for it in MENU_ITEMS if it["meal"] in ("lunch", "drink", "snack")]
        else:
            allowed = [it for it in MENU_ITEMS if it["meal"] in ("snack", "drink", "lunch")]

        # Select 1 to 3 items
        items_count = random.choices([1, 2, 3], weights=[0.5, 0.4, 0.1])[0]
        chosen_items = random.sample(allowed, k=min(items_count, len(allowed)))

        order_lines = []
        order_total = 0.0
        order_cost = 0.0

        for it in chosen_items:
            qty = random.choices([1, 2], weights=[0.85, 0.15])[0]
            line_subtotal = it["price"] * qty
            line_cost = it["cost"] * qty
            order_total += line_subtotal
            order_cost += line_cost
            item_sold_count[it["name"]] += qty

            cat = it["category"]
            category_summary[cat] = category_summary.get(cat, 0.0) + line_subtotal

            order_lines.append({
                "item": it["name"],
                "code": it["code"],
                "qty": qty,
                "unit_price": it["price"],
                "subtotal": line_subtotal
            })

        # Select payment method
        pm_choice = random.choices(
            PAYMENT_METHODS,
            weights=[p["weight"] for p in PAYMENT_METHODS]
        )[0]

        student_mssv = None
        if "Thẻ sinh viên" in pm_choice["name"] or random.random() < 0.3:
            student_mssv = random.choice(STUDENT_IDS)

        payment_summary[pm_choice["name"]] += order_total
        total_sales += order_total
        total_cost += order_cost

        orders.append({
            "order_number": f"ORD-20261117-{i:04d}",
            "time": cur_time.strftime("%H:%M:%S"),
            "lines": order_lines,
            "total_amount": order_total,
            "payment_method": pm_choice["name"],
            "student_id": student_mssv
        })

    # Session Reconciliation
    cash_collected = payment_summary["Tiền mặt (Cash)"]
    closing_cash_expected = opening_balance + cash_collected
    closing_cash_actual = closing_cash_expected  # Không lệch tiền
    cash_difference = closing_cash_actual - closing_cash_expected

    profit = total_sales - total_cost
    margin_pct = (profit / total_sales * 100) if total_sales else 0

    simulation_result = {
        "session_info": {
            "session_name": "POS/2026/11/17-001 (Ca ngày Căn-tin CS1)",
            "config": "Quầy Căn-tin CS1 - Cơm & Thức ăn",
            "cashier": "Tạ Huy Thiên Văn",
            "opened_at": session_start.strftime("%Y-%m-%d %H:%M:%S"),
            "closed_at": cur_time.strftime("%Y-%m-%d %H:%M:%S"),
            "opening_balance": opening_balance,
            "total_orders": len(orders),
            "total_sales": total_sales,
            "total_cost": total_cost,
            "gross_profit": profit,
            "profit_margin_pct": round(margin_pct, 2)
        },
        "cash_control": {
            "opening_cash": opening_balance,
            "cash_transactions": cash_collected,
            "expected_closing_cash": closing_cash_expected,
            "actual_closing_cash": closing_cash_actual,
            "difference": cash_difference
        },
        "payment_breakdown": payment_summary,
        "category_breakdown": category_summary,
        "top_selling_items": sorted(item_sold_count.items(), key=lambda x: x[1], reverse=True)[:5],
        "sample_orders": orders[:5]
    }

    return simulation_result


if __name__ == "__main__":
    result = run_canteen_simulation()
    print("=== KẾT QUẢ MÔ PHỎNG PHIÊN BÁN HÀNG CĂN-TIN UTH (ERP) ===")
    print(f"Phiên: {result['session_info']['session_name']}")
    print(f"Thu ngân: {result['session_info']['cashier']}")
    print(f"Tổng số đơn: {result['session_info']['total_orders']}")
    print(f"Tổng doanh thu: {result['session_info']['total_sales']:,.0f} VND")
    print(f"Lợi nhuận gộp: {result['session_info']['gross_profit']:,.0f} VND ({result['session_info']['profit_margin_pct']}%)")
    print("\n-- Cơ cấu thanh toán --")
    for k, v in result["payment_breakdown"].items():
        pct = (v / result["session_info"]["total_sales"]) * 100
        print(f"  * {k}: {v:,.0f} VND ({pct:.1f}%)")
    print("\n-- Doanh thu theo nhóm món --")
    for k, v in result["category_breakdown"].items():
        print(f"  * {k}: {v:,.0f} VND")
    print("\n-- Kiểm soát tiền mặt đóng phiên --")
    print(f"  * Số dư đầu ca: {result['cash_control']['opening_cash']:,.0f} VND")
    print(f"  * Tiền mặt thu: {result['cash_control']['cash_transactions']:,.0f} VND")
    print(f"  * Tiền mặt cuối ca kỳ vọng: {result['cash_control']['expected_closing_cash']:,.0f} VND")
    print(f"  * Chênh lệch kiểm kê: {result['cash_control']['difference']:,.0f} VND (Khớp 100%)")
